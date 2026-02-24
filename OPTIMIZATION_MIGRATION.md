# ArtEvolver v3.0 — Performance Optimization Migration Guide

**Source:** `D:\Users\WORKSTATION\artevolver2019` branch `modernization-upgrade`
**Target:** `G:\git\artevolver` (artevolver-core module)
**Date:** 2026-02-18
**Author:** AI Agent (from artevolver2019 optimization session)

---

## Context

The artevolver2019 repo (v2.02, Swing, Java 8) received 7 performance optimizations
that have NOT been applied to the v3.0 repo (LibGDX, Java 21, multi-module). The core
evolution engine files are nearly identical in structure, so these optimizations port
directly to `artevolver-core`.

All optimizations are **algorithm-preserving** — same fitness function, same genetic
operators, same output. Only the mechanical overhead is reduced.

---

## Target Files (all under artevolver-core/src/main/java/)

| File | Path |
|------|------|
| AbstractEvolver | `com/rndmodgames/evolver/AbstractEvolver.java` |
| ImageEvolver | `com/rndmodgames/evolver/ImageEvolver.java` |
| CrossOver | `com/rndmodgames/evolver/CrossOver.java` |
| Triangle | `com/rndmodgames/evolver/Triangle.java` |

---

## Optimization 1: Bulk Pixel Comparison (CRITICAL — est. 3-5x speedup on hottest path)

**File:** `AbstractEvolver.java`
**Problem:** `compare()` calls `img.getRGB(x, y)` per-pixel in a nested loop. Each call
goes through Java2D ColorModel conversion. For a 240x156 image, that's ~37,440 slow calls
PER fitness evaluation, PER iteration, PER thread.
**v3 has the exact same code** (lines 42-127) with the same TODO comment asking to optimize it.

**Solution:** Replace the entire class with bulk `int[]` array access + pre-cached reference pixels.

```java
package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public abstract class AbstractEvolver implements Runnable {

    public abstract void evolve(long start, int iterations);

    private static final double CONSTANT_SCORE_DIVIDER = 255d;
    private static final int CONSTANT_SCORE_MULTIPLIER = 3;

    private int[] pixelsBuf1;
    private int[] pixelsBuf2;

    // Pre-cached reference image pixels — extracted once, reused every iteration
    private int[] referencePixels;
    private int refWidth;
    private int refHeight;

    public void cacheReferencePixels(BufferedImage ref) {
        this.refWidth = ref.getWidth();
        this.refHeight = ref.getHeight();
        int totalPixels = refWidth * refHeight;
        this.referencePixels = new int[totalPixels];
        ref.getRGB(0, 0, refWidth, refHeight, this.referencePixels, 0, refWidth);
    }

    public double compare(BufferedImage img1, BufferedImage img2) {
        int w = img1.getWidth();
        int h = img1.getHeight();
        int totalPixels = w * h;

        int[] px1;
        int[] px2;

        // Use pre-cached reference pixels when possible (hot path)
        if (referencePixels != null && img2.getWidth() == refWidth && img2.getHeight() == refHeight) {
            px2 = referencePixels;
        } else {
            if (pixelsBuf2 == null || pixelsBuf2.length < totalPixels) {
                pixelsBuf2 = new int[totalPixels];
            }
            img2.getRGB(0, 0, w, h, pixelsBuf2, 0, w);
            px2 = pixelsBuf2;
        }

        // Direct DataBuffer access for TYPE_INT_ARGB/RGB (zero-copy)
        boolean directAccess = img1.getType() == BufferedImage.TYPE_INT_ARGB
                || img1.getType() == BufferedImage.TYPE_INT_RGB;

        if (directAccess) {
            px1 = ((DataBufferInt) img1.getRaster().getDataBuffer()).getData();
        } else {
            if (pixelsBuf1 == null || pixelsBuf1.length < totalPixels) {
                pixelsBuf1 = new int[totalPixels];
            }
            img1.getRGB(0, 0, w, h, pixelsBuf1, 0, w);
            px1 = pixelsBuf1;
        }

        long diff = 0;  // long to prevent overflow on large images
        int rgb1, rgb2;

        for (int i = 0; i < totalPixels; i++) {
            rgb1 = px1[i];
            rgb2 = px2[i];

            diff += Math.abs(((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff));
            diff += Math.abs(((rgb1 >>  8) & 0xff) - ((rgb2 >>  8) & 0xff));
            diff += Math.abs(( rgb1        & 0xff) - ( rgb2        & 0xff));
        }

        double n = (double) totalPixels * CONSTANT_SCORE_MULTIPLIER;
        double p = diff / n / CONSTANT_SCORE_DIVIDER;
        return 1.0d - p;
    }
}
```

**Wire-up required in ImageEvolver.setResizedOriginal():**
```java
public void setResizedOriginal(BufferedImage resizedOriginal) {
    this.resizedOriginal = resizedOriginal;
    cacheReferencePixels(resizedOriginal);  // ADD THIS LINE
}
```

---

## Optimization 2: Reusable Image Buffers (CRITICAL — est. 2-3x less GC pressure)

**File:** `ImageEvolver.java`
**Problem:** `new BufferedImage(...)` is created EVERY iteration in `evolve()` (line 1037),
`updateFitness()` (line 701), and initialization methods. For 32 threads x 2 iterations/batch,
that is thousands of large heap allocations per second.
**v3 even has a TODO on line 698:** `"TODO-NA: instead of new image, clear and reuse"`

**Solution:** Add reusable buffer fields + helper methods to ImageEvolver:

```java
// Add these fields
private BufferedImage reusableChildImage;
private Graphics reusableChildGraphics;

private void ensureReusableImage() {
    if (reusableChildImage == null
            || reusableChildImage.getWidth() != resizedOriginal.getWidth()
            || reusableChildImage.getHeight() != resizedOriginal.getHeight()) {
        if (reusableChildGraphics != null) {
            reusableChildGraphics.dispose();
        }
        reusableChildImage = new BufferedImage(
                resizedOriginal.getWidth(),
                resizedOriginal.getHeight(),
                ArtEvolver.IMAGE_TYPE);
        reusableChildGraphics = reusableChildImage.getGraphics();
    }
}

// Renders into the reusable buffer (hot path — used every iteration)
private BufferedImage renderTriangles(TriangleList<Triangle> triangles) {
    ensureReusableImage();
    reusableChildGraphics.clearRect(0, 0, reusableChildImage.getWidth(), reusableChildImage.getHeight());
    for (int i = 0, size = triangles.size(); i < size; i++) {
        Triangle triangle = triangles.get(i);
        if (triangle.getColor() != null) {
            reusableChildGraphics.setColor(triangle.getColor());
            reusableChildGraphics.fillPolygon(triangle);  // NOTE: no drawPolygon (see Opt 3)
        }
    }
    return reusableChildImage;
}

// Renders into a NEW image (only for bestImage snapshots that must persist)
private BufferedImage renderTrianglesToNewImage(TriangleList<Triangle> triangles) {
    BufferedImage img = new BufferedImage(
            resizedOriginal.getWidth(),
            resizedOriginal.getHeight(),
            ArtEvolver.IMAGE_TYPE);
    Graphics g = img.getGraphics();
    for (int i = 0, size = triangles.size(); i < size; i++) {
        Triangle triangle = triangles.get(i);
        if (triangle.getColor() != null) {
            g.setColor(triangle.getColor());
            g.fillPolygon(triangle);
        }
    }
    g.dispose();
    return img;
}
```

Then replace all `new BufferedImage` + manual rendering loops in `evolve()`,
`updateFitness()`, `initializeIsosceles()`, `initialize()`, and `initializeFromFile()`
with calls to `renderTriangles()` (for fitness evaluation) or
`renderTrianglesToNewImage()` (for bestImage snapshots).

**Example — in evolve(), replace lines 1006-1058 with:**
```java
double scoreA = 0d;

if (parentA.getScore() == null || parentA.getScore() <= 0d) {
    BufferedImage imgParentA = renderTriangles(parentA);
    scoreA = compare(imgParentA, resizedOriginal);
    parentA.setScore(scoreA);
} else {
    scoreA = parentA.getScore();
}

BufferedImage rendered = renderTriangles(childA);
double scoreC = compare(rendered, resizedOriginal);
childA.setScore(scoreC);
```

**And when setting bestImage (lines 1066-1071 and 1080-1083), use renderTrianglesToNewImage:**
```java
if (scoreA > bestScore) {
    bestScore = scoreA;
    bestImage = renderTrianglesToNewImage(parentA);  // must persist
    goodIterations++;
    isDirty = true;
}

if (scoreC > bestScore) {
    bestScore = scoreC;
    bestImage = renderTrianglesToNewImage(childA);  // must persist
    // ... rest unchanged
}
```

---

## Optimization 3: Remove Redundant drawPolygon() (HIGH — est. 15-20% rendering speedup)

**Files:** `ImageEvolver.java` (throughout), `Renderer.java` (optional)
**Problem:** Every triangle rendering calls BOTH `g.drawPolygon(triangle)` (outline) AND
`g.fillPolygon(triangle)` (fill). The outline contributes nothing to fitness evaluation
and doubles the rendering work.

**v3 locations to fix:**
- `initializeFromFile()` lines 167-173
- `initializeIsosceles()` lines 295-303
- `initialize()` lines 401-409
- `updateFitness()` lines 711-719
- `evolve()` lines 1016-1024 and 1044-1052

**Solution:** In all fitness-evaluation rendering, remove `g.drawPolygon(triangle)` and the
`else { g.setColor(Color.BLUE); g.drawPolygon(triangle); }` branch. Keep only `g.fillPolygon()`.

This is already handled if you adopt the `renderTriangles()` / `renderTrianglesToNewImage()`
helpers from Optimization 2. **Do NOT change Renderer.java** export rendering — that uses
drawPolygon intentionally for visual output.

---

## Optimization 4: Thread-Local Random Number Generators (HIGH — eliminates thread contention)

**Files:** `ImageEvolver.java`, `CrossOver.java`
**Problem in v3:** `ImageEvolver.random` is `public static final Random random = new Random()`
(line 34). `java.util.Random` is synchronized — every `nextInt()`, `nextBoolean()`,
`nextFloat()` call across ALL threads (24-32) acquires the same lock.
`CrossOver.getChild()` calls `ImageEvolver.random.nextBoolean()` and
`ImageEvolver.random.nextFloat()` in tight loops.

**Solution for ImageEvolver:**
```java
// Replace the static Random field:
//   public static final Random random = new Random();
// With:
private static final ThreadLocal<SplittableRandom> THREAD_RANDOM =
        ThreadLocal.withInitial(SplittableRandom::new);

public static SplittableRandom random() {
    return THREAD_RANDOM.get();
}
```

Then replace all `random.nextInt(n)` with `random().nextInt(n)`,
`random.nextBoolean()` with `random().nextBoolean()`, etc.

**Update `roll()` (line 547-548):**
```java
public static int roll(int n) {
    return random().nextInt(n);
}
```

**Update `switchCloseColor()` (line 518):**
```java
if (random().nextBoolean()) {
```

**Solution for CrossOver — replace `ImageEvolver.random.xxx` calls with ThreadLocalRandom:**
```java
import java.util.concurrent.ThreadLocalRandom;

// In getChild(), replace ImageEvolver.random.nextBoolean() etc:
ThreadLocalRandom r = ThreadLocalRandom.current();
boolean isParentA = r.nextBoolean();
// ... and all r.nextFloat() calls in the mutation loops
```

---

## Optimization 5: Fix Busy-Spin in run() (MEDIUM — saves CPU when paused)

**File:** `ImageEvolver.java` lines 1256-1271
**Problem:**
```java
while (true) {
    while (isRunning) {  // inner loop burns 100% CPU when isRunning=false
        ...
    }
}
```

**Solution:**
```java
@Override
public void run() {
    long start = System.currentTimeMillis();
    while (true) {
        if (isRunning) {
            try {
                evolve(start, ArtEvolver.EVOLVE_ITERATIONS);
            } catch (Exception e) {
                // resilient: ignore and retry
            }
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
```

**Also make `isRunning` volatile (line 1229):**
```java
volatile boolean isRunning = false;
```
This ensures cross-thread visibility when the GUI thread sets it.

---

## Optimization 6: Static Mutable Fields in CrossOver (MEDIUM — thread safety)

**File:** `CrossOver.java`
**Problem in v3:** Multiple `public static` mutable fields are modified by `halveGridSize()`
(called from all threads via `evolve()` line 1166):
- `GRID_MUTATION_CHANCES` (line 20)
- `RANDOM_MUTATION_CHANCES` (line 27)
- `RANDOM_CLOSE_MUTATION_CHANCES` (line 34)

These are modified without synchronization from 24-32 concurrent threads — data race.

**Solution options (pick one):**
1. **Per-instance fields:** Move mutable parameters to instance fields on each CrossOver
   (each ImageEvolver already has its own CrossOver). Pass them through the constructor.
2. **AtomicInteger/volatile:** If shared state is intentional, use `AtomicInteger` or
   `volatile` + careful ordering.
3. **Minimal fix:** Make `halveGridSize()` synchronized and the fields volatile.

Option 1 is cleanest and matches what was done in the artevolver2019 optimization.

---

## Optimization 7: Cache pop.size() in Hot Loops (LOW — micro-optimization)

**File:** `ImageEvolver.java`
**Problem:** `pop.size()` called repeatedly inside the iteration loop of `evolve()`.
While ArrayList.size() is O(1), caching it in a local avoids repeated field access and
enables JIT optimization.

**Solution:** At the start of `evolve()`:
```java
int popSize = pop.size();
```
Then use `popSize` throughout the method. Update it after any `pop.add()`/`pop.remove()`.

---

## v3-Specific Notes (differences from v2.02 that affect porting)

1. **Triangle has `PalleteColor palleteColor` field** — all switch methods also swap
   `palleteColor` along with `color`. The rendering optimization is unaffected by this;
   just ensure the `renderTriangles()` helper does not need to read `palleteColor`.

2. **CrossOver.getChild() is restructured** — v3 uses loop-based mutation chances
   (`for (int a = 0; a < GRID_MUTATION_CHANCES; a++)`) instead of v2's `while(notEvolved)`
   pattern. The ThreadLocalRandom fix applies the same way — replace
   `ImageEvolver.random.nextFloat()` with `ThreadLocalRandom.current().nextFloat()`.

3. **CrossOver.halveGridSize() is static** — this is the main thread-safety concern in v3.
   Called from `evolve()` line 1166 by all threads simultaneously.

4. **switchGridColor() takes a `gridSize` parameter** in v3 — the optimization still applies,
   just preserve the signature.

5. **Fitness-based parent selection** in `evolve()` (sequential mode, lines 858-959) uses
   `random.nextDouble()` — update to `ThreadLocalRandom.current().nextDouble()`.

6. **ArtEvolver.EVOLVE_ITERATIONS = 2** in v3 (vs 1000 in v2) — the image buffer reuse is
   even MORE critical here since the overhead-to-work ratio is higher per iteration.

7. **`isStarted` field exists** (line 1228) alongside `isRunning` — both should be `volatile`.

---

## Recommended Porting Order

1. **AbstractEvolver.java** — drop-in replacement (Opt 1)
2. **ImageEvolver.setResizedOriginal()** — add `cacheReferencePixels()` call
3. **ImageEvolver** — add reusable buffer fields + helpers (Opt 2+3)
4. **ImageEvolver.evolve() / updateFitness()** — use new helpers
5. **ImageEvolver.run()** — fix busy-spin + volatile (Opt 5)
6. **ImageEvolver** — ThreadLocal random (Opt 4)
7. **CrossOver** — ThreadLocalRandom + static field fix (Opt 4+6)
8. **Compile and verify** with `mvn compile` from root

---

## Verification

After applying all optimizations, the project should:
- Compile cleanly with `mvn compile` from the root
- Produce identical fitness scores for the same input (same math, just faster)
- Show significantly higher iterations/second in the stats display
- Use less memory (fewer GC pauses from eliminated BufferedImage allocations)
- Show reduced CPU usage when evolution is paused (no busy-spin)

The existing unit tests (`ImageEvolverTest`, `CrossOverTest`, `ArtEvolverToolsTest`)
should continue to pass unchanged.
