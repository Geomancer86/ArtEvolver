package com.rndmodgames.evolver;

import java.awt.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * Standalone movable window displaying a real-time fitness chart.
 * Supports multiple named data series with distinct colors for Tournament Mode.
 */
public class FitnessChartWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final Map<String, Series> seriesMap = new LinkedHashMap<>();
    private final ChartPanel chartPanel;
    private final JPanel statsBar;

    private static final Color BG_DARK     = new Color(30, 30, 38);
    private static final Color GRID_COLOR  = new Color(55, 55, 70);
    private static final Color AXIS_COLOR  = new Color(140, 140, 160);
    private static final Color TEXT_COLOR  = new Color(200, 200, 210);
    private static final Color LABEL_COLOR = new Color(160, 160, 175);

    private static final Font AXIS_FONT    = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font TITLE_FONT   = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font STAT_FONT    = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font STAT_LBL     = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font LEGEND_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private static final int MARGIN_LEFT   = 60;
    private static final int MARGIN_RIGHT  = 20;
    private static final int MARGIN_TOP    = 10;
    private static final int MARGIN_BOTTOM = 50;

    private static final int MAX_DATA_POINTS = 10_000;

    public FitnessChartWindow() {
        super("Fitness Progression");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(780, 480);
        setMinimumSize(new Dimension(400, 250));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        statsBar = new JPanel();
        statsBar.setBackground(new Color(25, 25, 32));
        statsBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));
        statsBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOR));
        add(statsBar, BorderLayout.NORTH);

        chartPanel = new ChartPanel();
        add(chartPanel, BorderLayout.CENTER);
    }

    /**
     * Add a data point to a named series.
     */
    public void addDataPoint(String seriesId, String displayName, long iterations, double score, Color color) {
        Series s;
        synchronized (seriesMap) {
            s = seriesMap.computeIfAbsent(seriesId, k -> {
                Series ns = new Series(displayName, color);
                rebuildStatsBar();
                return ns;
            });
        }
        s.name = displayName;
        s.color = color;
        synchronized (s.data) {
            if (s.firstIterations < 0) {
                s.firstIterations = iterations;
                s.firstScore = score;
            }
            if (score > s.peakScore) s.peakScore = score;
            s.data.add(new double[]{iterations, score});
            if (s.data.size() > MAX_DATA_POINTS) thin(s.data);
            s.latestScore = score;
        }
        updateStatsLabel(s);
        chartPanel.repaint();
    }

    /** Backward-compatible single-series method. */
    public void addDataPoint(long iterations, double score) {
        addDataPoint("default", "Default", iterations, score, new Color(80, 200, 120));
    }

    public void clearData() {
        synchronized (seriesMap) {
            seriesMap.clear();
        }
        rebuildStatsBar();
        chartPanel.repaint();
    }

    public void clearSeries(String seriesId) {
        synchronized (seriesMap) {
            seriesMap.remove(seriesId);
        }
        rebuildStatsBar();
        chartPanel.repaint();
    }

    private void rebuildStatsBar() {
        SwingUtilities.invokeLater(() -> {
            statsBar.removeAll();
            synchronized (seriesMap) {
                for (Series s : seriesMap.values()) {
                    statsBar.add(makeSeriesStat(s));
                }
            }
            if (seriesMap.isEmpty()) {
                JLabel empty = new JLabel("No data yet");
                empty.setFont(STAT_LBL);
                empty.setForeground(LABEL_COLOR);
                statsBar.add(empty);
            }
            statsBar.revalidate();
            statsBar.repaint();
        });
    }

    private JPanel makeSeriesStat(Series s) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel nameLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        nameLine.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(s.color);
                g.fillOval(0, 0, 10, 10);
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 10));
        nameLine.add(dot);
        JLabel lblName = new JLabel(s.name);
        lblName.setFont(STAT_LBL);
        lblName.setForeground(s.color);
        nameLine.add(lblName);
        p.add(nameLine);

        s.lblValue = new JLabel("--");
        s.lblValue.setFont(STAT_FONT);
        s.lblValue.setForeground(s.color);
        p.add(s.lblValue);

        return p;
    }

    private void updateStatsLabel(Series s) {
        if (s.lblValue != null) {
            DecimalFormat df = new DecimalFormat("0.0000");
            s.lblValue.setText(df.format(s.latestScore * 100) + "% (pk:" + df.format(s.peakScore * 100) + "%)");
        }
    }

    private void thin(List<double[]> data) {
        List<double[]> thinned = new ArrayList<>(data.size() / 2 + 1);
        for (int i = 0; i < data.size(); i += 2) thinned.add(data.get(i));
        if (data.size() % 2 == 0) thinned.add(data.get(data.size() - 1));
        data.clear();
        data.addAll(thinned);
    }

    static class Series {
        String name;
        Color color;
        final List<double[]> data = new ArrayList<>();
        double peakScore = 0;
        double latestScore = 0;
        long firstIterations = -1;
        double firstScore = 0;
        JLabel lblValue;

        Series(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    private class ChartPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final DecimalFormat dfAxis = new DecimalFormat("0.00");
        private final DecimalFormat dfIter = new DecimalFormat("#,###");

        ChartPanel() { setBackground(BG_DARK); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int chartW = w - MARGIN_LEFT - MARGIN_RIGHT;
            int chartH = h - MARGIN_TOP - MARGIN_BOTTOM;
            if (chartW < 10 || chartH < 10) return;

            drawGrid(g, chartW, chartH);

            List<Series> allSeries;
            synchronized (seriesMap) {
                allSeries = new ArrayList<>(seriesMap.values());
            }

            boolean hasData = false;
            for (Series s : allSeries) {
                synchronized (s.data) {
                    if (!s.data.isEmpty()) { hasData = true; break; }
                }
            }

            if (!hasData) {
                g.setColor(LABEL_COLOR);
                g.setFont(TITLE_FONT);
                String msg = "Waiting for data...";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, MARGIN_LEFT + (chartW - fm.stringWidth(msg)) / 2, MARGIN_TOP + chartH / 2);
                return;
            }

            double minScore = Double.MAX_VALUE, maxScore = Double.MIN_VALUE;
            double minIter = Double.MAX_VALUE, maxIter = Double.MIN_VALUE;

            for (Series s : allSeries) {
                synchronized (s.data) {
                    for (double[] dp : s.data) {
                        if (dp[1] < minScore) minScore = dp[1];
                        if (dp[1] > maxScore) maxScore = dp[1];
                        if (dp[0] < minIter) minIter = dp[0];
                        if (dp[0] > maxIter) maxIter = dp[0];
                    }
                }
            }

            double scoreRange = maxScore - minScore;
            if (scoreRange < 0.0001) { scoreRange = 0.001; minScore = maxScore - scoreRange / 2; }
            double pad = scoreRange * 0.08;
            minScore -= pad; maxScore += pad; scoreRange = maxScore - minScore;
            double iterRange = maxIter - minIter;
            if (iterRange < 1) iterRange = 1;

            drawAxes(g, chartW, chartH, minScore, maxScore, scoreRange, minIter, iterRange);

            for (Series s : allSeries) {
                synchronized (s.data) {
                    if (s.data.isEmpty()) continue;
                    drawSeries(g, s, chartW, chartH, minScore, maxScore, scoreRange, minIter, iterRange);
                }
            }

            drawLegend(g, allSeries, chartW, chartH);
        }

        private void drawGrid(Graphics2D g, int chartW, int chartH) {
            g.setColor(GRID_COLOR);
            g.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 5; i++) {
                int y = MARGIN_TOP + i * chartH / 5;
                g.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + chartW, y);
            }
            for (int i = 0; i <= 6; i++) {
                int x = MARGIN_LEFT + i * chartW / 6;
                g.drawLine(x, MARGIN_TOP, x, MARGIN_TOP + chartH);
            }
            g.setColor(AXIS_COLOR);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + chartH);
            g.drawLine(MARGIN_LEFT, MARGIN_TOP + chartH, MARGIN_LEFT + chartW, MARGIN_TOP + chartH);
        }

        private void drawAxes(Graphics2D g, int cW, int cH, double minS, double maxS, double sR, double minI, double iR) {
            g.setFont(AXIS_FONT);
            g.setColor(LABEL_COLOR);
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= 5; i++) {
                double val = maxS - i * sR / 5;
                int y = MARGIN_TOP + i * cH / 5;
                String label = dfAxis.format(val * 100) + "%";
                g.drawString(label, MARGIN_LEFT - fm.stringWidth(label) - 4, y + fm.getAscent() / 2);
            }
            for (int i = 0; i <= 6; i++) {
                double val = minI + i * iR / 6;
                int x = MARGIN_LEFT + i * cW / 6;
                String label = formatIter((long) val);
                g.drawString(label, x - fm.stringWidth(label) / 2, MARGIN_TOP + cH + fm.getHeight() + 2);
            }
        }

        private void drawSeries(Graphics2D g, Series s, int cW, int cH,
                                double minS, double maxS, double sR, double minI, double iR) {
            if (s.data.size() == 1) {
                double[] dp = s.data.get(0);
                double px = MARGIN_LEFT + ((dp[0] - minI) / iR) * cW;
                double py = MARGIN_TOP + ((maxS - dp[1]) / sR) * cH;
                g.setColor(s.color);
                g.fillOval((int) px - 5, (int) py - 5, 10, 10);
                return;
            }

            Path2D.Double line = new Path2D.Double();
            Path2D.Double fill = new Path2D.Double();
            boolean first = true;

            for (double[] dp : s.data) {
                double px = MARGIN_LEFT + ((dp[0] - minI) / iR) * cW;
                double py = MARGIN_TOP + ((maxS - dp[1]) / sR) * cH;
                if (first) {
                    line.moveTo(px, py);
                    fill.moveTo(px, MARGIN_TOP + cH);
                    fill.lineTo(px, py);
                    first = false;
                } else {
                    line.lineTo(px, py);
                    fill.lineTo(px, py);
                }
            }

            double[] last = s.data.get(s.data.size() - 1);
            double lastX = MARGIN_LEFT + ((last[0] - minI) / iR) * cW;
            double lastY = MARGIN_TOP + ((maxS - last[1]) / sR) * cH;
            fill.lineTo(lastX, MARGIN_TOP + cH);
            fill.closePath();

            Color fillTop = new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), 40);
            Color fillBot = new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), 5);
            g.setPaint(new GradientPaint(0, MARGIN_TOP, fillTop, 0, MARGIN_TOP + cH, fillBot));
            g.fill(fill);

            g.setColor(s.color);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);

            g.fillOval((int) lastX - 4, (int) lastY - 4, 8, 8);
        }

        private void drawLegend(Graphics2D g, List<Series> allSeries, int cW, int cH) {
            if (allSeries.size() <= 1) return;

            g.setFont(LEGEND_FONT);
            FontMetrics fm = g.getFontMetrics();
            int x = MARGIN_LEFT + 8;
            int y = MARGIN_TOP + cH + 22;

            for (Series s : allSeries) {
                g.setColor(s.color);
                g.fillRect(x, y - 8, 12, 12);
                g.setColor(TEXT_COLOR);
                String label = s.name + " " + new DecimalFormat("0.00").format(s.latestScore * 100) + "%";
                g.drawString(label, x + 16, y + 2);
                x += fm.stringWidth(label) + 30;
                if (x > MARGIN_LEFT + cW - 50) { x = MARGIN_LEFT + 8; y += 16; }
            }
        }

        private String formatIter(long val) {
            if (val >= 1_000_000) return dfIter.format(val / 1_000_000) + "M";
            if (val >= 1_000) return dfIter.format(val / 1_000) + "K";
            return dfIter.format(val);
        }
    }
}
