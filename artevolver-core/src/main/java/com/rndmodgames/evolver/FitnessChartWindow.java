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
 * X-axis can toggle between total iterations and elapsed time.
 */
public class FitnessChartWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    /** X-axis mode: 0 = iterations, 1 = elapsed time */
    private int xAxisMode = 0;

    private final Map<String, Series> seriesMap = new LinkedHashMap<>();
    private final Set<String> activeSeriesIds = Collections.synchronizedSet(new HashSet<>());
    private boolean cullingEnabled = true;
    private int maxVisibleSeries = 25;
    private JCheckBox chkCulling;
    private JSpinner spnMaxSeries;
    private JLabel lblCullingStatus;

    private final ChartPanel chartPanel;
    private final JPanel statsBar;
    private final JPanel toolBar;
    private final JToggleButton btnIterations;
    private final JToggleButton btnTime;

    private static final Color BG_DARK     = new Color(30, 30, 38);
    private static final Color GRID_COLOR  = new Color(55, 55, 70);
    private static final Color AXIS_COLOR  = new Color(140, 140, 160);
    private static final Color TEXT_COLOR  = new Color(200, 200, 210);
    private static final Color LABEL_COLOR = new Color(160, 160, 175);
    private static final Color ACCENT      = new Color(80, 200, 120);

    private static final Font AXIS_FONT    = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font TITLE_FONT   = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font STAT_FONT    = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font STAT_LBL     = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font LEGEND_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Font BTN_FONT     = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private static final int MARGIN_LEFT   = 60;
    private static final int MARGIN_RIGHT  = 20;
    private static final int MARGIN_TOP    = 10;
    private static final int MARGIN_BOTTOM = 50;

    private static final int MAX_DATA_POINTS = 10_000;

    public FitnessChartWindow() {
        super("Fitness Progression");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setMinimumSize(new Dimension(400, 250));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        chartPanel = new ChartPanel();
        chartPanel.setPreferredSize(new Dimension(850, 520));

        // Top bar: stats + axis toggle
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(25, 25, 32));

        statsBar = new JPanel();
        statsBar.setBackground(new Color(25, 25, 32));
        statsBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));
        topPanel.add(statsBar, BorderLayout.CENTER);

        toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
        toolBar.setBackground(new Color(25, 25, 32));

        JLabel lblAxis = new JLabel("X-Axis:");
        lblAxis.setFont(STAT_LBL);
        lblAxis.setForeground(LABEL_COLOR);
        toolBar.add(lblAxis);

        ButtonGroup bg = new ButtonGroup();
        btnIterations = makeToggle("Iterations", true);
        btnTime = makeToggle("Time", false);
        bg.add(btnIterations);
        bg.add(btnTime);
        btnIterations.addActionListener(e -> { xAxisMode = 0; chartPanel.repaint(); });
        btnTime.addActionListener(e -> { xAxisMode = 1; chartPanel.repaint(); });
        toolBar.add(btnIterations);
        toolBar.add(btnTime);

        toolBar.add(Box.createHorizontalStrut(12));

        chkCulling = new JCheckBox("Top", cullingEnabled);
        chkCulling.setFont(STAT_LBL);
        chkCulling.setForeground(LABEL_COLOR);
        chkCulling.setOpaque(false);
        chkCulling.setToolTipText("Limit chart to the top N contestants plus any currently running.");
        chkCulling.addActionListener(e -> {
            cullingEnabled = chkCulling.isSelected();
            spnMaxSeries.setEnabled(cullingEnabled);
            chartPanel.repaint();
        });
        toolBar.add(chkCulling);

        spnMaxSeries = new JSpinner(new SpinnerNumberModel(maxVisibleSeries, 5, 100, 5));
        spnMaxSeries.setFont(STAT_LBL);
        spnMaxSeries.setPreferredSize(new Dimension(55, 22));
        spnMaxSeries.setToolTipText("Max chart series to display (top N by score + all active).");
        spnMaxSeries.addChangeListener(ev -> {
            maxVisibleSeries = ((Number) spnMaxSeries.getValue()).intValue();
            chartPanel.repaint();
        });
        toolBar.add(spnMaxSeries);

        lblCullingStatus = new JLabel("");
        lblCullingStatus.setFont(STAT_LBL);
        lblCullingStatus.setForeground(new Color(120, 120, 140));
        toolBar.add(lblCullingStatus);

        topPanel.add(toolBar, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOR));
        add(topPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        pack();
        sizeToScreen();
    }

    private JToggleButton makeToggle(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(BTN_FONT);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(85, 22));
        btn.setBackground(selected ? ACCENT : new Color(50, 50, 60));
        btn.setForeground(selected ? Color.BLACK : TEXT_COLOR);
        btn.addChangeListener(e -> {
            btn.setBackground(btn.isSelected() ? ACCENT : new Color(50, 50, 60));
            btn.setForeground(btn.isSelected() ? Color.BLACK : TEXT_COLOR);
        });
        return btn;
    }

    private void sizeToScreen() {
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(
            getGraphicsConfiguration());
        int usableW = screenSize.width - insets.left - insets.right;
        int usableH = screenSize.height - insets.top - insets.bottom;

        int w = Math.min(Math.max(getWidth(), 850), (int) (usableW * 0.65));
        int h = Math.min(Math.max(getHeight(), 550), (int) (usableH * 0.7));
        setSize(w, h);
        setLocationRelativeTo(null);
    }

    /**
     * Add a data point to a named series.
     * Stores iterations, score, AND wall-clock timestamp for dual-axis support.
     */
    public void addDataPoint(String seriesId, String displayName,
                             long iterations, double score, Color color) {
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
        long now = System.currentTimeMillis();
        synchronized (s.data) {
            if (s.firstTimeMs < 0) {
                s.firstTimeMs = now;
                s.firstIterations = iterations;
                s.firstScore = score;
            }
            if (score > s.peakScore) s.peakScore = score;
            // [0]=iterations, [1]=score, [2]=elapsed ms since first data point
            s.data.add(new double[]{iterations, score, now - s.firstTimeMs});
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

    /** Mark a series as actively running (always shown regardless of culling). */
    public void setSeriesActive(String seriesId, boolean active) {
        if (active) {
            activeSeriesIds.add(seriesId);
        } else {
            activeSeriesIds.remove(seriesId);
        }
    }

    public void clearData() {
        synchronized (seriesMap) {
            seriesMap.clear();
        }
        activeSeriesIds.clear();
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

    /**
     * Returns the series that should be rendered, applying culling if enabled.
     * Active (running) series are always included. Remaining slots go to top
     * series by peak score.
     */
    private List<Series> getVisibleSeries() {
        List<Series> all;
        synchronized (seriesMap) {
            all = new ArrayList<>(seriesMap.values());
        }
        if (!cullingEnabled || all.size() <= maxVisibleSeries) {
            updateCullingLabel(all.size(), all.size());
            return all;
        }

        List<Series> visible = new ArrayList<>();
        List<Series> candidates = new ArrayList<>();

        for (Series s : all) {
            if (activeSeriesIds.contains(getSeriesId(s))) {
                visible.add(s);
            } else {
                candidates.add(s);
            }
        }

        candidates.sort((a, b) -> Double.compare(b.peakScore, a.peakScore));
        int remaining = maxVisibleSeries - visible.size();
        for (int i = 0; i < Math.min(remaining, candidates.size()); i++) {
            visible.add(candidates.get(i));
        }

        updateCullingLabel(visible.size(), all.size());
        return visible;
    }

    private String getSeriesId(Series s) {
        synchronized (seriesMap) {
            for (Map.Entry<String, Series> entry : seriesMap.entrySet()) {
                if (entry.getValue() == s) return entry.getKey();
            }
        }
        return "";
    }

    private void updateCullingLabel(int shown, int total) {
        if (cullingEnabled && shown < total) {
            lblCullingStatus.setText(" " + shown + "/" + total);
        } else {
            lblCullingStatus.setText("");
        }
    }

    static class Series {
        String name;
        Color color;
        final List<double[]> data = new ArrayList<>(); // [iterations, score, elapsedMs]
        double peakScore = 0;
        double latestScore = 0;
        long firstIterations = -1;
        long firstTimeMs = -1;
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

            List<Series> allSeries = getVisibleSeries();

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

            // X-value index: 0=iterations, 2=elapsed ms
            int xIdx = (xAxisMode == 1) ? 2 : 0;

            double minScore = Double.MAX_VALUE, maxScore = Double.MIN_VALUE;
            double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;

            for (Series s : allSeries) {
                synchronized (s.data) {
                    for (double[] dp : s.data) {
                        if (dp[1] < minScore) minScore = dp[1];
                        if (dp[1] > maxScore) maxScore = dp[1];
                        double xVal = dp.length > xIdx ? dp[xIdx] : dp[0];
                        if (xVal < minX) minX = xVal;
                        if (xVal > maxX) maxX = xVal;
                    }
                }
            }

            double scoreRange = maxScore - minScore;
            if (scoreRange < 0.0001) { scoreRange = 0.001; minScore = maxScore - scoreRange / 2; }
            double pad = scoreRange * 0.08;
            minScore -= pad; maxScore += pad; scoreRange = maxScore - minScore;
            double xRange = maxX - minX;
            if (xRange < 1) xRange = 1;

            drawAxes(g, chartW, chartH, minScore, maxScore, scoreRange, minX, xRange);

            for (Series s : allSeries) {
                synchronized (s.data) {
                    if (s.data.isEmpty()) continue;
                    drawSeries(g, s, chartW, chartH, minScore, maxScore, scoreRange, minX, xRange, xIdx);
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

        private void drawAxes(Graphics2D g, int cW, int cH,
                              double minS, double maxS, double sR, double minX, double xR) {
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
                double val = minX + i * xR / 6;
                int x = MARGIN_LEFT + i * cW / 6;
                String label = (xAxisMode == 1) ? formatTime((long) val) : formatIter((long) val);
                g.drawString(label, x - fm.stringWidth(label) / 2, MARGIN_TOP + cH + fm.getHeight() + 2);
            }

            // Axis title
            g.setFont(STAT_LBL);
            String axisTitle = (xAxisMode == 1) ? "Elapsed Time" : "Iterations";
            int titleW = g.getFontMetrics().stringWidth(axisTitle);
            g.setColor(AXIS_COLOR);
            g.drawString(axisTitle, MARGIN_LEFT + (cW - titleW) / 2, MARGIN_TOP + cH + fm.getHeight() + 16);
        }

        private void drawSeries(Graphics2D g, Series s, int cW, int cH,
                                double minS, double maxS, double sR,
                                double minX, double xR, int xIdx) {
            if (s.data.size() == 1) {
                double[] dp = s.data.get(0);
                double xVal = dp.length > xIdx ? dp[xIdx] : dp[0];
                double px = MARGIN_LEFT + ((xVal - minX) / xR) * cW;
                double py = MARGIN_TOP + ((maxS - dp[1]) / sR) * cH;
                g.setColor(s.color);
                g.fillOval((int) px - 5, (int) py - 5, 10, 10);
                return;
            }

            Path2D.Double line = new Path2D.Double();
            Path2D.Double fill = new Path2D.Double();
            boolean first = true;

            for (double[] dp : s.data) {
                double xVal = dp.length > xIdx ? dp[xIdx] : dp[0];
                double px = MARGIN_LEFT + ((xVal - minX) / xR) * cW;
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
            double lastXVal = last.length > xIdx ? last[xIdx] : last[0];
            double lastX = MARGIN_LEFT + ((lastXVal - minX) / xR) * cW;
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

        private String formatTime(long ms) {
            long totalSec = ms / 1000;
            if (totalSec < 60) return totalSec + "s";
            long min = totalSec / 60;
            long sec = totalSec % 60;
            if (min < 60) return min + "m" + (sec > 0 ? sec + "s" : "");
            long hrs = min / 60;
            min = min % 60;
            return hrs + "h" + (min > 0 ? min + "m" : "");
        }
    }
}
