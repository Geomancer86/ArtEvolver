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
    private JLabel lblGeneration;
    private JLabel lblCountdown;
    private JLabel lblBestEver;
    private JTextArea txtHistory;
    private JButton btnEvolve;
    private JButton btnQuickSetupRef;
    private JButton btnStartAllRef;
    private JButton btnStopAllRef;

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
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setMaxWidth(40);
        table.getColumnModel().getColumn(6).setPreferredWidth(200);

        table.getColumnModel().getColumn(1).setCellRenderer(new ColorCellRenderer());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(600, 200));

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
        btnStartAllRef.addActionListener(e -> artEvolver.startTournament());

        btnStopAllRef = makeBtn("\u25A0 Stop All");
        btnStopAllRef.setBackground(new Color(178, 34, 34));
        btnStopAllRef.setForeground(Color.WHITE);
        btnStopAllRef.addActionListener(e -> artEvolver.stopTournament());

        buttonBar.add(btnQuickSetupRef);
        buttonBar.add(Box.createHorizontalStrut(8));
        buttonBar.add(btnAdd);
        buttonBar.add(btnDuplicate);
        buttonBar.add(btnRemove);
        buttonBar.add(Box.createHorizontalStrut(20));
        buttonBar.add(btnStartAllRef);
        buttonBar.add(btnStopAllRef);

        JPanel evoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        evoBar.setOpaque(false);

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

        JPanel allBars = new JPanel();
        allBars.setLayout(new BoxLayout(allBars, BoxLayout.Y_AXIS));
        allBars.setOpaque(false);
        allBars.add(buttonBar);
        allBars.add(evoBar);

        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        detailPanel.setBackground(BG);

        JScrollPane detailScroll = new JScrollPane(detailPanel);
        detailScroll.setPreferredSize(new Dimension(300, 120));
        detailScroll.setBorder(BorderFactory.createTitledBorder("Contestant Parameters"));

        txtHistory = new JTextArea(5, 40);
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
        split.setDividerLocation(320);
        add(split, BorderLayout.CENTER);
        setSize(960, 620);
    }

    private JButton makeBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        btn.setFocusPainted(false);
        return btn;
    }

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
    };

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
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts;
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

        form.add(new JLabel("Total CPU Cores to Use:"));
        JSpinner spnCores = new JSpinner(new SpinnerNumberModel(
                Math.max(2, availableCores - 2), 2, availableCores * 2, 1));
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
        if (evoTournament == null) {
            evoTournament = new EvolutionaryTournament(artEvolver, contestants);
        }

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

            evoTournament.start();
            btnEvolve.setText("\u25A0 Stop Evolving");
            btnEvolve.setBackground(new Color(178, 34, 34));
            setEvoLockButtons(true);
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

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        form.add(new JLabel("Cutoff Interval (seconds):"));
        JSpinner spnCutoff = new JSpinner(new SpinnerNumberModel(
                evoTournament.getCutoffSeconds(), 10, 600, 10));
        form.add(spnCutoff);

        form.add(new JLabel("Mutation Rate (0.0-1.0):"));
        JSpinner spnMutRate = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getMutationRate(), 0.0, 1.0, 0.05));
        form.add(spnMutRate);

        form.add(new JLabel("Mutation Strength (0.0-1.0):"));
        JSpinner spnMutStr = new JSpinner(new SpinnerNumberModel(
                (double) evoTournament.getMutationStrength(), 0.0, 1.0, 0.05));
        form.add(spnMutStr);

        form.add(new JLabel("Min Contestants (floor):"));
        JSpinner spnMinPop = new JSpinner(new SpinnerNumberModel(
                evoTournament.getMinContestants(), 2, 20, 1));
        form.add(spnMinPop);

        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JLabel hint = new JLabel("<html><i>Cutoff can be changed while evolving is running.</i></html>");
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        hint.setForeground(Color.GRAY);
        form.add(hint);
        form.add(new JLabel(""));

        int result = JOptionPane.showConfirmDialog(this, form,
                "Evolutionary Tournament Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            evoTournament.setCutoffSeconds((int) spnCutoff.getValue());
            evoTournament.setMutationRate(((Number) spnMutRate.getValue()).floatValue());
            evoTournament.setMutationStrength(((Number) spnMutStr.getValue()).floatValue());
            evoTournament.setMinContestants((int) spnMinPop.getValue());
        }
    }

    /** Called periodically to update generation, countdown, best-ever, history. */
    public void refreshEvolutionaryState() {
        if (evoTournament == null) return;

        lblGeneration.setText("Gen: " + evoTournament.getGeneration());

        int remaining = evoTournament.getSecondsUntilNextTick();
        if (remaining >= 0) {
            int m = remaining / 60;
            int s = remaining % 60;
            String countText = (m > 0) ? String.format("Next: %d:%02d", m, s) : String.format("Next: %ds", s);
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
            lblBestEver.setText(bestText);
        }

        List<EvolutionaryTournament.GenerationRecord> records = evoTournament.getHistory();
        int historyLines = txtHistory.getLineCount() - 1;
        if (historyLines < records.size()) {
            StringBuilder sb = new StringBuilder();
            for (EvolutionaryTournament.GenerationRecord rec : records) {
                sb.append(rec.toString()).append('\n');
            }
            txtHistory.setText(sb.toString());
            txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
        }
    }

    public EvolutionaryTournament getEvoTournament() { return evoTournament; }

    private void notifyContestantsChanged() {
        tableModel.fireTableDataChanged();
        artEvolver.refreshContestantCombo();
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
        table.setRowSelectionInterval(contestants.size() - 1, contestants.size() - 1);
    }

    private void duplicateSelected() {
        int row = table.getSelectedRow();
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
        table.setRowSelectionInterval(contestants.size() - 1, contestants.size() - 1);
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
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
        int row = table.getSelectedRow();
        if (row < 0 || row >= contestants.size()) return;
        TournamentContestant c = contestants.get(row);
        if (c.isRunning()) {
            JOptionPane.showMessageDialog(this, "Stop the contestant before editing.", "Cannot Edit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        showParamEditor(c);
    }

    private void showSelectedDetail() {
        int row = table.getSelectedRow();
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

    public void refreshTable() {
        int sel = table.getSelectedRow();
        tableModel.fireTableDataChanged();
        if (sel >= 0 && sel < contestants.size()) {
            table.setRowSelectionInterval(sel, sel);
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
        private final String[] COLS = {"#", "", "Name", "Score", "Status", "Gen", "Parameters"};

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
                case 1: return c.getChartColor();
                case 2: return c.getName();
                case 3: return c.getBestScore() > 0 ? df.format(c.getBestScore() * 100) + "%" : "--";
                case 4: return c.isRunning() ? "Running" : "Stopped";
                case 5: return c.getGeneration();
                case 6: return c.getConfig().toSummary();
                default: return "";
            }
        }
    }
}
