package com.rndmodgames.evolver;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Tournament Manager: a separate window for creating, configuring, and
 * monitoring multiple independent evolution contestants.
 */
public class TournamentManagerWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ArtEvolver artEvolver;
    private final List<TournamentContestant> contestants;
    private final ContestantTableModel tableModel;
    private final JTable table;
    private final JPanel detailPanel;
    private int nextId = 1;

    private EvolutionaryTournament evoTournament;
    private PrehistoricMode prehistoricMode;
    private JLabel lblGeneration;
    private JLabel lblCountdown;
    private JLabel lblBestEver;
    private JTextArea txtHistory;
    private int lastHistoryRecordCount = 0;
    private JButton btnEvolve;
    private JButton btnQuickSetupRef;
    private JButton btnStartAllRef;
    private JButton btnStopAllRef;
    private JCheckBox chkAutoEvolve;

    // System Monitor + Autopilot + Dashboard
    private final SystemMonitor sysMonitor = new SystemMonitor();
    private DashboardServer dashboardServer;
    private JLabel lblSysStatus;
    private boolean autopilotActive = false;
    private double maxCpuPercent = 80;
    private double maxRamPercent = 85;
    private double maxHeapPercent = 80;
    private long lastSpawnMs = 0;
    private static final long SPAWN_COOLDOWN_MS = 5000;
    private static final int MAX_TOTAL_THREADS = Runtime.getRuntime().availableProcessors();
    private int maxAliveContestants = 12;

    // Prehistoric Mode UI
    private JPanel prehistoricPanel;
    private JLabel lblEra;
    private JLabel lblEraDesc;
    private JLabel lblEraCountdown;
    private JLabel lblCapabilities;
    private JButton btnAdvanceEra;
    private JButton btnAddThread;
    private JButton btnAddPreset;
    private JButton btnAddEvolved;
    private JButton btnStartPrehistoric;
    private JCheckBox chkAutoAdvance;
    private JProgressBar eraProgress;

    private static final Color BG = new Color(240, 240, 244);
    private static final Color HEADER_BG = new Color(50, 55, 70);
    private static final Font TBL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font HDR_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font SECTION_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    public TournamentManagerWindow(ArtEvolver artEvolver, List<TournamentContestant> contestants) {
        super("Tournament Manager");
        this.artEvolver = artEvolver;
        this.contestants = contestants;
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(900, 520);
        setMinimumSize(new Dimension(700, 350));
        getContentPane().setBackground(BG);

        tableModel = new ContestantTableModel();
        table = new JTable(tableModel);
        table.setFont(TBL_FONT);
        table.setRowHeight(28);
        table.getTableHeader().setFont(HDR_FONT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setMaxWidth(30);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        table.getColumnModel().getColumn(6).setMaxWidth(40);
        table.getColumnModel().getColumn(7).setPreferredWidth(150);
        table.getColumnModel().getColumn(8).setPreferredWidth(90);
        table.getColumnModel().getColumn(9).setPreferredWidth(200);

        table.getColumnModel().getColumn(1).setCellRenderer(new ColorCellRenderer());

        javax.swing.table.TableRowSorter<ContestantTableModel> sorter = new javax.swing.table.TableRowSorter<>(tableModel);
        sorter.setComparator(3, (Object a, Object b) -> {
            double sa = parseScore(a);
            double sb = parseScore(b);
            return Double.compare(sa, sb);
        });
        sorter.setComparator(4, (Object a, Object b) -> {
            double sa = parseScore(a);
            double sb = parseScore(b);
            return Double.compare(sa, sb);
        });
        table.setRowSorter(sorter);
        sorter.setSortKeys(java.util.Arrays.asList(
            new javax.swing.RowSorter.SortKey(5, javax.swing.SortOrder.ASCENDING),
            new javax.swing.RowSorter.SortKey(3, javax.swing.SortOrder.DESCENDING)
        ));

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(920, 250));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttonBar.setOpaque(false);

        JButton btnAdd = makeBtn("+ Add Contestant");
        btnAdd.addActionListener(e -> addContestant());

        JButton btnDuplicate = makeBtn("Duplicate Selected");
        btnDuplicate.addActionListener(e -> duplicateSelected());

        JButton btnRemove = makeBtn("Remove Selected");
        btnRemove.setForeground(new Color(178, 34, 34));
        btnRemove.addActionListener(e -> removeSelected());

        btnQuickSetupRef = makeBtn("\u26A1 Quick Setup");
        btnQuickSetupRef.setBackground(new Color(33, 150, 243));
        btnQuickSetupRef.setForeground(Color.WHITE);
        btnQuickSetupRef.addActionListener(e -> showQuickSetup());

        btnStartAllRef = makeBtn("\u25B6 Start All");
        btnStartAllRef.setBackground(new Color(46, 139, 87));
        btnStartAllRef.setForeground(Color.WHITE);
        btnStartAllRef.addActionListener(e -> {
            artEvolver.startTournament();
            if (chkAutoEvolve.isSelected()) autoStartEvolving();
        });

        btnStopAllRef = makeBtn("\u25A0 Stop All");
        btnStopAllRef.setBackground(new Color(178, 34, 34));
        btnStopAllRef.setForeground(Color.WHITE);
        btnStopAllRef.addActionListener(e -> artEvolver.stopTournament());

        btnStartPrehistoric = makeBtn("\uD83E\uDDB4 Prehistoric Mode");
        btnStartPrehistoric.setBackground(new Color(121, 85, 72));
        btnStartPrehistoric.setForeground(Color.WHITE);
        btnStartPrehistoric.addActionListener(e -> togglePrehistoricMode());

        buttonBar.add(btnQuickSetupRef);
        buttonBar.add(btnStartPrehistoric);
        buttonBar.add(Box.createHorizontalStrut(8));
        buttonBar.add(btnAdd);
        buttonBar.add(btnDuplicate);
        buttonBar.add(btnRemove);
        buttonBar.add(Box.createHorizontalStrut(20));
        buttonBar.add(btnStartAllRef);
        buttonBar.add(btnStopAllRef);

        JPanel evoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        evoBar.setOpaque(false);

        chkAutoEvolve = new JCheckBox("Auto-Evolve on Start", true);
        chkAutoEvolve.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        chkAutoEvolve.setOpaque(false);
        chkAutoEvolve.setToolTipText("When checked, pressing Start All automatically begins the evolutionary culling cycle.");
        evoBar.add(chkAutoEvolve);
        evoBar.add(Box.createHorizontalStrut(6));

        btnEvolve = makeBtn("\u2B50 Start Evolving");
        btnEvolve.setBackground(new Color(156, 39, 176));
        btnEvolve.setForeground(Color.WHITE);
        btnEvolve.addActionListener(e -> toggleEvolving());
        evoBar.add(btnEvolve);

        JButton btnEvoSettings = makeBtn("Evo Settings...");
        btnEvoSettings.addActionListener(e -> showEvoSettings());
        evoBar.add(btnEvoSettings);

        evoBar.add(Box.createHorizontalStrut(10));
        lblGeneration = new JLabel("Gen: 0");
        lblGeneration.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        lblGeneration.setForeground(new Color(156, 39, 176));
        evoBar.add(lblGeneration);

        evoBar.add(Box.createHorizontalStrut(6));
        lblCountdown = new JLabel("");
        lblCountdown.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        lblCountdown.setForeground(new Color(255, 152, 0));
        evoBar.add(lblCountdown);

        evoBar.add(Box.createHorizontalStrut(10));
        lblBestEver = new JLabel("");
        lblBestEver.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lblBestEver.setForeground(new Color(76, 175, 80));
        evoBar.add(lblBestEver);

        // ═══ Prehistoric Mode panel ═══
        prehistoricPanel = buildPrehistoricPanel();
        prehistoricPanel.setVisible(false);

        JPanel allBars = new JPanel();
        allBars.setLayout(new BoxLayout(allBars, BoxLayout.Y_AXIS));
        allBars.setOpaque(false);
        allBars.add(buttonBar);
        allBars.add(evoBar);
        allBars.add(prehistoricPanel);

        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        detailPanel.setBackground(BG);

        JScrollPane detailScroll = new JScrollPane(detailPanel);
        detailScroll.setPreferredSize(new Dimension(400, 180));
        detailScroll.setBorder(BorderFactory.createTitledBorder("Contestant Parameters"));

        txtHistory = new JTextArea(8, 50);
        txtHistory.setEditable(false);
        txtHistory.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        txtHistory.setBackground(new Color(30, 30, 38));
        txtHistory.setForeground(new Color(200, 200, 210));
        JScrollPane historyScroll = new JScrollPane(txtHistory);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Evolution History"));

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailScroll, historyScroll);
        bottomSplit.setResizeWeight(0.4);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedDetail();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(allBars, BorderLayout.NORTH);
        topPanel.add(tableScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomSplit);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        // System status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(30, 30, 38));
        statusBar.setBorder(new EmptyBorder(3, 8, 3, 8));
        lblSysStatus = new JLabel(sysMonitor.formatCompact());
        lblSysStatus.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        lblSysStatus.setForeground(new Color(130, 200, 130));
        statusBar.add(lblSysStatus, BorderLayout.WEST);

        JPanel statusBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        statusBtnPanel.setOpaque(false);
        JButton btnClicker = makeBtn("\uD83C\uDFAE Clicker");
        btnClicker.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        btnClicker.addActionListener(e -> openClicker());
        statusBtnPanel.add(btnClicker);
        JButton btnDashboard = makeBtn("\uD83C\uDF10 Dashboard");
        btnDashboard.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        btnDashboard.addActionListener(e -> openDashboard());
        statusBtnPanel.add(btnDashboard);
        JButton btnAutopilot = makeBtn("\u2699 Autopilot...");
        btnAutopilot.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        btnAutopilot.addActionListener(e -> showAutopilotSettings());
        statusBtnPanel.add(btnAutopilot);
        statusBar.add(statusBtnPanel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        pack();
        sizeToScreen();
    }

    private void sizeToScreen() {
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(
            getGraphicsConfiguration());
        int usableW = screenSize.width - insets.left - insets.right;
        int usableH = screenSize.height - insets.top - insets.bottom;

        int w = Math.min(Math.max(getWidth(), 1000), usableW);
        int h = Math.min(Math.max(getHeight(), 650), (int) (usableH * 0.85));
        setSize(w, h);
        setLocationRelativeTo(null);
    }

    private JButton makeBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        btn.setFocusPainted(false);
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SYSTEM MONITOR + AUTOPILOT
    // ═══════════════════════════════════════════════════════════════

    /** Refreshes system resource display. Called from refreshEvolutionaryState(). */
    private void refreshSystemStatus() {
        sysMonitor.poll();
        String status = sysMonitor.formatCompact();
        if (autopilotActive) status = "\u2699 AUTOPILOT  " + status;
        lblSysStatus.setText(status);

        double cpu = sysMonitor.getCpuUsagePercent();
        if (cpu >= 0 && cpu > 90) {
            lblSysStatus.setForeground(new Color(244, 67, 54));
        } else if (cpu >= 0 && cpu > 70) {
            lblSysStatus.setForeground(new Color(255, 193, 7));
        } else {
            lblSysStatus.setForeground(new Color(130, 200, 130));
        }

        if (autopilotActive) {
            autopilotTick();
        }
    }

    private void showAutopilotSettings() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));

        form.add(new JLabel("Max CPU Usage (%):"));
        JSpinner spnCpu = new JSpinner(new SpinnerNumberModel(maxCpuPercent, 10.0, 100.0, 5.0));
        form.add(spnCpu);

        form.add(new JLabel("Max RAM Usage (%):"));
        JSpinner spnRam = new JSpinner(new SpinnerNumberModel(maxRamPercent, 20.0, 100.0, 5.0));
        form.add(spnRam);

        form.add(new JLabel("Max JVM Heap (%):"));
        JSpinner spnHeap = new JSpinner(new SpinnerNumberModel(maxHeapPercent, 20.0, 100.0, 5.0));
        form.add(spnHeap);

        form.add(new JLabel(""));
        JCheckBox chkEnable = new JCheckBox("Enable Autopilot", autopilotActive);
        chkEnable.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(chkEnable);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(8, 0, 0, 0));
        int totalThreadsUsed = contestants.stream()
                .filter(c -> !c.isFinished() && c.isRunning())
                .mapToInt(c -> c.getConfig() != null ? c.getConfig().threads : 1)
                .sum();
        int threadBudget = Math.max(2, (int) (MAX_TOTAL_THREADS * (maxCpuPercent / 100.0)));
        int aliveNow = (int) contestants.stream().filter(c -> !c.isFinished()).count();
        JTextArea txtInfo = new JTextArea(
                "Autopilot monitors system resources and automatically:\n"
                + " - Replaces expired/stale contestants via evo tournament breeding\n"
                + " - Thread budget: " + totalThreadsUsed + "/" + threadBudget
                + " (" + MAX_TOTAL_THREADS + " logical cores * " + (int)maxCpuPercent + "% limit)\n"
                + " - Max alive pop: " + maxAliveContestants + " (alive now: " + aliveNow + ")\n"
                + " - " + (SPAWN_COOLDOWN_MS / 1000) + "s cooldown between spawns\n"
                + " - Once evo tournament starts, only IT breeds replacements\n"
                + " - Activates evolutionary tournament when 3+ contestants have scores\n\n"
                + "Current system:\n" + sysMonitor.toString());
        txtInfo.setEditable(false);
        txtInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        txtInfo.setBackground(new Color(245, 245, 250));
        info.add(txtInfo);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        wrapper.add(info, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, wrapper,
                "Autopilot & Resource Limits", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            maxCpuPercent = ((Number) spnCpu.getValue()).doubleValue();
            maxRamPercent = ((Number) spnRam.getValue()).doubleValue();
            maxHeapPercent = ((Number) spnHeap.getValue()).doubleValue();
            boolean wasActive = autopilotActive;
            autopilotActive = chkEnable.isSelected();

            if (autopilotActive && !wasActive) {
                System.out.println("[Autopilot] Enabled — limits: CPU "
                        + (int) maxCpuPercent + "%, RAM " + (int) maxRamPercent
                        + "%, Heap " + (int) maxHeapPercent + "%");
                startAutopilot();
            } else if (!autopilotActive && wasActive) {
                System.out.println("[Autopilot] Disabled");
            }
        }
    }

    private void startAutopilot() {
        if (artEvolver.getResizedOriginal() == null) {
            JOptionPane.showMessageDialog(this,
                    "Load an image first before enabling Autopilot.",
                    "No Image", JOptionPane.WARNING_MESSAGE);
            autopilotActive = false;
            return;
        }

        // Thread budget: full core count scaled by CPU limit.
        int threadBudget = Math.max(2, (int) (MAX_TOTAL_THREADS * (maxCpuPercent / 100.0)));

        if (contestants.isEmpty() && (prehistoricMode == null || !prehistoricMode.isActive())) {
            prehistoricMode = new PrehistoricMode(artEvolver, contestants);
            prehistoricMode.setMaxThreadsBudget(threadBudget);
            prehistoricMode.setAutoAdvance(true);
            prehistoricMode.setEraDurationSeconds(10);
            if (prehistoricMode.start()) {
                prehistoricPanel.setVisible(true);
                btnStartPrehistoric.setText("\u25A0 Stop Prehistoric");
                btnStartPrehistoric.setBackground(new Color(178, 34, 34));
                chkAutoAdvance.setSelected(true);
                System.out.println("[Autopilot] Started Genesis Mode (budget: "
                        + threadBudget + "/" + MAX_TOTAL_THREADS + " threads, 60s eras)");
            }
        } else if (!contestants.isEmpty()) {
            boolean anyNotRunning = contestants.stream()
                    .anyMatch(c -> !c.isFinished() && !c.isRunning());
            if (anyNotRunning) {
                artEvolver.startTournament();
                if (chkAutoEvolve.isSelected()) autoStartEvolving();
            }
        }
        lastSpawnMs = System.currentTimeMillis();
    }

    /** Called every refresh cycle when autopilot is active. */
    private void autopilotTick() {
        if (!autopilotActive) return;

        long now = System.currentTimeMillis();
        int aliveCount = (int) contestants.stream().filter(c -> !c.isFinished()).count();
        int totalThreadsUsed = contestants.stream()
                .filter(c -> !c.isFinished() && c.isRunning())
                .mapToInt(c -> c.getConfig() != null ? c.getConfig().threads : 1)
                .sum();

        // Thread budget: use the full logical core count scaled by CPU limit.
        int threadBudget = Math.max(2, (int) (MAX_TOTAL_THREADS * (maxCpuPercent / 100.0)));
        boolean threadBudgetAvailable = totalThreadsUsed + 2 <= threadBudget;

        // Derive max alive from thread budget to prevent unbounded population growth
        int threadsPerContestantEst = 2;
        for (TournamentContestant cc : contestants) {
            if (!cc.isFinished() && cc.getConfig() != null) {
                threadsPerContestantEst = cc.getConfig().threads;
                break;
            }
        }
        maxAliveContestants = Math.max(3, threadBudget / Math.max(1, threadsPerContestantEst));

        boolean resourcesAvailable = sysMonitor.canAddWork(maxCpuPercent, maxRamPercent, maxHeapPercent);

        boolean cooldownPassed = (now - lastSpawnMs) > SPAWN_COOLDOWN_MS;

        boolean canSpawn = threadBudgetAvailable && resourcesAvailable && cooldownPassed;

        // If prehistoric mode is active, keep its budget in sync and add contestants
        if (prehistoricMode != null && prehistoricMode.isActive()) {
            prehistoricMode.setMaxThreadsBudget(threadBudget);
            if (canSpawn && !prehistoricMode.isAtFinalEra()) {
                String spawned = prehistoricMode.addPresetContestant();
                if (spawned != null) {
                    lastSpawnMs = now;
                    refreshTable();
                }
            }
            // Don't return — fall through so evo tournament can start during genesis
        }

        // Determine target threads per new contestant from existing contestants
        int threadsPerContestant = 2;
        for (TournamentContestant c : contestants) {
            if (!c.isFinished() && c.getConfig() != null) {
                threadsPerContestant = c.getConfig().threads;
                break;
            }
        }

        // Spawn replacement when threads are available (only if not in prehistoric mode)
        boolean inPrehistoric = prehistoricMode != null && prehistoricMode.isActive();
        int freeThreads = threadBudget - totalThreadsUsed;
        boolean needsReplacement = freeThreads >= threadsPerContestant;
        boolean underPopCap = aliveCount < maxAliveContestants;
        boolean evoRunning = evoTournament != null && evoTournament.isRunning();

        // Only autopilot-spawn raw contestants if the evo tournament isn't handling breeding.
        // Once the evo tournament is active, IT manages all culling/spawning via onCutoffTick.
        if (!inPrehistoric && !evoRunning && canSpawn && needsReplacement && underPopCap) {
            EvolutionConfig cfg = new EvolutionConfig();
            artEvolver.populateConfigFromUI(cfg);
            cfg.threads = threadsPerContestant;
            cfg.name = "Auto-" + nextId;
            TournamentContestant c = new TournamentContestant("a" + nextId++, cfg.name);
            c.setConfig(cfg);
            cfg.chartColor = c.getChartColor();
            contestants.add(c);
            refreshTable();
            artEvolver.refreshContestantCombo();

            if (artEvolver.getResizedOriginal() != null) {
                try {
                    c.createEvolvers(artEvolver.getPallete(),
                            artEvolver.getTriangleWidth(), artEvolver.getTriangleHeight(),
                            artEvolver.getWidthTriangles(), artEvolver.getHeightTriangles(),
                            artEvolver.getTriangleScaleHeight(), artEvolver.getJumpDistances());
                    c.initializeWithImage(artEvolver.getResizedOriginal());
                    c.start();
                } catch (Exception ex) {
                    System.err.println("[Autopilot] Failed to start " + cfg.name + ": " + ex);
                }
                if (!artEvolver.isRunning()) {
                    artEvolver.setTournamentMode(true);
                    artEvolver.startProcessTimer();
                }
            }
            lastSpawnMs = now;
            System.out.println("[Autopilot] Spawned " + cfg.name + " (" + threadsPerContestant
                    + "T, " + (totalThreadsUsed + threadsPerContestant) + "/" + threadBudget
                    + " budget, " + aliveCount + " alive)");
        }

        // Auto-start evolutionary tournament if enough running contestants
        if (aliveCount >= 3 && (evoTournament == null || !evoTournament.isRunning())) {
            boolean anyHaveScores = contestants.stream()
                    .anyMatch(c -> !c.isFinished() && c.getBestScore() > 0);
            if (anyHaveScores) {
                createEvoTournament();
                if (evoTournament != null && !evoTournament.isRunning()) {
                    evoTournament.setCutoffSeconds(evoTournament.getAdaptiveCutoffMin());
                    evoTournament.setAdaptiveCutoff(true);
                    lastHistoryRecordCount = 0;
                    evoTournament.start();
                    System.out.println("[Autopilot] Started evolutionary tournament"
                            + " (fast start at " + evoTournament.getCutoffSeconds()
                            + "s, " + aliveCount + " contestants)");
                }
            }
        }
    }

    public SystemMonitor getSystemMonitor() { return sysMonitor; }
    public boolean isAutopilotActive() { return autopilotActive; }

    private void openDashboard() {
        ensureDashboardServer();
        if (dashboardServer != null) {
            dashboardServer.openInBrowser();
        }
    }

    public DashboardServer getDashboardServer() { return dashboardServer; }

    private void openClicker() {
        ensureDashboardServer();
        if (dashboardServer != null) {
            try {
                java.awt.Desktop.getDesktop().browse(
                        java.net.URI.create(dashboardServer.getUrl() + "/clicker"));
            } catch (Exception ex) {
                System.err.println("[Clicker] Could not open browser: " + ex.getMessage());
            }
        }
    }

    private void ensureDashboardServer() {
        if (dashboardServer != null) return;
        try {
            dashboardServer = new DashboardServer(artEvolver, contestants, this);
            dashboardServer.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to start server:\n" + ex.getMessage(),
                    "Server Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PREHISTORIC MODE UI
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildPrehistoricPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(121, 85, 72)),
                new EmptyBorder(4, 8, 4, 8)));
        panel.setBackground(new Color(62, 44, 36));

        // Row 1: Era info + progress + advance
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row1.setOpaque(false);

        lblEra = new JLabel("ERA 0: Primordial Soup");
        lblEra.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblEra.setForeground(new Color(255, 183, 77));
        row1.add(lblEra);

        eraProgress = new JProgressBar(0, 100);
        eraProgress.setPreferredSize(new Dimension(120, 18));
        eraProgress.setStringPainted(true);
        eraProgress.setString("--");
        row1.add(eraProgress);

        lblEraCountdown = new JLabel("");
        lblEraCountdown.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        lblEraCountdown.setForeground(new Color(255, 152, 0));
        row1.add(lblEraCountdown);

        btnAdvanceEra = makeBtn("Advance Era \u00BB");
        btnAdvanceEra.setBackground(new Color(255, 152, 0));
        btnAdvanceEra.setForeground(Color.BLACK);
        btnAdvanceEra.addActionListener(e -> {
            if (prehistoricMode != null && prehistoricMode.isActive()) {
                prehistoricMode.advanceEra();
                refreshPrehistoricState();
            }
        });
        row1.add(btnAdvanceEra);

        chkAutoAdvance = new JCheckBox("Auto-Advance", false);
        chkAutoAdvance.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        chkAutoAdvance.setOpaque(false);
        chkAutoAdvance.setForeground(new Color(200, 200, 210));
        chkAutoAdvance.addActionListener(e -> {
            if (prehistoricMode != null) {
                prehistoricMode.setAutoAdvance(chkAutoAdvance.isSelected());
            }
        });
        row1.add(chkAutoAdvance);

        panel.add(row1);

        // Row 2: Action buttons + capabilities
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.setOpaque(false);

        btnAddThread = makeBtn("+1 Thread");
        btnAddThread.addActionListener(e -> {
            if (prehistoricMode != null && prehistoricMode.isActive()) {
                int threads = prehistoricMode.addThread();
                refreshPrehistoricState();
                JOptionPane.showMessageDialog(this,
                        "Threads per contestant: " + threads,
                        "+1 Thread", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        row2.add(btnAddThread);

        btnAddPreset = makeBtn("+ Add Preset Contestant");
        btnAddPreset.setBackground(new Color(33, 150, 243));
        btnAddPreset.setForeground(Color.WHITE);
        btnAddPreset.addActionListener(e -> {
            if (prehistoricMode != null && prehistoricMode.isActive()) {
                String name = prehistoricMode.addPresetContestant();
                if (name != null) {
                    refreshTable();
                    refreshPrehistoricState();
                }
            }
        });
        row2.add(btnAddPreset);

        btnAddEvolved = makeBtn("+ Add Evolved Contestant");
        btnAddEvolved.setBackground(new Color(156, 39, 176));
        btnAddEvolved.setForeground(Color.WHITE);
        btnAddEvolved.addActionListener(e -> {
            if (prehistoricMode != null && prehistoricMode.isActive()) {
                String name = prehistoricMode.addEvolvedContestant();
                if (name != null) {
                    refreshTable();
                    refreshPrehistoricState();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Need at least 2 contestants with scores to breed.",
                            "Cannot Breed", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        row2.add(btnAddEvolved);

        row2.add(Box.createHorizontalStrut(8));

        lblCapabilities = new JLabel("");
        lblCapabilities.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblCapabilities.setForeground(new Color(178, 223, 138));
        row2.add(lblCapabilities);

        panel.add(row2);

        // Row 3: Era description
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row3.setOpaque(false);

        lblEraDesc = new JLabel("");
        lblEraDesc.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        lblEraDesc.setForeground(new Color(180, 180, 190));
        row3.add(lblEraDesc);

        panel.add(row3);

        return panel;
    }

    private void togglePrehistoricMode() {
        if (prehistoricMode != null && prehistoricMode.isActive()) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Stop Prehistoric Mode? All contestants will be preserved.",
                    "Stop Prehistoric Mode", JOptionPane.YES_NO_OPTION);
            if (opt != JOptionPane.YES_OPTION) return;
            prehistoricMode.stop();
            prehistoricPanel.setVisible(false);
            btnStartPrehistoric.setText("\uD83E\uDDB4 Prehistoric Mode");
            btnStartPrehistoric.setBackground(new Color(121, 85, 72));
            setEvoLockButtons(false);
            return;
        }

        if (artEvolver.getResizedOriginal() == null) {
            JOptionPane.showMessageDialog(this,
                    "Load an image first before starting Prehistoric Mode.",
                    "No Image", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm clearing existing contestants
        if (!contestants.isEmpty()) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Prehistoric Mode clears existing contestants and starts from scratch.\n\nContinue?",
                    "Start Prehistoric Mode", JOptionPane.OK_CANCEL_OPTION);
            if (opt != JOptionPane.OK_OPTION) return;
        }

        // Show era duration config
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));

        form.add(new JLabel("Era Duration (seconds):"));
        JSpinner spnDuration = new JSpinner(new SpinnerNumberModel(60, 10, 600, 10));
        form.add(spnDuration);

        form.add(new JLabel("Max Evolver Threads:"));
        int avail = Runtime.getRuntime().availableProcessors();
        int defaultBudget = Math.max(2, avail / 2);
        JSpinner spnThreads = new JSpinner(new SpinnerNumberModel(
                defaultBudget, 2, avail, 1));
        spnThreads.setToolTipText("Max evolver threads (" + avail + " logical cores). "
                + "Each evolver thread uses 100% of a core. Default = half of cores.");
        form.add(spnThreads);

        form.add(new JLabel("Auto-Advance:"));
        JCheckBox chkAuto = new JCheckBox("Automatically progress through eras", false);
        form.add(chkAuto);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Prehistoric Mode Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        prehistoricMode = new PrehistoricMode(artEvolver, contestants);
        prehistoricMode.setEraDurationSeconds((int) spnDuration.getValue());
        prehistoricMode.setMaxThreadsBudget((int) spnThreads.getValue());
        prehistoricMode.setAutoAdvance(chkAuto.isSelected());
        chkAutoAdvance.setSelected(chkAuto.isSelected());

        if (!prehistoricMode.start()) {
            JOptionPane.showMessageDialog(this,
                    "Failed to start Prehistoric Mode. Make sure an image is loaded.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        prehistoricPanel.setVisible(true);
        btnStartPrehistoric.setText("\u25A0 Stop Prehistoric");
        btnStartPrehistoric.setBackground(new Color(178, 34, 34));

        refreshPrehistoricState();
        refreshTable();
    }

    /** Updates the Prehistoric Mode UI panel with current era state. */
    public void refreshPrehistoricState() {
        if (prehistoricMode == null || !prehistoricMode.isActive()) return;

        int era = prehistoricMode.getCurrentEra();
        lblEra.setText("ERA " + era + ": " + prehistoricMode.getCurrentEraName());
        lblEraDesc.setText(prehistoricMode.getCurrentEraDescription());

        String[] caps = prehistoricMode.getCurrentCapabilities();
        StringBuilder capStr = new StringBuilder();
        for (int i = 0; i < caps.length; i++) {
            if (i > 0) capStr.append(" | ");
            capStr.append(caps[i]);
        }
        lblCapabilities.setText(capStr.toString());

        boolean atFinal = prehistoricMode.isAtFinalEra();
        btnAdvanceEra.setEnabled(!atFinal);
        btnAddPreset.setEnabled(true);
        btnAddEvolved.setEnabled(prehistoricMode.arePresetsExhausted()
                || contestants.stream().filter(c -> !c.isFinished() && c.getBestScore() > 0).count() >= 2);

        if (prehistoricMode.arePresetsExhausted()) {
            btnAddPreset.setText("+ Add Preset (recycled)");
        } else {
            btnAddPreset.setText("+ Add Preset (" + (PrehistoricMode.PRESET_STRATEGIES.length - prehistoricMode.getNextPresetIndex()) + " left)");
        }

        int secondsLeft = prehistoricMode.getSecondsUntilNextEra();
        if (secondsLeft >= 0) {
            int pct = (int) (100.0 * (1.0 - (double) secondsLeft / prehistoricMode.getEraDurationSeconds()));
            eraProgress.setValue(pct);
            eraProgress.setString(secondsLeft + "s");
            lblEraCountdown.setText("Next era: " + secondsLeft + "s");
        } else {
            eraProgress.setValue(atFinal ? 100 : 0);
            eraProgress.setString(atFinal ? "FINAL ERA" : "Manual");
            lblEraCountdown.setText(atFinal ? "Final Era Reached" : "");
        }

        // Update era history in the main history area
        java.util.List<PrehistoricMode.EraRecord> eraRecords = prehistoricMode.getHistory();
        if (eraRecords.size() > lastHistoryRecordCount) {
            lastHistoryRecordCount = eraRecords.size();
            StringBuilder sb = new StringBuilder();
            for (PrehistoricMode.EraRecord rec : eraRecords) {
                sb.append(rec.toNarrative()).append('\n');
            }
            txtHistory.setText(sb.toString());
            txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
        }
    }

    public PrehistoricMode getPrehistoricMode() { return prehistoricMode; }

    // --- Strategy presets for Quick Setup ---

    private static final String[] STRATEGY_NAMES = {
        "Balanced",
        "Aggressive Explorer",
        "Grid Refiner",
        "Targeted Precision",
        "Heavy Random",
        "Close Mutation Focus",
        "Fast Convergence",
        "Wide Search",
        "Micro Surgeon",
        "Chaos Engine",
        "Gradient Chaser",
        "Population Boom",
        "Sniper",
        "Blitz",
        "Deep Grid",
        "Hybrid Adaptive",
    };

    public int getStrategyCount() { return STRATEGY_NAMES.length; }
    public String getStrategyName(int index) { return STRATEGY_NAMES[index % STRATEGY_NAMES.length]; }
    public EvolutionConfig applyStrategyPublic(EvolutionConfig base, int index) {
        return applyStrategy(base, index);
    }

    private EvolutionConfig applyStrategy(EvolutionConfig base, int strategyIndex) {
        EvolutionConfig cfg = base.clone();
        switch (strategyIndex % STRATEGY_NAMES.length) {
            case 0: // Balanced — defaults from UI
                cfg.name = STRATEGY_NAMES[0];
                break;
            case 1: // Aggressive Explorer — high random, low grid
                cfg.name = STRATEGY_NAMES[1];
                cfg.gridMutationChances = Math.max(4, cfg.gridMutationChances / 4);
                cfg.randomMutationChances = cfg.randomMutationChances * 4;
                cfg.randomMutationPercent = Math.min(1f, cfg.randomMutationPercent * 4);
                cfg.targetedSwapAttempts = Math.max(2, cfg.targetedSwapAttempts / 2);
                cfg.closeMutationChances = Math.max(2, cfg.closeMutationChances / 2);
                break;
            case 2: // Grid Refiner — heavy grid, low random
                cfg.name = STRATEGY_NAMES[2];
                cfg.gridMutationChances = cfg.gridMutationChances * 4;
                cfg.gridMutationDecay = cfg.gridMutationDecay * 0.5f;
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 4);
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 2;
                break;
            case 3: // Targeted Precision — max targeted swaps
                cfg.name = STRATEGY_NAMES[3];
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 4;
                cfg.gridMutationChances = Math.max(4, cfg.gridMutationChances / 2);
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 2);
                cfg.closeMutationChances = cfg.closeMutationChances * 2;
                break;
            case 4: // Heavy Random — brute-force diversity
                cfg.name = STRATEGY_NAMES[4];
                cfg.randomMutationChances = cfg.randomMutationChances * 8;
                cfg.randomMutationPercent = Math.min(1f, cfg.randomMutationPercent * 2);
                cfg.gridMutationChances = 0;
                cfg.targetedSwapAttempts = Math.max(2, cfg.targetedSwapAttempts / 4);
                break;
            case 5: // Close Mutation Focus — fine local tuning
                cfg.name = STRATEGY_NAMES[5];
                cfg.closeMutationChances = cfg.closeMutationChances * 6;
                cfg.closeMutationPercent = Math.min(1f, cfg.closeMutationPercent * 4);
                cfg.gridMutationChances = Math.max(4, cfg.gridMutationChances / 2);
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 4);
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 2;
                break;
            case 6: // Fast Convergence — all operators high, small population
                cfg.name = STRATEGY_NAMES[6];
                cfg.gridMutationChances = cfg.gridMutationChances * 2;
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 3;
                cfg.closeMutationChances = cfg.closeMutationChances * 2;
                cfg.population = Math.max(1, cfg.population);
                break;
            case 7: // Wide Search — big population, moderate mutations
                cfg.name = STRATEGY_NAMES[7];
                cfg.population = Math.max(cfg.population, 4);
                cfg.gridMutationChances = cfg.gridMutationChances * 2;
                cfg.randomMutationChances = cfg.randomMutationChances * 2;
                break;
            case 8: // Micro Surgeon — extreme close mutation, tiny percent, very local
                cfg.name = STRATEGY_NAMES[8];
                cfg.closeMutationChances = cfg.closeMutationChances * 10;
                cfg.closeMutationPercent = Math.min(0.01f, cfg.closeMutationPercent * 8);
                cfg.gridMutationChances = Math.max(2, cfg.gridMutationChances / 4);
                cfg.randomMutationChances = Math.max(5, cfg.randomMutationChances / 8);
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 3;
                break;
            case 9: // Chaos Engine — max everything, brute-force diversity bomb
                cfg.name = STRATEGY_NAMES[9];
                cfg.gridMutationChances = cfg.gridMutationChances * 4;
                cfg.randomMutationChances = cfg.randomMutationChances * 6;
                cfg.randomMutationPercent = Math.min(1f, cfg.randomMutationPercent * 6);
                cfg.closeMutationChances = cfg.closeMutationChances * 4;
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 4;
                break;
            case 10: // Gradient Chaser — targeted + grid, minimal random noise
                cfg.name = STRATEGY_NAMES[10];
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 6;
                cfg.gridMutationChances = cfg.gridMutationChances * 3;
                cfg.gridMutationDecay = cfg.gridMutationDecay * 0.3f;
                cfg.randomMutationChances = Math.max(5, cfg.randomMutationChances / 6);
                cfg.closeMutationChances = cfg.closeMutationChances * 3;
                break;
            case 11: // Population Boom — large population with crossover focus
                cfg.name = STRATEGY_NAMES[11];
                cfg.population = Math.max(6, cfg.population * 3);
                cfg.crossoverMax = Math.max(4, cfg.crossoverMax * 2);
                cfg.blockCrossoverEnabled = true;
                cfg.gridMutationChances = cfg.gridMutationChances * 2;
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 2;
                break;
            case 12: // Sniper — very high targeted, minimal noise
                cfg.name = STRATEGY_NAMES[12];
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 8;
                cfg.gridMutationChances = 0;
                cfg.randomMutationChances = Math.max(5, cfg.randomMutationChances / 10);
                cfg.closeMutationChances = Math.max(2, cfg.closeMutationChances / 2);
                break;
            case 13: // Blitz — fast iterations, low mutation per step, high throughput
                cfg.name = STRATEGY_NAMES[13];
                cfg.randomMutationChances = cfg.randomMutationChances * 3;
                cfg.randomMutationPercent = Math.max(0.0001f, cfg.randomMutationPercent / 4);
                cfg.gridMutationChances = cfg.gridMutationChances * 2;
                cfg.gridMutationPercent = Math.max(0.01f, cfg.gridMutationPercent / 2);
                cfg.closeMutationChances = cfg.closeMutationChances * 2;
                cfg.closeMutationPercent = Math.max(0.00001f, cfg.closeMutationPercent / 2);
                break;
            case 14: // Deep Grid — extreme grid with slow decay
                cfg.name = STRATEGY_NAMES[14];
                cfg.gridMutationChances = cfg.gridMutationChances * 8;
                cfg.gridMutationDecay = cfg.gridMutationDecay * 0.2f;
                cfg.gridMutationPercent = Math.min(1f, cfg.gridMutationPercent * 2);
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 2);
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts;
                break;
            case 15: // Hybrid Adaptive — balanced high all with moderate population
                cfg.name = STRATEGY_NAMES[15];
                cfg.gridMutationChances = cfg.gridMutationChances * 3;
                cfg.randomMutationChances = cfg.randomMutationChances * 3;
                cfg.closeMutationChances = cfg.closeMutationChances * 3;
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 3;
                cfg.population = Math.max(3, cfg.population * 2);
                cfg.crossoverMax = Math.max(3, cfg.crossoverMax * 2);
                break;
        }
        return cfg;
    }

    private void showQuickSetup() {
        boolean hasRunning = contestants.stream().anyMatch(TournamentContestant::isRunning);
        if (hasRunning) {
            JOptionPane.showMessageDialog(this, "Stop all contestants before Quick Setup.",
                    "Cannot Setup", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int availableCores = Runtime.getRuntime().availableProcessors();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        form.add(new JLabel("Total Evolver Threads:"));
        int defaultCores = Math.max(2, availableCores / 2);
        JSpinner spnCores = new JSpinner(new SpinnerNumberModel(
                defaultCores, 2, availableCores, 1));
        spnCores.setToolTipText("Each evolver thread uses 100% of a core. "
                + "Default = half of " + availableCores + " logical cores.");
        form.add(spnCores);

        form.add(new JLabel("Threads per Contestant:"));
        JSpinner spnThreadsEach = new JSpinner(new SpinnerNumberModel(
                Math.max(1, Math.min(4, availableCores / 4)), 1, availableCores, 1));
        form.add(spnThreadsEach);

        JLabel lblContestants = new JLabel();
        Runnable updateCount = () -> {
            int cores = (int) spnCores.getValue();
            int threadsEach = (int) spnThreadsEach.getValue();
            int count = Math.max(1, cores / threadsEach);
            int capped = Math.min(count, STRATEGY_NAMES.length);
            lblContestants.setText(capped + " contestants (" + (capped * threadsEach) + " total threads)");
        };
        updateCount.run();
        spnCores.addChangeListener(e -> updateCount.run());
        spnThreadsEach.addChangeListener(e -> updateCount.run());

        form.add(new JLabel("Auto-calculated:"));
        lblContestants.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lblContestants.setForeground(new Color(33, 150, 243));
        form.add(lblContestants);

        form.add(new JLabel(""));
        JCheckBox chkClearExisting = new JCheckBox("Clear existing contestants", true);
        form.add(chkClearExisting);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(8, 0, 0, 0));
        JLabel lblInfo = new JLabel("<html><b>Strategies generated:</b></html>");
        lblInfo.setFont(SECTION_FONT);
        info.add(lblInfo);
        for (String name : STRATEGY_NAMES) {
            JLabel lbl = new JLabel("  \u2022 " + name);
            lbl.setFont(TBL_FONT);
            info.add(lbl);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        wrapper.add(info, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, wrapper,
                "Quick Tournament Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        int totalCores = (int) spnCores.getValue();
        int threadsEach = (int) spnThreadsEach.getValue();
        int numContestants = Math.min(Math.max(1, totalCores / threadsEach), STRATEGY_NAMES.length);

        if (chkClearExisting.isSelected()) {
            contestants.clear();
            nextId = 1;
        }

        EvolutionConfig baseConfig = EvolutionConfig.fromCurrentSettings();
        artEvolver.populateConfigFromUI(baseConfig);
        baseConfig.threads = threadsEach;

        for (int i = 0; i < numContestants; i++) {
            EvolutionConfig stratCfg = applyStrategy(baseConfig, i);
            stratCfg.threads = threadsEach;

            TournamentContestant c = new TournamentContestant("c" + nextId++, stratCfg.name);
            c.setConfig(stratCfg);
            stratCfg.chartColor = c.getChartColor();
            contestants.add(c);
        }

        notifyContestantsChanged();
        if (!contestants.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    // --- Evolutionary Tournament ---

    private void toggleEvolving() {
        createEvoTournament();

        if (evoTournament.isRunning()) {
            evoTournament.stop();
            btnEvolve.setText("\u2B50 Start Evolving");
            btnEvolve.setBackground(new Color(156, 39, 176));
            lblCountdown.setText("");
            setEvoLockButtons(false);
        } else {
            if (contestants.size() < evoTournament.getMinContestants()) {
                JOptionPane.showMessageDialog(this,
                        "Need at least " + evoTournament.getMinContestants()
                                + " contestants. Use Quick Setup first.",
                        "Not Enough Contestants", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean allRunning = contestants.stream().allMatch(TournamentContestant::isRunning);
            if (!allRunning) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Not all contestants are running. Start All first?",
                        "Start Tournament", JOptionPane.YES_NO_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    artEvolver.startTournament();
                } else if (opt == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }

            lastHistoryRecordCount = 0;
            boolean started = evoTournament.start();
            if (!started) {
                JOptionPane.showMessageDialog(this,
                        "Failed to start evolution. Check that you have at least "
                                + evoTournament.getMinContestants() + " contestants and they are running.",
                        "Evolution Not Started", JOptionPane.ERROR_MESSAGE);
                return;
            }
            btnEvolve.setText("\u25A0 Stop Evolving");
            btnEvolve.setBackground(new Color(178, 34, 34));
            setEvoLockButtons(true);
        }
    }

    /** Silently starts evolving if preconditions are met (called by auto-evolve). */
    private void autoStartEvolving() {
        createEvoTournament();
        if (evoTournament.isRunning()) return;

        long alive = contestants.stream().filter(c -> !c.isFinished()).count();
        if (alive < evoTournament.getMinContestants()) return;

        if (evoTournament.start()) {
            btnEvolve.setText("\u25A0 Stop Evolving");
            btnEvolve.setBackground(new Color(178, 34, 34));
            setEvoLockButtons(true);
            System.out.println("[Tournament] Auto-evolve started");
        }
    }

    private void setEvoLockButtons(boolean evolving) {
        btnQuickSetupRef.setEnabled(!evolving);
    }

    /** Resets the evolving button to its idle state (called by ArtEvolver.stopTournament). */
    public void resetEvolveButton() {
        if (evoTournament != null && evoTournament.isRunning()) {
            evoTournament.stop();
        }
        btnEvolve.setText("\u2B50 Start Evolving");
        btnEvolve.setBackground(new Color(156, 39, 176));
        lblCountdown.setText("");
        setEvoLockButtons(false);
    }

    private void showEvoSettings() {
        if (evoTournament == null) {
            evoTournament = new EvolutionaryTournament(artEvolver, contestants);
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 4));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Timing & Population ---
        addSectionLabel(form, "TIMING & POPULATION");

        form.add(new JLabel("Cutoff Interval (seconds):"));
        JSpinner spnCutoff = new JSpinner(new SpinnerNumberModel(
                evoTournament.getCutoffSeconds(), 5, 600, 5));
        spnCutoff.setToolTipText("Time between tournament cycles. Start low (5-10s) for fast churn.");
        form.add(spnCutoff);

        form.add(new JLabel("Grace Period (ticks):"));
        JSpinner spnGrace = new JSpinner(new SpinnerNumberModel(
                evoTournament.getGracePeriodTicks(), 0, 10, 1));
        form.add(spnGrace);

        form.add(new JLabel("Min Contestants (floor):"));
        JSpinner spnMinPop = new JSpinner(new SpinnerNumberModel(
                evoTournament.getMinContestants(), 2, 20, 1));
        form.add(spnMinPop);

        form.add(new JLabel("Spawns per Tick:"));
        JSpinner spnSpawns = new JSpinner(new SpinnerNumberModel(
                evoTournament.getSpawnsPerTick(), 1, 10, 1));
        form.add(spnSpawns);

        // --- Adaptive Cutoff ---
        addSectionLabel(form, "ADAPTIVE CUTOFF");

        form.add(new JLabel("Adaptive Cutoff:"));
        JCheckBox chkAdaptive = new JCheckBox("Auto-adjust interval", evoTournament.isAdaptiveCutoff());
        form.add(chkAdaptive);

        form.add(new JLabel("Adaptive Min (seconds):"));
        JSpinner spnAdaptMin = new JSpinner(new SpinnerNumberModel(
                evoTournament.getAdaptiveCutoffMin(), 5, 120, 5));
        form.add(spnAdaptMin);

        form.add(new JLabel("Adaptive Max (seconds):"));
        JSpinner spnAdaptMax = new JSpinner(new SpinnerNumberModel(
                evoTournament.getAdaptiveCutoffMax(), 30, 600, 10));
        form.add(spnAdaptMax);

        // --- Lifespan Cap ---
        addSectionLabel(form, "CONTESTANT LIFESPAN");

        form.add(new JLabel("Max Lifespan (seconds, 0=off):"));
        JSpinner spnLifespan = new JSpinner(new SpinnerNumberModel(
                evoTournament.getMaxLifespanSeconds(), 0, 3600, 5));
        spnLifespan.setToolTipText("Promote and replace after N seconds. Default 60s for fast iteration.");
        form.add(spnLifespan);

        form.add(new JLabel("Stale Detection (seconds, 0=off):"));
        JSpinner spnStale = new JSpinner(new SpinnerNumberModel(
                evoTournament.getStaleThresholdSeconds(), 0, 120, 5));
        spnStale.setToolTipText("Eliminate flat-line contestants after N seconds of zero improvement.");
        form.add(spnStale);

        form.add(new JLabel("Max Promoted (hall of fame):"));
        JSpinner spnMaxPromoted = new JSpinner(new SpinnerNumberModel(
                evoTournament.getMaxPromoted(), 1, 50, 1));
        spnMaxPromoted.setToolTipText("Max promoted contestants kept for breeding. Oldest/worst rotate out.");
        form.add(spnMaxPromoted);

        form.add(new JLabel("Preset Injection Interval:"));
        JSpinner spnPresetInject = new JSpinner(new SpinnerNumberModel(
                evoTournament.getPresetInjectionInterval(), 0, 20, 1));
        spnPresetInject.setToolTipText("Every Nth spawn, inject an untried preset strategy (0=off).");
        form.add(spnPresetInject);

        // --- Ranking Strategy ---
        addSectionLabel(form, "RANKING STRATEGY");

        form.add(new JLabel("Strategy:"));
        JComboBox<EvolutionaryTournament.RankingStrategy> cmbStrategy = new JComboBox<>(
                EvolutionaryTournament.RankingStrategy.values());
        cmbStrategy.setSelectedItem(evoTournament.getRankingStrategy());
        cmbStrategy.setToolTipText(
                "BALANCED: use base weights. VELOCITY_FIRST: favor fast learners. "
                + "FITNESS_FIRST: favor peak score. AUTO: start velocity-heavy, shift to fitness.");
        form.add(cmbStrategy);

        form.add(new JLabel("AUTO Transition Gen:"));
        JSpinner spnAutoTransGen = new JSpinner(new SpinnerNumberModel(
                evoTournament.getAutoTransitionGen(), 1, 100, 1));
        spnAutoTransGen.setToolTipText("In AUTO mode, how many generations to fully transition from velocity to fitness.");
        form.add(spnAutoTransGen);

        // --- Breeding ---
        addSectionLabel(form, "BREEDING & MUTATION");

        form.add(new JLabel("Mutation Rate (0.0-1.0):"));
        JSpinner spnMutRate = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getMutationRate(), 0.0, 1.0, 0.05));
        form.add(spnMutRate);

        form.add(new JLabel("Mutation Strength (0.0-1.0):"));
        JSpinner spnMutStr = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getMutationStrength(), 0.0, 1.0, 0.05));
        form.add(spnMutStr);

        form.add(new JLabel("Ancestral Crossover:"));
        JCheckBox chkAncestral = new JCheckBox("Blend grandparent genes", evoTournament.isUseAncestralCrossover());
        form.add(chkAncestral);

        form.add(new JLabel("Ancestry Depth:"));
        JSpinner spnAncDepth = new JSpinner(new SpinnerNumberModel(
                evoTournament.getAncestryDepth(), 1, 10, 1));
        form.add(spnAncDepth);

        // --- Composite Ranking Weights ---
        addSectionLabel(form, "BASE WEIGHTS (used by BALANCED, overridden by other strategies)");

        form.add(new JLabel("Fitness Weight:"));
        JSpinner spnWFit = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getFitnessWeight(), 0.0, 1.0, 0.05));
        form.add(spnWFit);

        form.add(new JLabel("Velocity Weight:"));
        JSpinner spnWVel = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getVelocityWeight(), 0.0, 1.0, 0.05));
        form.add(spnWVel);

        form.add(new JLabel("Acceleration Weight:"));
        JSpinner spnWAcc = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getAccelerationWeight(), 0.0, 1.0, 0.05));
        form.add(spnWAcc);

        form.add(new JLabel("Lineage Weight:"));
        JSpinner spnWLin = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getLineageWeight(), 0.0, 1.0, 0.05));
        form.add(spnWLin);

        // --- Lineage ---
        addSectionLabel(form, "LINEAGE & VELOCITY");

        form.add(new JLabel("Lineage Decay (per gen):"));
        JSpinner spnLinDecay = new JSpinner(new SpinnerNumberModel(
                evoTournament.getLineageDecay(), 0.0, 1.0, 0.05));
        form.add(spnLinDecay);

        form.add(new JLabel("Velocity Window (seconds):"));
        JSpinner spnVelWin = new JSpinner(new SpinnerNumberModel(
                evoTournament.getVelocityWindowSeconds(), 5, 300, 5));
        form.add(spnVelWin);

        JLabel hint = new JLabel("<html><i>Settings can be changed while evolving is running.<br>"
                + "Grace period protects newcomers from immediate culling.<br>"
                + "Lifespan cap promotes contestants to Hall of Fame (breeding pool).<br>"
                + "Preset injection introduces untried strategies into the gene pool.<br>"
                + "AUTO ranking starts velocity-heavy and shifts to fitness.</i></html>");
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        hint.setForeground(Color.GRAY);
        form.add(hint);
        form.add(new JLabel(""));

        JScrollPane scroll = new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(500, 660));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        int result = JOptionPane.showConfirmDialog(this, scroll,
                "Evolutionary Tournament Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            evoTournament.setCutoffSeconds((int) spnCutoff.getValue());
            evoTournament.setGracePeriodTicks((int) spnGrace.getValue());
            evoTournament.setMinContestants((int) spnMinPop.getValue());
            evoTournament.setSpawnsPerTick((int) spnSpawns.getValue());
            evoTournament.setAdaptiveCutoff(chkAdaptive.isSelected());
            evoTournament.setAdaptiveCutoffMin((int) spnAdaptMin.getValue());
            evoTournament.setAdaptiveCutoffMax((int) spnAdaptMax.getValue());
            evoTournament.setMaxLifespanSeconds((int) spnLifespan.getValue());
            evoTournament.setStaleThresholdSeconds((int) spnStale.getValue());
            evoTournament.setMaxPromoted((int) spnMaxPromoted.getValue());
            evoTournament.setPresetInjectionInterval((int) spnPresetInject.getValue());
            evoTournament.setRankingStrategy(
                    (EvolutionaryTournament.RankingStrategy) cmbStrategy.getSelectedItem());
            evoTournament.setAutoTransitionGen((int) spnAutoTransGen.getValue());
            evoTournament.setMutationRate(((Number) spnMutRate.getValue()).floatValue());
            evoTournament.setMutationStrength(((Number) spnMutStr.getValue()).floatValue());
            evoTournament.setUseAncestralCrossover(chkAncestral.isSelected());
            evoTournament.setAncestryDepth((int) spnAncDepth.getValue());
            evoTournament.setFitnessWeight(((Number) spnWFit.getValue()).floatValue());
            evoTournament.setVelocityWeight(((Number) spnWVel.getValue()).floatValue());
            evoTournament.setAccelerationWeight(((Number) spnWAcc.getValue()).floatValue());
            evoTournament.setLineageWeight(((Number) spnWLin.getValue()).floatValue());
            evoTournament.setLineageDecay(((Number) spnLinDecay.getValue()).doubleValue());
            evoTournament.setVelocityWindowSeconds((int) spnVelWin.getValue());
        }
    }

    private void addSectionLabel(JPanel form, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lbl.setForeground(new Color(100, 100, 120));
        lbl.setBorder(new EmptyBorder(8, 0, 2, 0));
        form.add(lbl);
        form.add(new JLabel(""));
    }

    /** Called periodically to update generation, countdown, best-ever, history. */
    public void refreshEvolutionaryState() {
        refreshSystemStatus();
        if (prehistoricMode != null && prehistoricMode.isActive()) {
            refreshPrehistoricState();
        }
        if (evoTournament == null) return;

        lblGeneration.setText("Gen: " + evoTournament.getGeneration());

        int remaining = evoTournament.getSecondsUntilNextTick();
        if (remaining >= 0) {
            int m = remaining / 60;
            int s = remaining % 60;
            String countText = (m > 0) ? String.format("Next: %d:%02d", m, s) : String.format("Next: %ds", s);
            if (evoTournament.isAdaptiveCutoff()) {
                countText += " [adaptive " + evoTournament.getCutoffSeconds() + "s]";
            }
            if (evoTournament.getSpawnsPerTick() > 1) {
                countText += " x" + evoTournament.getSpawnsPerTick();
            }
            lblCountdown.setText(countText);
            if (remaining <= 10) {
                lblCountdown.setForeground(new Color(244, 67, 54));
            } else if (remaining <= 30) {
                lblCountdown.setForeground(new Color(255, 152, 0));
            } else {
                lblCountdown.setForeground(new Color(100, 181, 246));
            }
        } else {
            lblCountdown.setText("");
        }

        if (evoTournament.getBestEverScore() > 0) {
            DecimalFormat df = new DecimalFormat("0.0000");
            String bestText = "Best Ever: " + evoTournament.getBestEverName()
                    + " " + df.format(evoTournament.getBestEverScore() * 100) + "%";
            if (evoTournament.isConverged()) {
                bestText += "  [CONVERGED]";
                lblBestEver.setForeground(new Color(255, 193, 7));
            } else {
                lblBestEver.setForeground(new Color(76, 175, 80));
            }
            long protectedCount = contestants.stream().filter(c -> !c.isFinished() && c.isProtected()).count();
            if (protectedCount > 0) {
                bestText += "  \u2B50" + protectedCount + " protected";
            }
            lblBestEver.setText(bestText);
        }

        List<EvolutionaryTournament.GenerationRecord> records = evoTournament.getHistory();
        if (records.size() > lastHistoryRecordCount) {
            lastHistoryRecordCount = records.size();
            StringBuilder sb = new StringBuilder();
            sb.append(buildLiveStatusBlock());
            sb.append('\n');
            for (EvolutionaryTournament.GenerationRecord rec : records) {
                sb.append(rec.toNarrative()).append('\n');
            }
            txtHistory.setText(sb.toString());
            txtHistory.setCaretPosition(0);
        } else if (evoTournament.isRunning()) {
            // Between generations: refresh the live status block at the top
            String currentText = txtHistory.getText();
            String liveBlock = buildLiveStatusBlock();
            int dividerIdx = currentText.indexOf("\n--- ");
            if (dividerIdx > 0) {
                txtHistory.setText(liveBlock + "\n" + currentText.substring(dividerIdx));
            } else {
                txtHistory.setText(liveBlock);
            }
            txtHistory.setCaretPosition(0);
        }
    }

    /**
     * Builds a live status block shown at the top of the history panel.
     * Acts as a "smart announcer" with real-time contestant analysis.
     */
    private String buildLiveStatusBlock() {
        StringBuilder sb = new StringBuilder();
        java.text.DecimalFormat df2 = new java.text.DecimalFormat("0.00");
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("0.0000");

        List<TournamentContestant> alive = contestants.stream()
                .filter(c -> !c.isFinished()).collect(java.util.stream.Collectors.toList());
        long promotedCount = contestants.stream().filter(c -> c.isPromoted() && !c.isEliminated()).count();
        long eliminatedCount = contestants.stream().filter(TournamentContestant::isEliminated).count();

        sb.append("═══ LIVE TOURNAMENT STATUS ═══\n");

        if (alive.isEmpty()) {
            sb.append("  No active contestants.\n");
            return sb.toString();
        }

        alive.sort((a, b) -> Double.compare(b.getBestScore(), a.getBestScore()));

        TournamentContestant leader = alive.get(0);
        TournamentContestant trailer = alive.get(alive.size() - 1);

        sb.append("  Active: ").append(alive.size());
        if (promotedCount > 0) sb.append("  |  \uD83C\uDFC5 Promoted: ").append(promotedCount);
        if (eliminatedCount > 0) sb.append("  |  Eliminated: ").append(eliminatedCount);
        sb.append("  |  Gen: ").append(evoTournament != null ? evoTournament.getGeneration() : 0)
          .append('\n');

        // Leader info
        double leaderVel = leader.getFitnessTracker().getVelocity();
        sb.append("  \uD83C\uDFC6 Leader: ").append(leader.getName())
          .append(" — ").append(df2.format(leader.getBestScore() * 100)).append("% fitness");
        if (leaderVel > 0) {
            sb.append(", +").append(df4.format(leaderVel * 100)).append("%/s");
        }
        sb.append('\n');

        // Per-contestant mini leaderboard with promise indicators
        sb.append('\n');
        for (int i = 0; i < alive.size(); i++) {
            TournamentContestant c = alive.get(i);
            double vel = c.getFitnessTracker().getVelocity();
            double acc = c.getFitnessTracker().getAcceleration();
            double score = c.getBestScore();
            long startMs = c.getStartTimeMs();
            long uptimeSec = startMs > 0 ? (System.currentTimeMillis() - startMs) / 1000 : 0;

            String rank = String.format("  %2d. ", i + 1);
            sb.append(rank).append(c.getName());

            // Score
            sb.append("  ").append(df2.format(score * 100)).append("%");

            // Velocity indicator
            if (vel > 0.0001) {
                sb.append("  \u25B2").append(df4.format(vel * 100)).append("/s");
            } else if (vel < -0.0001) {
                sb.append("  \u25BC").append(df4.format(Math.abs(vel) * 100)).append("/s");
            } else {
                sb.append("  \u25AC flat");
            }

            // Promise tag
            if (vel > 0.001) sb.append("  \u2B50 FAST");
            else if (vel > 0 && acc > 0) sb.append("  \u2197 rising");
            else if (vel <= 0 && acc < 0) sb.append("  \u2198 fading");
            else if (uptimeSec > 30 && score < leader.getBestScore() * 0.95 && vel <= 0)
                sb.append("  \u26A0 at risk");

            // Grace period
            if (c.isProtected()) sb.append("  \u2B50");

            // Uptime + lifespan
            if (evoTournament != null && evoTournament.getMaxLifespanSeconds() > 0 && startMs > 0) {
                long remainLife = evoTournament.getMaxLifespanSeconds() - uptimeSec;
                if (remainLife <= 0) sb.append("  \u23F3 EXPIRED");
                else if (uptimeSec < 60) sb.append("  [" + uptimeSec + "s/" + evoTournament.getMaxLifespanSeconds() + "s]");
                else sb.append("  [" + (uptimeSec / 60) + "m" + (uptimeSec % 60) + "s/" + evoTournament.getMaxLifespanSeconds() + "s]");
            } else if (uptimeSec < 60) sb.append("  [" + uptimeSec + "s]");
            else sb.append("  [" + (uptimeSec / 60) + "m" + (uptimeSec % 60) + "s]");

            sb.append('\n');
        }

        // Summary stats
        double avgScore = alive.stream().mapToDouble(TournamentContestant::getBestScore).average().orElse(0);
        double maxVel = alive.stream().mapToDouble(c -> c.getFitnessTracker().getVelocity()).max().orElse(0);
        String fastestName = alive.stream()
                .max((a, b) -> Double.compare(a.getFitnessTracker().getVelocity(),
                        b.getFitnessTracker().getVelocity()))
                .map(TournamentContestant::getName).orElse("?");

        sb.append('\n');
        sb.append("  Avg fitness: ").append(df2.format(avgScore * 100)).append("%");
        sb.append("  |  Spread: ").append(df2.format((leader.getBestScore() - trailer.getBestScore()) * 100)).append("pp");
        if (maxVel > 0) {
            sb.append("  |  Fastest learner: ").append(fastestName);
        }
        sb.append('\n');

        if (evoTournament != null && evoTournament.isRunning()) {
            // Ranking mode
            EvolutionaryTournament.RankingStrategy strat = evoTournament.getRankingStrategy();
            float[] w = evoTournament.getCurrentEffectiveWeights();
            sb.append("  Ranking: ").append(strat);
            if (strat == EvolutionaryTournament.RankingStrategy.AUTO) {
                sb.append(" [fit=").append(df2.format(w[0]))
                  .append(" vel=").append(df2.format(w[1])).append("]");
            }
            sb.append('\n');

            // Lifespan warnings
            int lifespan = evoTournament.getMaxLifespanSeconds();
            if (lifespan > 0) {
                long now = System.currentTimeMillis();
                for (TournamentContestant c : alive) {
                    long startMs = c.getStartTimeMs();
                    if (startMs > 0) {
                        long ageSec = (now - startMs) / 1000;
                        long remainLife = lifespan - ageSec;
                        if (remainLife <= 15 && remainLife > 0) {
                            sb.append("  \u23F3 ").append(c.getName())
                              .append(" expires in ").append(remainLife).append("s\n");
                        }
                    }
                }
            }

            int remaining = evoTournament.getSecondsUntilNextTick();
            if (remaining >= 0) {
                sb.append("  Next cycle in ").append(remaining).append("s");
                sb.append(" [").append(evoTournament.getCutoffSeconds()).append("s interval");
                if (evoTournament.isAdaptiveCutoff()) sb.append(", adaptive");
                sb.append("]\n");
            }
        }

        return sb.toString();
    }

    public EvolutionaryTournament getEvoTournament() { return evoTournament; }

    /** Creates the EvolutionaryTournament if not already present, for programmatic use. */
    public void createEvoTournament() {
        if (evoTournament == null) {
            evoTournament = new EvolutionaryTournament(artEvolver, contestants);
        }
    }

    private void notifyContestantsChanged() {
        tableModel.fireTableDataChanged();
        artEvolver.refreshContestantCombo();
    }

    private int getModelRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return -1;
        try {
            return table.convertRowIndexToModel(viewRow);
        } catch (IndexOutOfBoundsException e) { return -1; }
    }

    private void addContestant() {
        String name = JOptionPane.showInputDialog(this, "Contestant name:", "Contestant #" + nextId);
        if (name == null || name.trim().isEmpty()) return;

        TournamentContestant c = new TournamentContestant("c" + nextId++, name.trim());
        EvolutionConfig cfg = EvolutionConfig.fromCurrentSettings();
        artEvolver.populateConfigFromUI(cfg);
        c.setConfig(cfg);
        c.getConfig().name = name.trim();
        c.getConfig().chartColor = c.getChartColor();
        contestants.add(c);
        notifyContestantsChanged();
    }

    private void duplicateSelected() {
        int row = getModelRow();
        if (row < 0 || row >= contestants.size()) return;

        TournamentContestant src = contestants.get(row);
        String name = JOptionPane.showInputDialog(this, "Name for copy:", src.getName() + " (copy)");
        if (name == null || name.trim().isEmpty()) return;

        TournamentContestant c = new TournamentContestant("c" + nextId++, name.trim());
        EvolutionConfig cfg = src.getConfig().clone();
        cfg.name = name.trim();
        c.setConfig(cfg);
        cfg.chartColor = c.getChartColor();
        contestants.add(c);
        notifyContestantsChanged();
    }

    private void removeSelected() {
        int row = getModelRow();
        if (row < 0 || row >= contestants.size()) return;
        TournamentContestant c = contestants.get(row);
        if (c.isRunning()) {
            JOptionPane.showMessageDialog(this, "Stop the contestant before removing it.", "Cannot Remove", JOptionPane.WARNING_MESSAGE);
            return;
        }
        contestants.remove(row);
        notifyContestantsChanged();
        detailPanel.removeAll();
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void editSelected() {
        int row = getModelRow();
        if (row < 0 || row >= contestants.size()) return;
        TournamentContestant c = contestants.get(row);
        if (c.isRunning() || c.isFinished()) {
            JOptionPane.showMessageDialog(this, "Cannot edit a running or finished contestant.", "Cannot Edit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        showParamEditor(c);
    }

    private void showSelectedDetail() {
        int row = getModelRow();
        detailPanel.removeAll();
        if (row < 0 || row >= contestants.size()) {
            detailPanel.revalidate();
            detailPanel.repaint();
            return;
        }

        TournamentContestant c = contestants.get(row);
        EvolutionConfig cfg = c.getConfig();

        addDetail("Name", c.getName());
        addDetail("Generation", String.valueOf(c.getGeneration()));
        addDetail("Parentage", c.getParentage());

        if (c.isProtected()) {
            addDetail("Grace Period", c.getGraceTicks() + " ticks remaining \u2B50");
        }

        if (c.isPromoted()) {
            addDetail("Status", "\uD83C\uDFC5 PROMOTED at Gen " + c.getPromotedAtGeneration());
            addDetail("Final Score", new DecimalFormat("0.0000").format(c.getFinalScore() * 100) + "%");
        } else if (c.isEliminated()) {
            addDetail("Status", "ELIMINATED at Gen " + c.getEliminatedAtGeneration());
            addDetail("Final Score", new DecimalFormat("0.0000").format(c.getFinalScore() * 100) + "%");
        }

        FitnessTracker ft = c.getFitnessTracker();
        if (ft.getSnapshotCount() > 1) {
            DecimalFormat dfv = new DecimalFormat("0.000000");
            addDetail("Velocity", dfv.format(ft.getVelocity() * 100) + "%/s");
            addDetail("Acceleration", dfv.format(ft.getAcceleration() * 100) + "%/s\u00B2");
            addDetail("Peak Fitness", new DecimalFormat("0.0000").format(ft.getPeakFitness() * 100) + "%");
            addDetail("Peak Velocity", dfv.format(ft.getPeakVelocity() * 100) + "%/s");
        }

        if (c.getLineageNode() != null) {
            LineageNode ln = c.getLineageNode();
            addDetail("Lineage Fitness", new DecimalFormat("0.0000").format(
                    ln.getLineageFitness(0.7, 3) * 100) + "%");
            String ancestry = ln.getAncestryString(2);
            if (ancestry.contains("\n")) {
                addDetail("Ancestry", "");
                JTextArea txtAnc = new JTextArea(ancestry);
                txtAnc.setEditable(false);
                txtAnc.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
                txtAnc.setOpaque(false);
                txtAnc.setAlignmentX(Component.LEFT_ALIGNMENT);
                detailPanel.add(txtAnc);
            } else if (!ancestry.isEmpty()) {
                addDetail("Ancestry", ancestry);
            }
        }

        if (c.isRunning() && c.getStartTimeMs() > 0) {
            long sec = (System.currentTimeMillis() - c.getStartTimeMs()) / 1000;
            long h = sec / 3600; long m = (sec % 3600) / 60; long s = sec % 60;
            String runtime = (h > 0) ? h + "h " + m + "m " + s + "s" : (m > 0) ? m + "m " + s + "s" : s + "s";
            addDetail("Runtime", runtime);
            if (sec > 0 && c.getTotalIterations() > 0) {
                addDetail("Speed", String.format("%,d iter/s", c.getTotalIterations() / sec));
            }
        }

        addDetail("Threads", String.valueOf(cfg.threads));
        addDetail("Population", String.valueOf(cfg.population));
        addDetail("Crossover Max", String.valueOf(cfg.crossoverMax));
        addDetail("Evolve Iters/Batch", String.valueOf(cfg.evolveIterations));
        addDetail("Init Method", cfg.initializationMethod == 0 ? "Random" : cfg.initializationMethod == 1 ? "Smart Greedy" : "LAP Optimal");
        addDetail("Delta Evolution", String.valueOf(cfg.useDeltaEvolution));
        addDetail("Grid Mutations", String.valueOf((int) cfg.gridMutationChances));
        addDetail("Grid Decay", String.valueOf(cfg.gridMutationDecay));
        addDetail("Random Mutations", String.valueOf(cfg.randomMutationChances));
        addDetail("Random Mut. Prob", String.valueOf(cfg.randomMutationPercent));
        addDetail("Close Mutations", String.valueOf(cfg.closeMutationChances));
        addDetail("Close Mut. Prob", String.valueOf(cfg.closeMutationPercent));
        addDetail("Targeted Swaps", String.valueOf(cfg.targetedSwapAttempts));
        addDetail("Block Crossover", String.valueOf(cfg.blockCrossoverEnabled));

        JButton btnEdit = makeBtn("Edit Parameters...");
        btnEdit.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnEdit.addActionListener(e -> showParamEditor(c));
        detailPanel.add(Box.createVerticalStrut(8));
        detailPanel.add(btnEdit);

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void addDetail(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(400, 22));
        JLabel lblK = new JLabel(label + ":");
        lblK.setFont(SECTION_FONT);
        lblK.setPreferredSize(new Dimension(140, 18));
        JLabel lblV = new JLabel(value);
        lblV.setFont(TBL_FONT);
        row.add(lblK);
        row.add(lblV);
        detailPanel.add(row);
    }

    private void showParamEditor(TournamentContestant c) {
        EvolutionConfig cfg = c.getConfig();
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));

        JTextField fName           = addField(form, "Name:", c.getName());
        JSpinner sThreads          = addSpinner(form, "Threads:", cfg.threads, 1, 128, 1);
        JSpinner sPopulation       = addSpinner(form, "Population:", cfg.population, 1, 256, 1);
        JSpinner sCrossoverMax     = addSpinner(form, "Crossover Max:", cfg.crossoverMax, 1, 64, 1);
        JSpinner sEvolveIters      = addSpinner(form, "Evolve Iters:", cfg.evolveIterations, 1, 100, 1);
        JComboBox<String> cInit    = addCombo(form, "Init Method:", new String[]{"Random", "Smart Greedy", "LAP Optimal"}, cfg.initializationMethod);
        JCheckBox cDelta           = addCheck(form, "Delta Evolution:", cfg.useDeltaEvolution);
        JSpinner sGridMut          = addSpinner(form, "Grid Mutations:", (int) cfg.gridMutationChances, 0, 512, 4);
        JSpinner sGridDecay        = addSpinner(form, "Grid Decay (x1000):", (int)(cfg.gridMutationDecay * 1000), 0, 1000, 10);
        JSpinner sRndMut           = addSpinner(form, "Random Mutations:", cfg.randomMutationChances, 0, 10000, 100);
        JSpinner sRndPct           = addSpinner(form, "Rnd Mut Prob (x10000):", (int)(cfg.randomMutationPercent * 10000), 0, 10000, 1);
        JSpinner sCloseMut         = addSpinner(form, "Close Mutations:", cfg.closeMutationChances, 0, 200, 5);
        JSpinner sClosePct         = addSpinner(form, "Close Prob (x10000):", (int)(cfg.closeMutationPercent * 10000), 0, 10000, 1);
        JSpinner sTargeted         = addSpinner(form, "Targeted Swaps:", cfg.targetedSwapAttempts, 0, 128, 4);
        JCheckBox cBlock           = addCheck(form, "Block Crossover:", cfg.blockCrossoverEnabled);

        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(form),
                "Edit: " + c.getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            c.setName(fName.getText().trim().isEmpty() ? c.getName() : fName.getText().trim());
            cfg.name = c.getName();
            cfg.threads = (int) sThreads.getValue();
            cfg.population = (int) sPopulation.getValue();
            cfg.crossoverMax = (int) sCrossoverMax.getValue();
            cfg.evolveIterations = (int) sEvolveIters.getValue();
            cfg.initializationMethod = cInit.getSelectedIndex();
            cfg.useDeltaEvolution = cDelta.isSelected();
            cfg.gridMutationChances = (int) sGridMut.getValue();
            cfg.gridMutationDecay = (int) sGridDecay.getValue() / 1000f;
            cfg.randomMutationChances = (int) sRndMut.getValue();
            cfg.randomMutationPercent = (int) sRndPct.getValue() / 10000f;
            cfg.closeMutationChances = (int) sCloseMut.getValue();
            cfg.closeMutationPercent = (int) sClosePct.getValue() / 10000f;
            cfg.targetedSwapAttempts = (int) sTargeted.getValue();
            cfg.blockCrossoverEnabled = cBlock.isSelected();

            notifyContestantsChanged();
            showSelectedDetail();
        }
    }

    private JTextField addField(JPanel form, String label, String value) {
        form.add(new JLabel(label));
        JTextField f = new JTextField(value);
        form.add(f);
        return f;
    }

    private JSpinner addSpinner(JPanel form, String label, int value, int min, int max, int step) {
        form.add(new JLabel(label));
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        form.add(s);
        return s;
    }

    private JComboBox<String> addCombo(JPanel form, String label, String[] options, int selected) {
        form.add(new JLabel(label));
        JComboBox<String> c = new JComboBox<>(options);
        c.setSelectedIndex(Math.min(selected, options.length - 1));
        form.add(c);
        return c;
    }

    private JCheckBox addCheck(JPanel form, String label, boolean value) {
        form.add(new JLabel(label));
        JCheckBox c = new JCheckBox("", value);
        form.add(c);
        return c;
    }

    private static double parseScore(Object val) {
        if (val instanceof String) {
            String s = ((String) val).replace("%", "").trim();
            if ("--".equals(s)) return -1;
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return -1; }
        }
        return -1;
    }

    public void refreshTable() {
        int sel = table.getSelectedRow();
        int modelSel = (sel >= 0) ? table.convertRowIndexToModel(sel) : -1;
        tableModel.fireTableDataChanged();
        if (modelSel >= 0 && modelSel < contestants.size()) {
            try {
                int viewRow = table.convertRowIndexToView(modelSel);
                if (viewRow >= 0) table.setRowSelectionInterval(viewRow, viewRow);
            } catch (IndexOutOfBoundsException ignored) {}
        }
    }

    private static class ColorCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, "", sel, foc, r, c);
            if (v instanceof Color) {
                lbl.setBackground((Color) v);
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }

    private class ContestantTableModel extends AbstractTableModel {
        private final String[] COLS = {"#", "", "Name", "Score", "Velocity", "Status", "Gen", "Parentage", "Breed", "Parameters"};

        @Override public int getRowCount() { return contestants.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            if (row < 0 || row >= contestants.size()) return "";
            TournamentContestant c = contestants.get(row);
            DecimalFormat df = new DecimalFormat("0.0000");
            switch (col) {
                case 0: return row + 1;
                case 1: return c.isEliminated() ? Color.DARK_GRAY
                              : c.isPromoted() ? new Color(255, 193, 7) : c.getChartColor();
                case 2: {
                    String n;
                    if (c.isPromoted()) n = "\uD83C\uDFC5 " + c.getName();
                    else if (c.isEliminated()) n = "\u2620 " + c.getName();
                    else n = c.getName();
                    if (c.isProtected()) n += " \u2B50";
                    return n;
                }
                case 3: {
                    double score = c.isFinished() ? c.getFinalScore() : c.getBestScore();
                    return score > 0 ? df.format(score * 100) + "%" : "--";
                }
                case 4: {
                    if (c.isFinished()) return "--";
                    double vel = c.getFitnessTracker().getVelocity();
                    if (vel <= 0) return "--";
                    return new DecimalFormat("0.000000").format(vel * 100) + "/s";
                }
                case 5: {
                    if (c.isPromoted()) return "\uD83C\uDFC5 Promoted (Gen " + c.getPromotedAtGeneration() + ")";
                    if (c.isEliminated()) return "Eliminated (Gen " + c.getEliminatedAtGeneration() + ")";
                    return c.isRunning() ? "Running" : "Stopped";
                }
                case 6: return c.getGeneration();
                case 7: return c.getParentage();
                case 8: return c.getBreedType();
                case 9: return c.getConfig().toSummary();
                default: return "";
            }
        }
    }
}
