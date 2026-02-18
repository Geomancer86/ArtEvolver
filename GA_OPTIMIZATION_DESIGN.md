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
