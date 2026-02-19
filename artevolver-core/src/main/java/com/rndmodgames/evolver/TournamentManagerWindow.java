package com.rndmodgames.evolver;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

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

        JButton btnStartAll = makeBtn("\u25B6 Start All");
        btnStartAll.setBackground(new Color(46, 139, 87));
        btnStartAll.setForeground(Color.WHITE);
        btnStartAll.addActionListener(e -> artEvolver.startTournament());

        JButton btnStopAll = makeBtn("\u25A0 Stop All");
        btnStopAll.setBackground(new Color(178, 34, 34));
        btnStopAll.setForeground(Color.WHITE);
        btnStopAll.addActionListener(e -> artEvolver.stopTournament());

        buttonBar.add(btnAdd);
        buttonBar.add(btnDuplicate);
        buttonBar.add(btnRemove);
        buttonBar.add(Box.createHorizontalStrut(20));
        buttonBar.add(btnStartAll);
        buttonBar.add(btnStopAll);

        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        detailPanel.setBackground(BG);

        JScrollPane detailScroll = new JScrollPane(detailPanel);
        detailScroll.setPreferredSize(new Dimension(300, 200));
        detailScroll.setBorder(BorderFactory.createTitledBorder("Contestant Parameters"));

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
        topPanel.add(buttonBar, BorderLayout.NORTH);
        topPanel.add(tableScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, detailScroll);
        split.setResizeWeight(0.6);
        split.setDividerLocation(280);
        add(split, BorderLayout.CENTER);
    }

    private JButton makeBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        btn.setFocusPainted(false);
        return btn;
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
        tableModel.fireTableDataChanged();
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
        tableModel.fireTableDataChanged();
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
        tableModel.fireTableDataChanged();
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

            tableModel.fireTableDataChanged();
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
        tableModel.fireTableDataChanged();
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
        private final String[] COLS = {"#", "", "Name", "Score", "Status", "Parameters"};

        @Override public int getRowCount() { return contestants.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            TournamentContestant c = contestants.get(row);
            DecimalFormat df = new DecimalFormat("0.0000");
            switch (col) {
                case 0: return row + 1;
                case 1: return c.getChartColor();
                case 2: return c.getName();
                case 3: return c.getBestScore() > Double.MIN_VALUE ? df.format(c.getBestScore() * 100) + "%" : "--";
                case 4: return c.isRunning() ? "Running" : "Stopped";
                case 5: return c.getConfig().toSummary();
                default: return "";
            }
        }
    }
}
