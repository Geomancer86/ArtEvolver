package com.rndmodgames.evolver;

import java.awt.*;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Standalone movable window displaying a real-time fitness chart.
 * Tracks score progression over iterations with auto-scaling axes.
 */
public class FitnessChartWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final List<double[]> dataPoints = new ArrayList<>();
    private final ChartPanel chartPanel;
    private final JLabel lblCurrentScore;
    private final JLabel lblPeakScore;
    private final JLabel lblDataPoints;
    private final JLabel lblGainRate;

    private double peakScore = 0;
    private long firstIterations = -1;
    private double firstScore = 0;

    private static final Color BG_DARK     = new Color(30, 30, 38);
    private static final Color GRID_COLOR  = new Color(55, 55, 70);
    private static final Color AXIS_COLOR  = new Color(140, 140, 160);
    private static final Color LINE_COLOR  = new Color(80, 200, 120);
    private static final Color FILL_TOP    = new Color(80, 200, 120, 60);
    private static final Color FILL_BOT    = new Color(80, 200, 120, 5);
    private static final Color TEXT_COLOR  = new Color(200, 200, 210);
    private static final Color LABEL_COLOR = new Color(160, 160, 175);
    private static final Color PEAK_COLOR  = new Color(255, 200, 60);

    private static final Font AXIS_FONT    = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final Font TITLE_FONT   = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font STAT_FONT    = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font STAT_LBL     = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private static final int MARGIN_LEFT   = 60;
    private static final int MARGIN_RIGHT  = 20;
    private static final int MARGIN_TOP    = 10;
    private static final int MARGIN_BOTTOM = 35;

    private static final int MAX_DATA_POINTS = 10_000;

    public FitnessChartWindow() {
        super("Fitness Progression");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(700, 420);
        setMinimumSize(new Dimension(400, 250));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        JPanel statsBar = new JPanel();
        statsBar.setBackground(new Color(25, 25, 32));
        statsBar.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 6));
        statsBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOR));

        lblCurrentScore = makeStatLabel("Score: --");
        lblPeakScore    = makeStatLabel("Peak: --");
        lblDataPoints   = makeStatLabel("Samples: 0");
        lblGainRate     = makeStatLabel("Gain/min: --");

        statsBar.add(makePair("Score", lblCurrentScore));
        statsBar.add(makePair("Peak", lblPeakScore));
        statsBar.add(makePair("Samples", lblDataPoints));
        statsBar.add(makePair("Gain/min", lblGainRate));

        add(statsBar, BorderLayout.NORTH);

        chartPanel = new ChartPanel();
        add(chartPanel, BorderLayout.CENTER);
    }

    private JLabel makeStatLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(STAT_FONT);
        lbl.setForeground(LINE_COLOR);
        return lbl;
    }

    private JPanel makePair(String label, JLabel value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label);
        lbl.setFont(STAT_LBL);
        lbl.setForeground(LABEL_COLOR);
        p.add(lbl);
        p.add(value);
        return p;
    }

    /**
     * Add a data point and refresh.
     * @param iterations total iterations at this point
     * @param score fitness score (0.0 to 1.0)
     */
    public void addDataPoint(long iterations, double score) {
        if (firstIterations < 0) {
            firstIterations = iterations;
            firstScore = score;
        }
        if (score > peakScore) peakScore = score;

        synchronized (dataPoints) {
            dataPoints.add(new double[]{iterations, score});
            if (dataPoints.size() > MAX_DATA_POINTS) {
                thin();
            }
        }

        DecimalFormat df = new DecimalFormat("0.0000");
        lblCurrentScore.setText(df.format(score * 100) + "%");
        lblPeakScore.setText(df.format(peakScore * 100) + "%");
        lblDataPoints.setText(String.valueOf(dataPoints.size()));

        if (iterations > firstIterations && firstIterations >= 0) {
            double gain = score - firstScore;
            double perIter = gain / (iterations - firstIterations);
            double perMin = perIter * 60_000;
            lblGainRate.setText(df.format(perMin * 100) + "%");
        }

        chartPanel.repaint();
    }

    public void clearData() {
        synchronized (dataPoints) {
            dataPoints.clear();
        }
        peakScore = 0;
        firstIterations = -1;
        firstScore = 0;
        chartPanel.repaint();
    }

    /** Keep every other point when we exceed MAX_DATA_POINTS */
    private void thin() {
        List<double[]> thinned = new ArrayList<>(dataPoints.size() / 2 + 1);
        for (int i = 0; i < dataPoints.size(); i += 2) {
            thinned.add(dataPoints.get(i));
        }
        if (dataPoints.size() % 2 == 0) {
            thinned.add(dataPoints.get(dataPoints.size() - 1));
        }
        dataPoints.clear();
        dataPoints.addAll(thinned);
    }

    private class ChartPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final DecimalFormat dfAxis = new DecimalFormat("0.00");
        private final DecimalFormat dfIter = new DecimalFormat("#,###");

        ChartPanel() {
            setBackground(BG_DARK);
        }

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

            synchronized (dataPoints) {
                drawGrid(g, chartW, chartH);
                if (dataPoints.size() < 2) {
                    g.setColor(LABEL_COLOR);
                    g.setFont(TITLE_FONT);
                    String msg = dataPoints.isEmpty() ? "Waiting for data..." : "Collecting data...";
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(msg,
                            MARGIN_LEFT + (chartW - fm.stringWidth(msg)) / 2,
                            MARGIN_TOP + chartH / 2);
                    return;
                }
                drawChart(g, chartW, chartH);
            }
        }

        private void drawGrid(Graphics2D g, int chartW, int chartH) {
            g.setColor(GRID_COLOR);
            g.setStroke(new BasicStroke(1));

            int hLines = 5;
            for (int i = 0; i <= hLines; i++) {
                int y = MARGIN_TOP + i * chartH / hLines;
                g.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + chartW, y);
            }

            int vLines = 6;
            for (int i = 0; i <= vLines; i++) {
                int x = MARGIN_LEFT + i * chartW / vLines;
                g.drawLine(x, MARGIN_TOP, x, MARGIN_TOP + chartH);
            }

            g.setColor(AXIS_COLOR);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP + chartH);
            g.drawLine(MARGIN_LEFT, MARGIN_TOP + chartH, MARGIN_LEFT + chartW, MARGIN_TOP + chartH);
        }

        private void drawChart(Graphics2D g, int chartW, int chartH) {
            double minScore = Double.MAX_VALUE, maxScore = Double.MIN_VALUE;
            double minIter = dataPoints.get(0)[0];
            double maxIter = dataPoints.get(dataPoints.size() - 1)[0];

            for (double[] dp : dataPoints) {
                if (dp[1] < minScore) minScore = dp[1];
                if (dp[1] > maxScore) maxScore = dp[1];
            }

            double scoreRange = maxScore - minScore;
            if (scoreRange < 0.0001) {
                scoreRange = 0.001;
                minScore = maxScore - scoreRange / 2;
            }
            double scorePad = scoreRange * 0.08;
            minScore -= scorePad;
            maxScore += scorePad;
            scoreRange = maxScore - minScore;

            double iterRange = maxIter - minIter;
            if (iterRange < 1) iterRange = 1;

            g.setFont(AXIS_FONT);
            g.setColor(LABEL_COLOR);
            FontMetrics fm = g.getFontMetrics();

            int hLines = 5;
            for (int i = 0; i <= hLines; i++) {
                double val = maxScore - i * scoreRange / hLines;
                int y = MARGIN_TOP + i * chartH / hLines;
                String label = dfAxis.format(val * 100) + "%";
                g.drawString(label, MARGIN_LEFT - fm.stringWidth(label) - 4, y + fm.getAscent() / 2);
            }

            int vLines = 6;
            for (int i = 0; i <= vLines; i++) {
                double val = minIter + i * iterRange / vLines;
                int x = MARGIN_LEFT + i * chartW / vLines;
                String label = formatIterations((long) val);
                g.drawString(label, x - fm.stringWidth(label) / 2, MARGIN_TOP + chartH + fm.getHeight() + 2);
            }

            Path2D.Double linePath = new Path2D.Double();
            Path2D.Double fillPath = new Path2D.Double();
            boolean first = true;

            for (double[] dp : dataPoints) {
                double px = MARGIN_LEFT + ((dp[0] - minIter) / iterRange) * chartW;
                double py = MARGIN_TOP + ((maxScore - dp[1]) / scoreRange) * chartH;

                if (first) {
                    linePath.moveTo(px, py);
                    fillPath.moveTo(px, MARGIN_TOP + chartH);
                    fillPath.lineTo(px, py);
                    first = false;
                } else {
                    linePath.lineTo(px, py);
                    fillPath.lineTo(px, py);
                }
            }

            double[] last = dataPoints.get(dataPoints.size() - 1);
            double lastX = MARGIN_LEFT + ((last[0] - minIter) / iterRange) * chartW;
            fillPath.lineTo(lastX, MARGIN_TOP + chartH);
            fillPath.closePath();

            GradientPaint grad = new GradientPaint(
                    0, MARGIN_TOP, FILL_TOP,
                    0, MARGIN_TOP + chartH, FILL_BOT);
            g.setPaint(grad);
            g.fill(fillPath);

            g.setColor(LINE_COLOR);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linePath);

            double lastPx = lastX;
            double lastPy = MARGIN_TOP + ((maxScore - last[1]) / scoreRange) * chartH;
            g.setColor(LINE_COLOR);
            g.fillOval((int) lastPx - 4, (int) lastPy - 4, 8, 8);

            if (peakScore > 0) {
                double peakPy = MARGIN_TOP + ((maxScore - peakScore) / scoreRange) * chartH;
                g.setColor(PEAK_COLOR);
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{4, 4}, 0));
                g.drawLine(MARGIN_LEFT, (int) peakPy, MARGIN_LEFT + chartW, (int) peakPy);
                g.setFont(AXIS_FONT);
                g.drawString("peak " + dfAxis.format(peakScore * 100) + "%",
                        MARGIN_LEFT + 4, (int) peakPy - 3);
            }
        }

        private String formatIterations(long val) {
            if (val >= 1_000_000) return dfIter.format(val / 1_000_000) + "M";
            if (val >= 1_000) return dfIter.format(val / 1_000) + "K";
            return dfIter.format(val);
        }
    }
}
