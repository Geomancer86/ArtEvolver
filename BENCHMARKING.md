# Benchmarking Guide

ArtEvolver v3.1 includes a structured benchmarking system that replaces ad-hoc
`System.out.println` output with machine-readable CSV files, automated test harnesses,
and human-readable summary reports.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Benchmark Output Format](#benchmark-output-format)
- [Running Benchmarks](#running-benchmarks)
- [GUI Benchmarking](#gui-benchmarking)
- [Comparing Runs](#comparing-runs)
- [Analyzing Results](#analyzing-results)
- [Available Benchmark Tests](#available-benchmark-tests)
- [Custom Benchmarks](#custom-benchmarks)

---

## Quick Start

Run the quick smoke test to verify the pipeline works:

```bash
mvn test -pl artevolver-core -Dtest=BenchmarkTest#quickBenchmark
```

Results are written to `artevolver-core/benchmarks/` as timestamped CSV files.

---

## Benchmark Output Format

Each benchmark run produces a CSV file in the `benchmarks/` directory with this schema:

| Column | Type | Description |
|--------|------|-------------|
| `elapsed_s` | float | Seconds since benchmark start |
| `total_iterations` | long | Cumulative iterations across all threads |
| `good_iterations` | long | Iterations that improved the best score |
| `health_pct` | float | Good iteration ratio as percentage |
| `best_score` | double | Best fitness score (0.0 to 1.0) |
| `iter_per_sec` | float | Throughput: iterations per second |
| `good_iter_per_sec` | float | Effective throughput: good iterations per second |
| `threads` | int | Number of evolver threads |
| `population` | int | Population size per thread |
| `triangles` | int | Total triangle count in the grid |
| `mode` | string | Configuration label |

File naming: `benchmark_{label}_{YYYYMMDD_HHmmss}.csv`

---

## Running Benchmarks

### All Benchmark Tests

```bash
mvn test -pl artevolver-core -Dtest=BenchmarkTest
```

### Individual Tests

```bash
# Quick smoke test (1 thread, 1000 iterations, ~12 seconds)
mvn test -pl artevolver-core -Dtest=BenchmarkTest#quickBenchmark

# Single-thread baseline (1 thread, 5000 iterations)
mvn test -pl artevolver-core -Dtest=BenchmarkTest#singleThreadBaseline

# Thread scaling comparison (1, 2, 4, 8 threads)
mvn test -pl artevolver-core -Dtest=BenchmarkTest#threadScalingBenchmark

# Population size comparison (pop 2, 4, 8)
mvn test -pl artevolver-core -Dtest=BenchmarkTest#populationSizeBenchmark
```

### Output Location

CSV files: `artevolver-core/benchmarks/`
Summary reports: printed to stdout during test execution

---

## GUI Benchmarking

When running the full ArtEvolver GUI application, benchmark logging is enabled by default.
CSV data is recorded automatically while the evolution is running.

**To toggle:**
- Set `ArtEvolver.BENCHMARK_LOGGING = true/false` before starting
- Set `ArtEvolver.BENCHMARK_OUTPUT_DIR` to change the output directory (default: `benchmarks/`)

**Workflow:**
1. Launch ArtEvolver
2. Load an image and click Start
3. Let the evolution run
4. Click Stop -- the benchmark summary is printed to stdout and the CSV file is saved

---

## Comparing Runs

The `BenchmarkLogger.compare()` method generates side-by-side comparison reports:

```java
String report = BenchmarkLogger.compare(
    runA.snapshots, "Before optimization",
    runB.snapshots, "After optimization");
System.out.println(report);
```

Output format:
```
=== BENCHMARK COMPARISON ===
  Metric                         Before opt        After opt      Delta
  ---                            ---               ---            ---
  Final score                    0.4892542654      0.4893100000   +0.0000557346
  Total iterations               1,000             1,000          +0
  Avg iter/sec                   84                142            +58
  Duration (s)                   11.7              7.0            -4.7
============================
```

---

## Analyzing Results

### In Excel / Google Sheets

1. Open the CSV file in your spreadsheet application
2. The columns are already labeled -- no header manipulation needed
3. Recommended charts:
   - **Score over time:** X=`elapsed_s`, Y=`best_score` (line chart)
   - **Throughput over time:** X=`elapsed_s`, Y=`iter_per_sec` (line chart)
   - **Health over time:** X=`elapsed_s`, Y=`health_pct` (line chart)
   - **Score vs iterations:** X=`total_iterations`, Y=`best_score` (scatter)

### In Python (pandas)

```python
import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv('benchmarks/benchmark_quick_smoke_20260218_010135.csv')

fig, axes = plt.subplots(2, 2, figsize=(12, 8))
df.plot(x='elapsed_s', y='best_score', ax=axes[0,0], title='Score over Time')
df.plot(x='elapsed_s', y='iter_per_sec', ax=axes[0,1], title='Throughput')
df.plot(x='elapsed_s', y='health_pct', ax=axes[1,0], title='Health %')
df.plot(x='total_iterations', y='best_score', ax=axes[1,1], title='Score vs Iterations')
plt.tight_layout()
plt.savefig('benchmark_report.png')
```

---

## Available Benchmark Tests

| Test | Threads | Iterations | Purpose |
|------|---------|------------|---------|
| `quickBenchmark` | 1 | 1,000 | Pipeline validation, CI/CD gate |
| `singleThreadBaseline` | 1 | 5,000 | Raw single-thread performance baseline |
| `threadScalingBenchmark` | 1,2,4,8 | 4,000 each | Parallel scaling efficiency |
| `populationSizeBenchmark` | 4 | 5,000 each | Population diversity vs overhead trade-off |

---

## Custom Benchmarks

Use the `BenchmarkRunner.Builder` to create custom benchmark configurations:

```java
BenchmarkRunner.Result result = new BenchmarkRunner.Builder()
    .imagePath("path/to/image.jpg")
    .totalIterations(50_000)
    .snapshotInterval(1_000)
    .threads(16)
    .population(4)
    .palettes(4)
    .widthTriangles(76)
    .heightTriangles(77)
    .triangleScale(3f)
    .label("custom_high_res")
    .outputDir("benchmarks")
    .build()
    .run();

System.out.println(result.summary);
// result.csvPath    -- path to the CSV file
// result.snapshots  -- list of Snapshot objects for programmatic analysis
// result.finalScore -- best score achieved
// result.elapsedMs  -- total wall-clock time
```
