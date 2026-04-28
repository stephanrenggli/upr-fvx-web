import fs from "node:fs/promises";
import path from "node:path";
import { spawn } from "node:child_process";
import AdmZip from "adm-zip";
import { config } from "./config.js";

const ROM_EXTENSIONS = new Set([".gb", ".sgb", ".gbc", ".gba", ".nds", ".3ds", ".cci", ".cxi"]);
const randomizerChildren = new Set();

export class HttpError extends Error {
  constructor(status, message, cause) {
    super(message);
    this.name = "HttpError";
    this.status = status;
    this.cause = cause;
  }
}

export function validateRandomizeRequest({ file, settingsString, seed }) {
  if (!file) {
    throw new HttpError(400, "Upload a ROM file before randomizing.");
  }

  if (!settingsString || !settingsString.trim()) {
    throw new HttpError(400, "Paste an FVX settings string before randomizing.");
  }

  if (seed && !/^-?\d+$/.test(seed.trim())) {
    throw new HttpError(400, "Seed must be an integer.");
  }

  const extension = path.extname(file.originalname).toLowerCase();
  if (extension && !ROM_EXTENSIONS.has(extension)) {
    throw new HttpError(400, "Unsupported ROM extension. Expected GB, GBC, GBA, NDS, 3DS, CCI, or CXI.");
  }
}

export async function randomizeRom({ file, settingsString, seed, saveLog }) {
  validateRandomizeRequest({ file, settingsString, seed });
  await assertRuntimeAvailable();

  const workDir = path.dirname(file.path);
  const inputPath = file.path;
  const outputBase = path.join(workDir, outputFileName(file.originalname));

  try {
    const cliResult = await runRandomizer({
      inputPath,
      outputBase,
      settingsString: settingsString.trim(),
      seed: seed?.trim(),
      saveLog
    });

    const outputPath = await findOutputFile({
      workDir,
      inputPath,
      requestedOutputPath: outputBase,
      cliOutput: cliResult.output
    });
    const logPath = `${outputPath}.log`;

    if (saveLog) {
      return await packageZip({ outputPath, logPath });
    }

    return {
      body: await fs.readFile(outputPath),
      contentType: "application/octet-stream",
      filename: path.basename(outputPath)
    };
  } finally {
    await fs.rm(workDir, { recursive: true, force: true });
  }
}

async function assertRuntimeAvailable() {
  try {
    await fs.access(config.jarPath);
  } catch (error) {
    throw new HttpError(
      500,
      `UPR-FVX JAR not found at ${config.jarPath}. Place UPR-FVX.jar in vendor/ or set UPR_FVX_JAR.`,
      error
    );
  }
}

function runRandomizer({ inputPath, outputBase, settingsString, seed, saveLog }) {
  const args = ["-Xmx4096M", "-jar", config.jarPath, "cli", "-i", inputPath, "-o", outputBase, "-S", settingsString];

  if (seed) {
    args.push("-z", seed);
  }

  if (saveLog) {
    args.push("-l");
  }

  return new Promise((resolve, reject) => {
    const child = spawn(config.javaBin, args, {
      cwd: config.rootDir,
      stdio: ["ignore", "pipe", "pipe"]
    });
    randomizerChildren.add(child);

    let stdout = "";
    let stderr = "";
    let didTimeout = false;

    const timeout = setTimeout(() => {
      didTimeout = true;
      child.kill("SIGTERM");
    }, config.randomizerTimeoutMs);

    child.stdout.on("data", (chunk) => {
      stdout += chunk.toString();
    });

    child.stderr.on("data", (chunk) => {
      stderr += chunk.toString();
    });

    child.on("error", (error) => {
      randomizerChildren.delete(child);
      clearTimeout(timeout);
      reject(new HttpError(500, `Could not start Java runtime: ${error.message}`, error));
    });

    child.on("close", (code) => {
      randomizerChildren.delete(child);
      clearTimeout(timeout);

      if (didTimeout) {
        reject(new HttpError(504, "Randomization timed out. Try a smaller ROM or increase RANDOMIZER_TIMEOUT_MS."));
        return;
      }

      if (code !== 0) {
        reject(new HttpError(500, summarizeCliFailure(stdout, stderr, code)));
        return;
      }

      resolve({ output: `${stdout}\n${stderr}`.trim() });
    });
  });
}

export function terminateRandomizerProcesses() {
  for (const child of randomizerChildren) {
    if (child.exitCode === null) {
      child.kill("SIGTERM");
      setTimeout(() => {
        if (child.exitCode === null) {
          child.kill("SIGKILL");
        }
      }, 2000).unref();
    }
  }
}

async function findOutputFile({ workDir, inputPath, requestedOutputPath, cliOutput }) {
  const exactOutput = await getFileCandidate(requestedOutputPath, inputPath);
  if (exactOutput) {
    return exactOutput.path;
  }

  const files = await listFiles(workDir);
  const candidates = files
    .filter((file) => file.path !== inputPath)
    .filter((file) => !file.path.endsWith(".log"))
    .filter((file) => ROM_EXTENSIONS.has(path.extname(file.path).toLowerCase()))
    .sort((a, b) => b.mtimeMs - a.mtimeMs);

  if (candidates.length > 0) {
    return candidates[0].path;
  }

  throw new HttpError(500, missingOutputMessage(files, cliOutput));
}

async function getFileCandidate(filePath, inputPath) {
  if (filePath === inputPath) {
    return null;
  }

  try {
    const stat = await fs.stat(filePath);
    return stat.isFile() ? { path: filePath, mtimeMs: stat.mtimeMs } : null;
  } catch {
    return null;
  }
}

async function listFiles(rootDir) {
  const entries = await fs.readdir(rootDir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const entryPath = path.join(rootDir, entry.name);

    if (entry.isDirectory()) {
      files.push(...(await listFiles(entryPath)));
      continue;
    }

    if (entry.isFile()) {
      const stat = await fs.stat(entryPath);
      files.push({ path: entryPath, mtimeMs: stat.mtimeMs, size: stat.size });
    }
  }

  return files;
}

function missingOutputMessage(files, cliOutput) {
  const fileSummary = files
    .map((file) => `${path.basename(file.path)} (${file.size} bytes)`)
    .join(", ");
  const cliSummary = cliOutput?.split("\n").filter(Boolean).slice(-8).join("\n");

  return [
    "Randomizer finished without producing a detectable output ROM.",
    fileSummary ? `Temp files: ${fileSummary}` : "Temp files: none",
    cliSummary ? `FVX output:\n${cliSummary}` : null
  ]
    .filter(Boolean)
    .join("\n");
}

async function packageZip({ outputPath, logPath }) {
  const zip = new AdmZip();
  zip.addLocalFile(outputPath);

  try {
    await fs.access(logPath);
    zip.addLocalFile(logPath);
  } catch {
    // FVX may skip log creation on some failures that still exit successfully.
  }

  return {
    body: zip.toBuffer(),
    contentType: "application/zip",
    filename: `${path.basename(outputPath, path.extname(outputPath))}-upr-fvx.zip`
  };
}

function outputFileName(originalName) {
  const extension = path.extname(originalName) || ".rom";
  const base = path.basename(originalName, extension).replace(/[^a-z0-9._-]/gi, "_") || "randomized";
  return `${base}-randomized${extension}`;
}

function summarizeCliFailure(stdout, stderr, code) {
  const output = `${stdout}\n${stderr}`.trim();
  const detail = output.split("\n").filter(Boolean).slice(-8).join("\n");
  return `UPR-FVX exited with code ${code}.${detail ? `\n${detail}` : ""}`;
}
