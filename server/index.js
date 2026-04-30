import crypto from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";
import express from "express";
import multer from "multer";
import { config } from "./config.js";
import { HttpError, randomizeRom, terminateRandomizerProcesses, validateRandomizeRequest } from "./randomizer.js";
import { encodeSettings, getSettingsSchema, terminateBridgeProcesses } from "./settingsBridge.js";

const app = express();

await fs.mkdir(config.tempRoot, { recursive: true });

const upload = multer({
  storage: multer.diskStorage({
    destination: async (_req, _file, callback) => {
      try {
        const dir = path.join(config.tempRoot, crypto.randomUUID());
        await fs.mkdir(dir, { recursive: true });
        callback(null, dir);
      } catch (error) {
        callback(error);
      }
    },
    filename: (_req, file, callback) => {
      const extension = path.extname(file.originalname);
      callback(null, `input${extension}`);
    }
  }),
  limits: {
    files: 1,
    fileSize: config.maxRomSizeBytes
  }
});

app.use(express.json());

app.use((req, res, next) => {
  const requestId = crypto.randomUUID();
  const startedAt = Date.now();

  req.requestId = requestId;
  res.setHeader("X-Request-Id", requestId);

  res.on("finish", () => {
    const durationMs = Date.now() - startedAt;
    console.info(`[${requestId}] ${req.method} ${req.originalUrl} -> ${res.statusCode} (${durationMs}ms)`);
  });

  next();
});

app.get("/api/health", (_req, res) => {
  res.json({
    ok: true,
    jarConfigured: config.jarPath,
    maxRomSizeBytes: config.maxRomSizeBytes,
    timeoutMs: config.randomizerTimeoutMs
  });
});

app.get("/api/settings/schema", async (_req, res, next) => {
  try {
    res.json(await getSettingsSchema());
  } catch (error) {
    next(error);
  }
});

app.post("/api/settings/encode", async (req, res, next) => {
  try {
    res.json({ settingsString: await encodeSettings(req.body ?? {}) });
  } catch (error) {
    next(error);
  }
});

app.post("/api/randomize", upload.single("rom"), async (req, res, next) => {
  try {
    const saveLog = parseBoolean(req.body.saveLog);
    validateRandomizeRequest({
      file: req.file,
      settingsString: req.body.settingsString,
      seed: req.body.seed
    });

    const settingsString = req.body.settingsString.trim();
    console.info(
      `[${req.requestId}] randomize request accepted for ${req.file.originalname}; saveLog=${saveLog}; seedProvided=${Boolean(
        req.body.seed?.trim()
      )}; settingsString=${JSON.stringify(settingsString)}`
    );

    const result = await randomizeRom({
      file: req.file,
      settingsString,
      seed: req.body.seed,
      saveLog
    });

    console.info(`[${req.requestId}] randomize request completed with ${result.filename}`);
    res.setHeader("Content-Type", result.contentType);
    res.setHeader("Content-Disposition", `attachment; filename="${result.filename}"`);
    res.send(result.body);
  } catch (error) {
    if (req.file?.path) {
      await fs.rm(path.dirname(req.file.path), { recursive: true, force: true });
    }
    next(error);
  }
});

app.use(express.static(path.join(config.rootDir, "dist")));

app.use((_req, res) => {
  res.sendFile(path.join(config.rootDir, "dist", "index.html"));
});

app.use((error, _req, res, _next) => {
  if (error instanceof multer.MulterError) {
    const message = error.code === "LIMIT_FILE_SIZE" ? "ROM file is larger than the configured upload limit." : error.message;
    res.status(400).json({ error: message });
    return;
  }

  if (error instanceof HttpError) {
    if (error.status >= 500) {
      console.error(`[${_req.requestId ?? "unknown"}] ${error.message}`, error.cause ?? error);
    }
    res.status(error.status).json({ error: error.message });
    return;
  }

  console.error(`[${_req.requestId ?? "unknown"}] unexpected server error`, error);
  res.status(500).json({ error: "Unexpected server error." });
});

const server = app.listen(config.port, () => {
  console.log(`UPR web server listening on http://localhost:${config.port}`);
});
let isShuttingDown = false;

process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));

function parseBoolean(value) {
  return value === true || value === "true" || value === "1" || value === "on";
}

function shutdown(signal) {
  if (isShuttingDown) {
    return;
  }
  isShuttingDown = true;
  console.log(`Received ${signal}; shutting down.`);
  terminateBridgeProcesses();
  terminateRandomizerProcesses();

  const forceExit = setTimeout(() => {
    server.closeAllConnections?.();
    process.exit(1);
  }, 8000);
  forceExit.unref();

  server.close((error) => {
    clearTimeout(forceExit);
    process.exit(error ? 1 : 0);
  });
}
