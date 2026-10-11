// Starts the Spring Boot backend with the e2e profile; used by playwright.config.ts.
// A tiny launcher keeps the config free of per-OS shell quirks for mvnw vs mvnw.cmd.
import { spawn } from "node:child_process";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const backend = fileURLToPath(new URL("../backend", import.meta.url));
const windows = process.platform === "win32";
const child = spawn(`"${join(backend, windows ? "mvnw.cmd" : "mvnw")}"`, ["-B", "-q", "spring-boot:run", "-Dspring-boot.run.profiles=e2e"], {
  cwd: backend,
  stdio: "inherit",
  shell: true,
});
child.on("exit", (code) => process.exit(code ?? 1));
for (const signal of ["SIGINT", "SIGTERM"]) process.on(signal, () => child.kill(signal));
