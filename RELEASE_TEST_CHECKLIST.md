# Release v3.2.0 — Test Checklist

**For human verification before push.**

---

## Scripts (all use dynamic version from POM — no jar paths)

| Script | What it does | Automated test |
|--------|--------------|----------------|
| `start.bat` | Full ArtEvolver GUI | ✓ mvn compile + exec:java |
| `start.sh` | Full ArtEvolver GUI (Linux/Mac) | ✓ Same |
| `clicker.bat` | Evolution Clicker (browser) | ✓ mvn exec:java --clicker |
| `clicker.sh` | Evolution Clicker (Linux/Mac) | ✓ Same |
| `benchmark.bat` | Benchmark runner | ✓ Passed with BenchmarkTest#quickBenchmark |
| `benchmark.sh` | Benchmark runner (Linux/Mac) | ✓ Same logic |

**Version**: All scripts read `project.version` from POM via `mvn help:evaluate`. With 3.2.0, they display "ArtEvolver 3.2.0".

**No jar names**: Scripts use `mvn exec:java -pl artevolver-core` and `mvn test -pl artevolver-core`. No `artevolver-core-3.2.0.jar` paths.

---

## Human tests (before push)

1. **start.bat** (Windows): Double-click or `start.bat` → GUI opens, Load image, Start evolution.
2. **clicker.bat** (Windows): Double-click or `clicker.bat` → Browser opens to clicker, drop image, play.
3. **start.sh** (Linux/Mac): `./start.sh` → Same as start.bat.
4. **clicker.sh** (Linux/Mac): `./clicker.sh` → Same as clicker.bat.
5. **benchmark.bat**: `benchmark.bat BenchmarkTest#quickBenchmark` or run interactively (choice 1).
6. **benchmark.sh**: Same.

---

## Automated results (2026-02-27)

- `mvn compile -pl artevolver-core` — **PASS**
- `mvn test -pl artevolver-core` — **PASS**
- `benchmark.bat BenchmarkTest#quickBenchmark` — **PASS** (displays ArtEvolver 3.2.0)
