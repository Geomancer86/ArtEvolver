package com.rndmodgames.evolver.exporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.rndmodgames.evolver.PalleteColor;
import com.rndmodgames.evolver.Triangle;

/**
 * Exports a paint-by-colors guide from the evolved triangle arrangement.
 * 
 * Generates:
 *   1. A console-friendly summary of all triangles and their assigned palette colors
 *   2. A CSV file mapping each triangle position to its Sherwin-Williams color name/ID
 *   3. A materials list (bill of materials) summarizing how many of each color are needed
 * 
 * This is the key bridge between the digital GA optimization and the
 * physical mural construction using real Sherwin-Williams color decks.
 */
public class PaintByColorsExporter {

    public PaintByColorsExporter() {}

    /**
     * Prints a text summary of the color assignment to stdout.
     */
    public static void printColorAssignment(List<Triangle> drawing) {
        int triangleCount = 0;

        System.out.println("================================================================================");
        System.out.println("  PAINT-BY-COLORS ASSIGNMENT");
        System.out.println("================================================================================");

        for (Triangle triangle : drawing) {
            PalleteColor pc = triangle.getPalleteColor();

            System.out.println("TRIANGLE #" + triangleCount);
            if (pc != null) {
                System.out.println("  COLOR ID : " + pc.getId());
                System.out.println("  COLOR    : " + pc.getName());
                System.out.println("  RGB      : (" + pc.getColor().getRed() + ", "
                                                     + pc.getColor().getGreen() + ", "
                                                     + pc.getColor().getBlue() + ")");
            } else {
                System.out.println("  COLOR    : <unassigned>");
            }

            triangleCount++;
            System.out.println("--------------------------------------------------------------------------------");
        }

        System.out.println("TOTAL TRIANGLES: " + triangleCount);
    }

    /**
     * Exports a CSV file with columns: TriangleIndex, ColorId, ColorName, R, G, B
     * 
     * @return true if export succeeded
     */
    public static boolean exportToCSV(List<Triangle> drawing, String folder, String sourceName) {
        String filename = folder + sourceName + "_paint_by_colors.csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(filename)))) {
            pw.println("TriangleIndex,ColorId,ColorName,R,G,B");

            for (int i = 0; i < drawing.size(); i++) {
                Triangle tri = drawing.get(i);
                PalleteColor pc = tri.getPalleteColor();

                if (pc != null) {
                    pw.println(i + "," + pc.getId() + "," + escapeCsv(pc.getName()) + ","
                             + pc.getColor().getRed() + ","
                             + pc.getColor().getGreen() + ","
                             + pc.getColor().getBlue());
                } else {
                    pw.println(i + ",,,,,");
                }
            }

            System.out.println("[PaintByColors] CSV exported: " + filename);
            return true;

        } catch (IOException e) {
            System.err.println("[PaintByColors] Failed to export CSV: " + filename + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Exports a bill of materials: how many triangles use each color,
     * sorted by color name for easy shopping/cutting reference.
     */
    public static boolean exportMaterialsList(List<Triangle> drawing, String folder, String sourceName) {
        String filename = folder + sourceName + "_materials_list.csv";

        Map<String, int[]> colorCounts = new TreeMap<>();
        int unassigned = 0;

        for (Triangle tri : drawing) {
            PalleteColor pc = tri.getPalleteColor();
            if (pc == null || pc.getName() == null) {
                unassigned++;
                continue;
            }

            String key = pc.getName();
            int[] entry = colorCounts.computeIfAbsent(key, k -> new int[]{0,
                    pc.getColor().getRed(), pc.getColor().getGreen(), pc.getColor().getBlue()});
            entry[0]++;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(filename)))) {
            pw.println("ColorName,Count,R,G,B");

            for (Map.Entry<String, int[]> e : colorCounts.entrySet()) {
                int[] v = e.getValue();
                pw.println(escapeCsv(e.getKey()) + "," + v[0] + "," + v[1] + "," + v[2] + "," + v[3]);
            }

            if (unassigned > 0) {
                pw.println("<unassigned>," + unassigned + ",,,");
            }

            System.out.println("[PaintByColors] Materials list exported: " + filename
                             + " (" + colorCounts.size() + " unique colors, " + drawing.size() + " triangles)");
            return true;

        } catch (IOException e) {
            System.err.println("[PaintByColors] Failed to export materials: " + filename + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates that every triangle has a PalleteColor assigned and reports any gaps.
     * 
     * @return number of triangles missing a PalleteColor assignment
     */
    public static int validateAssignment(List<Triangle> drawing) {
        int missing = 0;
        for (int i = 0; i < drawing.size(); i++) {
            if (drawing.get(i).getPalleteColor() == null) {
                missing++;
            }
        }

        if (missing > 0) {
            System.out.println("[PaintByColors] WARNING: " + missing + " of " + drawing.size()
                             + " triangles have no palette color assigned!");
        } else {
            System.out.println("[PaintByColors] Validation passed: all " + drawing.size()
                             + " triangles have palette colors assigned.");
        }

        return missing;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
