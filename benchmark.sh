#!/usr/bin/env bash
set -euo pipefail

echo ""
echo " ============================================"
echo "  ArtEvolver v3.2 - Benchmark Runner"
echo " ============================================"
echo ""

# -------------------------------------------------------------------
# Check prerequisites
# -------------------------------------------------------------------
command -v java &>/dev/null || { echo " [ERROR] Java not found on PATH."; exit 1; }
echo " [OK] Java found"

command -v mvn &>/dev/null || { echo " [ERROR] Maven not found on PATH."; exit 1; }
echo " [OK] Maven found"

# -------------------------------------------------------------------
# Select benchmark
# -------------------------------------------------------------------
TEST_NAME="${1:-}"

if [ -z "$TEST_NAME" ]; then
    echo ""
    echo " Select benchmark to run:"
    echo ""
    echo "   1) Quick smoke test        (~12 seconds)"
    echo "   2) Single-thread baseline  (~60 seconds)"
    echo "   3) Thread scaling test     (~4 minutes)"
    echo "   4) Population size test    (~3 minutes)"
    echo "   5) All benchmarks"
    echo "   0) Exit"
    echo ""
    read -rp "  Enter choice [1-5]: " choice

    case "$choice" in
        1) TEST_NAME="BenchmarkTest#quickBenchmark" ;;
        2) TEST_NAME="BenchmarkTest#singleThreadBaseline" ;;
        3) TEST_NAME="BenchmarkTest#threadScalingBenchmark" ;;
        4) TEST_NAME="BenchmarkTest#populationSizeBenchmark" ;;
        5) TEST_NAME="BenchmarkTest" ;;
        0) exit 0 ;;
        *) echo " Invalid choice."; exit 1 ;;
    esac
fi

# -------------------------------------------------------------------
# Run benchmark
# -------------------------------------------------------------------
echo ""
echo " Running: $TEST_NAME"
echo " Output will be saved to: artevolver-core/benchmarks/"
echo ""

mvn test -pl artevolver-core -Dtest="$TEST_NAME"

echo ""
echo " ============================================"
echo " Benchmark complete. CSV files saved to:"
echo "   artevolver-core/benchmarks/"
echo " ============================================"
echo ""
