import crypto from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";
import { spawn } from "node:child_process";
import { config } from "./config.js";
import { HttpError } from "./randomizer.js";

const bridgeJarPath = path.join(config.rootDir, "bridge", "dist", "settings-bridge.jar");
const bridgeChildren = new Set();

export async function encodeSettings(settings) {
  await assertBridgeAvailable();
  await fs.mkdir(config.tempRoot, { recursive: true });

  const workDir = path.join(config.tempRoot, crypto.randomUUID());
  const inputPath = path.join(workDir, "settings.json");

  try {
    await fs.mkdir(workDir, { recursive: true });
    await fs.writeFile(inputPath, JSON.stringify(settings), "utf8");
    return await runBridge(["encode", inputPath]);
  } finally {
    await fs.rm(workDir, { recursive: true, force: true });
  }
}

export async function getSettingsSchema() {
  await assertBridgeAvailable();
  return JSON.parse(await runBridge(["schema"]));
}

async function assertBridgeAvailable() {
  try {
    await fs.access(config.jarPath);
  } catch (error) {
    throw new HttpError(500, `UPR-FVX JAR not found at ${config.jarPath}.`, error);
  }

  try {
    await fs.access(bridgeJarPath);
  } catch (error) {
    throw new HttpError(500, `Settings bridge JAR not found at ${bridgeJarPath}. Run npm run build:bridge.`, error);
  }
}

function runBridge(args) {
  return new Promise((resolve, reject) => {
    const child = spawn(config.javaBin, ["-cp", `${config.jarPath}:${bridgeJarPath}`, "dev.uprweb.SettingsBridge", ...args], {
      cwd: config.rootDir,
      stdio: ["ignore", "pipe", "pipe"]
    });
    bridgeChildren.add(child);

    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (chunk) => {
      stdout += chunk.toString();
    });

    child.stderr.on("data", (chunk) => {
      stderr += chunk.toString();
    });

    child.on("error", (error) => {
      bridgeChildren.delete(child);
      reject(new HttpError(500, `Could not start settings bridge: ${error.message}`, error));
    });

    child.on("close", (code) => {
      bridgeChildren.delete(child);
      if (code !== 0) {
        reject(new HttpError(500, `Settings bridge failed.${stderr ? `\n${stderr.trim()}` : ""}`));
        return;
      }
      resolve(stdout.trim());
    });
  });
}

export function terminateBridgeProcesses() {
  for (const child of bridgeChildren) {
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
