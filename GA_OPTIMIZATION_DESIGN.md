# ArtEvolver Genetic Algorithm — Deep Analysis & Optimization Design

## 1. Problem Definition

ArtEvolver solves a **constrained color-permutation optimization** problem:
- **Input:** A target image + a physical palette of N paint chips (each a unique color)
- **Output:** An assignment of palette colors to a fixed triangular mesh that minimizes visual difference from the target image
- **Constraint:** Every palette color must be used exactly once (it's a permutation)
- **Physical outcome:** The solution is built as a real mural using cut paint palette pieces

This is fundamentally similar to the **Linear Assignment Problem** (LAP) or a constrained Traveling Salesman variant — not a typical genetic algorithm with free-valued genes.

## 2. Current Algorithm Architecture

### 2.1 Pipeline Per Iteration

```
Parent Selection → Child Deep-Copy → Crossover → Mutations → Render → Compare → Replace
     ~0%              ~12%            ~2%         ~3%        ~50%     ~25%       ~0%
```

**Key insight: 75% of CPU time is spent in rendering + comparison, which are redundant for most mutations.**

### 2.2 Current Parameters (QUALITY_MODE_STREAM)

| Parameter | Value | Description |
|-----------|-------|-------------|
| THREADS | 8 | Independent evolver threads |
| POPULATION | 3 per evolver | 24 total individuals |
| EVOLVE_ITERATIONS | 2 | Children per evolve() call |
| Grid mutations | ~32 per child | Localized swaps |
| Random mutations | ~1 per child | Global swaps |
| Targeted swaps | 12 per child | Source-image-guided repair |
| Crossover block | 1/12 to 1/4 of N | Spatial region from secondary parent |
| Tournament decay | Every 80 iterations | Reduces mutation rates globally |

### 2.3 CPU Time Breakdown

| Activity | % Time | Cause |
|----------|--------|-------|
| **Rendering (fillPolygon)** | **40-55%** | Software rasterization of 5700 triangles on ~1.8M pixel canvas |
| **Pixel comparison** | **20-30%** | 1.85M pixel Manhattan distance (all pixels, every iteration) |
| Object allocation | 10-15% | N new Triangle objects per child (Polygon constructor copies arrays) |
| Mutation logic | 5-10% | Targeted swap dominates (12 × 896 color comparisons) |
| Random number gen | 2-5% | SplittableRandom, very fast |
| Selection/bookkeeping | <1% | Trivial |

## 3. Identified Issues

### 3.1 Algorithmic Issues (Impact: HIGH)

1. **Full re-render every iteration.** When mutations only change 1-32 colors out of 5700, re-rendering ALL triangles is wasteful. A delta approach (only recompute changed pixels) would be 100-5000x faster per mutation.

2. **Full pixel comparison every iteration.** Comparing all 1.85M pixels when only 2 triangles changed wastes ~98% of comparison work. Delta comparison should only compare pixels in affected triangles.

3. **Effectively (1+1) hill-climbing.** With pop=3 and elitist parent selection, the GA is not doing meaningful genetic recombination. The population structure adds overhead without diversity benefit.

4. **Irreversible parameter decay.** Mutation rates monotonically decrease with no reheat mechanism. This causes premature convergence — the algorithm freezes in a local optimum.

5. **Static mutation parameters shared across threads.** All 8 threads race on the same static fields, causing decay N× faster than intended.

### 3.2 Implementation Issues (Impact: MEDIUM)

6. **Triangle deep-copy.** Each `new Triangle()` extends `Polygon`, which copies int arrays internally. 5700 allocations per child → GC pressure.

7. **Race condition in population sync.** EDT pushes best individual via `setBestPop()` while worker thread reads `pop` in `evolve()`. No synchronization.

8. **`getGeneticChild()` is O(n²).** Uses `contains()` (linear scan) per triangle.

9. **`switchGridColor` can infinite-loop** when `gridSize == 1`.

### 3.3 Missed Opportunities (Impact: HIGH)

10. **Pre-computed triangle pixel masks.** Since geometry is fixed, compute which pixels belong to each triangle once at init. Then rendering becomes a pixel-array fill, not fillPolygon calls.

11. **Delta fitness evaluation.** For a single swap (most mutations), only 2 triangles change. Subtract their old contribution from the score, add the new contribution. This turns O(1.85M) into O(~650) per swap.

12. **Speculative mutation acceptance.** Try a swap, compute delta fitness. If better, keep it; if not, undo it. No child object allocation needed — mutate the parent in-place.

## 4. Optimization Roadmap

### Phase 1: Delta Fitness (THIS SPRINT) — Expected 10-50x speedup

The single highest-impact change. Eliminates rendering and full comparison.

**Design:**
```
At initialization:
  1. For each triangle, rasterize it to find which pixels it covers
  2. Store as int[] pixelIndices per triangle
  3. For each pixel in the reference image, store the target RGB
  4. Compute initial score by summing pixel differences

Per mutation (swap colors at positions i, j):
  1. For triangle i's pixels: subtract old diff, add new diff with color_j
  2. For triangle j's pixels: subtract old diff, add new diff with color_i
  3. delta = new_partial_sum - old_partial_sum
  4. If parent_score + delta > parent_score: accept swap (it's a net improvement)
  5. Otherwise: reject (no work wasted)
```

**Benefits:**
- No `renderTriangles()` call per iteration (eliminates 40-55% of CPU)
- No full `compare()` call per iteration (eliminates 20-30% of CPU)
- No child Triangle allocation (eliminates 10-15% of CPU)
- Accept/reject decisions are instant (no speculative rendering)
- Total: **75-85% of current CPU time is eliminated**

### Phase 2: Speculative In-Place Mutation — Expected 2-5x on top of Phase 1

Instead of deep-copying the parent to create a child:
- Mutate the parent in-place (swap two colors)
- Compute delta fitness
- If better: keep the swap
- If worse: undo the swap (swap back)

This eliminates ALL object allocation per iteration.

### Phase 3: Adaptive Mutation / Simulated Annealing — Quality improvement

- Replace monotonic decay with temperature-based acceptance:
  Accept worse solutions with probability `exp(-delta/temperature)`
- Temperature decreases over time but can reheat when progress stalls
- Per-thread independent temperature (not shared static)

### Phase 4: Intelligent Operators — Quality improvement

- **Neighborhood-aware swaps:** Prefer swapping colors between spatially close triangles
- **Gradient-guided mutations:** Use the per-triangle error to choose which triangles to mutate
- **Multi-resolution:** Solve at low resolution first, then refine at higher resolution

## 5. Phase 1 Implementation Detail

### 5.1 New Class: `TriangleMask`

```java
public class TriangleMask {
    int triangleIndex;
    int[] pixelIndices;  // flat array of (y * width + x) for each pixel in this triangle
    int pixelCount;
}
```

### 5.2 Initialization (in ImageEvolver)

```java
private TriangleMask[] triangleMasks;
private int[] referenceRGB;      // flat array: reference image pixels
private long baselineDiff;       // total pixel difference for current color assignment

void buildTriangleMasks() {
    // For each triangle, rasterize to find covered pixels
    // Use a temporary 1-pixel-thick scanline approach or
    // render each triangle solo and scan for non-zero pixels
}
```

### 5.3 Delta Fitness Computation

```java
long computeSwapDelta(int triA, int triB, Color colorA, Color colorB) {
    long delta = 0;
    
    // Triangle A: was colorA, will become colorB
    for (int px : triangleMasks[triA].pixelIndices) {
        int ref = referenceRGB[px];
        int refR = (ref >> 16) & 0xff, refG = (ref >> 8) & 0xff, refB = ref & 0xff;
        
        // Remove old contribution
        delta -= Math.abs(refR - colorA.getRed()) + Math.abs(refG - colorA.getGreen()) + Math.abs(refB - colorA.getBlue());
        // Add new contribution
        delta += Math.abs(refR - colorB.getRed()) + Math.abs(refG - colorB.getGreen()) + Math.abs(refB - colorB.getBlue());
    }
    
    // Triangle B: was colorB, will become colorA (symmetric)
    for (int px : triangleMasks[triB].pixelIndices) {
        int ref = referenceRGB[px];
        int refR = (ref >> 16) & 0xff, refG = (ref >> 8) & 0xff, refB = ref & 0xff;
        delta -= Math.abs(refR - colorB.getRed()) + Math.abs(refG - colorB.getGreen()) + Math.abs(refB - colorB.getBlue());
        delta += Math.abs(refR - colorA.getRed()) + Math.abs(refG - colorA.getGreen()) + Math.abs(refB - colorA.getBlue());
    }
    
    return delta;  // negative = improvement
}
```

### 5.4 Expected Performance

With ~5700 triangles covering ~1.85M pixels:
- Average pixels per triangle: ~325 pixels
- A single swap evaluates: 2 × 325 = **650 pixels**
- Vs. current: render 5700 triangles + compare 1.85M pixels = **~2M operations**
- **Speedup per swap: ~3000x**

Even accounting for per-child overhead (32 grid swaps + targeted swap):
- 32 swaps × 650 pixels = ~21K pixel operations
- Vs. current single render + compare = ~2M operations
- **Per-child speedup: ~100x**

Conservative estimate accounting for memory access patterns and overhead: **10-50x real-world speedup**

## 6. Metrics for Success

| Metric | Current (5000 iter benchmark) | Target |
|--------|-------------------------------|--------|
| Iterations/sec (1T) | ~650-700 | 5,000-30,000 |
| Score gain per second | ~0.00004/sec | ~0.001/sec (25x) |
| Time to 0.55 score | ~hours | ~minutes |
| Memory per iteration | ~5700 Triangle allocs | ~0 (in-place) |

---

## 7. Key Insight: This Is a Linear Assignment Problem

### 7.1 The Fundamental Realization

ArtEvolver's problem — assign N colors to N triangles to minimize total pixel error,
with each color used exactly once — is **exactly** the
[Linear Assignment Problem (LAP)](https://en.wikipedia.org/wiki/Assignment_problem).

The cost matrix is:

```
C[i][j] = total pixel error when triangle i has color j
        = SUM over all pixels in triangle i of:
            |refR[px] - colorR[j]| + |refG[px] - colorG[j]| + |refB[px] - colorB[j]|
```

The optimal solution is a permutation sigma that minimizes:
```
SUM over all i of C[i, sigma(i)]
```

The LAP has **known polynomial-time exact solvers** (O(N^3)), meaning we can find the
**provably optimal** color assignment — not an approximation — in a predictable time.

### 7.2 Algorithm: Jonker-Volgenant (LAPJV)

The fastest practical solver for dense cost matrices is the Jonker-Volgenant algorithm
(1987). It runs in O(N^3) with excellent constants due to an augmenting row reduction
initialization that skips many augmentation steps.

Published benchmarks (C++ with AVX2, 2.4 GHz Xeon):

| Matrix Size | LAPJV Solve Time |
|-------------|------------------|
| 1024 | 0.027s |
| 2048 | 0.140s |
| 4096 | 0.745s |
| 8192 | 3.053s |

Java will be roughly 2-5x slower, but JIT compilation helps for loop-heavy code.

### 7.3 Estimated End-to-End Runtimes

| Phase | N=1482 (1-palette) | N=5700 (4-palette) |
|-------|--------------------|--------------------|
| Build cost matrix (8 threads) | 0.3-0.5s | 4-8s |
| Solve LAP (LAPJV, Java) | 0.1-0.2s | 5-10s |
| **Total for optimal solution** | **~0.5-1s** | **~10-20s** |

Compare: the GA runs for **minutes to hours** and produces only an approximation.

### 7.4 Memory Requirements

| Scenario | N | Entries | Memory (int[]) |
|----------|---|---------|----------------|
| 1-palette | 1482 | 2.2M | 8.8 MB |
| 4-palette | 5700 | 32.5M | 130 MB |

Max cost per cell: ~325 pixels * 765 max Manhattan distance = ~248K. Fits in int.

### 7.5 Integration Architecture

```
1. Build triangles with fixed geometry (existing code)
2. Build DeltaFitnessEngine pixel masks (existing, ~200ms)
3. Build N x N cost matrix using pixel masks + reference image
4. Solve LAP via LAPJV → optimal permutation sigma
5. Assign color[sigma[i]] to triangle[i]
6. DONE — provably optimal, no iteration needed
```

The existing `smartAssignColors()` (greedy O(N^2)) would be replaced.
The `evolveDelta()` / GA machinery becomes unnecessary for color assignment.

### 7.6 Implementation Plan

**Approach: Port LAPJV from C to Java (~200 lines, zero dependencies)**

The original algorithm is described in:
> R. Jonker and A. Volgenant, "A shortest augmenting path algorithm for dense and
> sparse linear assignment problems," Computing, vol. 38, pp. 325-340, 1987.

The C reference implementation is ~200 lines of array operations with no external
dependencies. The port to Java is straightforward — the algorithm uses only flat arrays
and integer arithmetic.

**Alternative: Google OR-Tools** `LinearSumAssignment` — production-quality but requires
a native JNI dependency (~50MB). Overkill for what is a 200-line algorithm.

### 7.7 GA vs LAP: When Each Is Appropriate

| Criterion | GA + Hill-climbing | LAP Solver |
|-----------|-------------------|------------|
| Solution quality | Heuristic (non-optimal) | **Provably optimal** |
| Time to solution | Minutes to hours | **Seconds** |
| Progressive display | Yes (evolves visually) | No (one-shot) |
| Interactive/streaming | Good for entertainment | Poor (instant result) |
| Very large N (>10K) | Feasible | Memory-intensive |

**Recommendation**: Implement LAP as the primary solver for best quality, keep GA/delta
as a "live evolution" display mode for streaming/entertainment.

---

## 8. UI Performance Analysis

### 8.1 Bugs Found

1. **Image repaint at 0.8 FPS instead of 20 FPS**: `currentFrame % GUI_UPDATE_MS`
   used `GUI_UPDATE_MS = 50` (a millisecond value) as a frame-count modulus.
   Every 50th tick at 25ms/tick = one repaint every 1,250ms.
   **Fix**: Use `currentFrame % (FPS / GUI_FPS)` = every 2nd tick = 20 FPS.

2. **`getGraphics()` anti-pattern**: Direct painting via `imagePanel.getGraphics()`
   creates a transient context that gets invalidated on resize/minimize/overlap.
   **Fix**: Draw in `paintComponent()` override, trigger via `repaint()`.

3. **Console logging nested inside HEALTH_ITERATIONS block**: Fires at most every
   25 seconds despite `LOG_INTERVAL_MS = 5000` suggesting every 5 seconds.
   **Fix**: Move logging outside the health check block.

4. **Delta sync every 50 iterations**: With 4,295 iter/sec, this means the display
   image updates at most ~86 times/sec internally, but combined with the 0.8 FPS
   repaint bug, the user sees changes ~0.8 times/sec.
   **Fix**: Reduce to every 10 iterations.
