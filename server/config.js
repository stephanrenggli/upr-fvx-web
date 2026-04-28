import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

export const config = {
  rootDir,
  port: Number.parseInt(process.env.PORT ?? "3000", 10),
  jarPath: process.env.UPR_FVX_JAR ?? path.join(rootDir, "vendor", "UPR-FVX.jar"),
  tempRoot: process.env.UPR_WEB_TMP ?? path.join(rootDir, "tmp"),
  maxRomSizeBytes: Number.parseInt(process.env.MAX_ROM_SIZE_BYTES ?? String(1024 * 1024 * 1024), 10),
  randomizerTimeoutMs: Number.parseInt(process.env.RANDOMIZER_TIMEOUT_MS ?? String(5 * 60 * 1000), 10),
  javaBin: process.env.JAVA_BIN ?? "java"
};
