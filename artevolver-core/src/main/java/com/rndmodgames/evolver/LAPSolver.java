package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Jonker-Volgenant Linear Assignment Problem solver.
 *
 * Solves the problem: given an N x N integer cost matrix, find the permutation
 * sigma that minimizes SUM of cost[i][sigma[i]].
 *
 * For ArtEvolver, cost[i][j] = total pixel error when triangle i has color j.
 * The solution is the provably optimal color assignment.
 *
 * Algorithm: Jonker & Volgenant (1987), "A shortest augmenting path algorithm
 * for dense and sparse linear assignment problems." Computing 38, 325-340.
 *
 * Time: O(N^3), Space: O(N^2) for cost matrix + O(N) working arrays.
 */
public class LAPSolver {

    /**
     * Solves the LAP. Returns the optimal column assignment for each row.
     *
     * @param cost flat N*N cost matrix in row-major order: cost[i*n + j]
     * @param n matrix dimension
     * @return int[n] where result[i] = column assigned to row i
     */
    public static int[] solve(int[] cost, int n) {
        int[] u = new int[n + 1]; // row dual variables
        int[] v = new int[n + 1]; // column dual variables
        int[] p = new int[n + 1]; // column -> row assignment (1-indexed)
        int[] way = new int[n + 1]; // augmenting path
        int[] minv = new int[n + 1]; // minimum reduced cost to each column
        boolean[] used = new boolean[n + 1]; // visited columns

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            java.util.Arrays.fill(minv, Integer.MAX_VALUE);
            java.util.Arrays.fill(used, false);

            do {
                used[j0] = true;
                int i0 = p[j0], delta = Integer.MAX_VALUE, j1 = -1;
                for (int j = 1; j <= n; j++) {
                    if (used[j]) continue;
                    int cur = cost[(i0 - 1) * n + (j - 1)] - u[i0] - v[j];
                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }
                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }

                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }

                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] result = new int[n];
        for (int j = 1; j <= n; j++) {
            result[p[j] - 1] = j - 1;
        }
        return result;
    }

    /**
     * Builds the cost matrix for ArtEvolver: cost[i][j] = pixel error when
     * triangle i is filled with color j.
     *
     * Uses DeltaFitnessEngine's pre-computed pixel masks for efficient computation.
     *
     * @param engine the initialized DeltaFitnessEngine (provides pixel masks and ref image)
     * @param colors the palette colors (N colors matching N triangles)
     * @return flat int[N*N] cost matrix in row-major order
     */
    public static int[] buildCostMatrix(DeltaFitnessEngine engine, Color[] colors) {
        int n = engine.getTriangleCount();
        if (n != colors.length) {
            throw new IllegalArgumentException("Triangle count (" + n +
                ") != color count (" + colors.length + ")");
        }

        int[] cost = new int[n * n];

        int[] colR = new int[n], colG = new int[n], colB = new int[n];
        for (int j = 0; j < n; j++) {
            colR[j] = colors[j].getRed();
            colG[j] = colors[j].getGreen();
            colB[j] = colors[j].getBlue();
        }

        for (int i = 0; i < n; i++) {
            int rowBase = i * n;
            int pixCount = engine.getPixelCount(i);
            int[] pixels = engine.getTrianglePixelIndices(i);
            int[] rr = engine.getRefR();
            int[] rg = engine.getRefG();
            int[] rb = engine.getRefB();

            for (int j = 0; j < n; j++) {
                int err = 0;
                int cr = colR[j], cg = colG[j], cb = colB[j];
                for (int p = 0; p < pixCount; p++) {
                    int px = pixels[p];
                    err += Math.abs(rr[px] - cr) + Math.abs(rg[px] - cg) + Math.abs(rb[px] - cb);
                }
                cost[rowBase + j] = err;
            }
        }

        return cost;
    }

    /**
     * Convenience: builds cost matrix and solves in one call.
     * Returns the optimal color index for each triangle.
     */
    public static int[] solveOptimal(DeltaFitnessEngine engine, Color[] colors) {
        long t0 = System.nanoTime();
        int[] cost = buildCostMatrix(engine, colors);
        long buildMs = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        int[] assignment = solve(cost, engine.getTriangleCount());
        long solveMs = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("[LAP] Cost matrix: " + buildMs + "ms, Solve: " + solveMs + "ms" +
            " (N=" + engine.getTriangleCount() + ")");
        return assignment;
    }
}
