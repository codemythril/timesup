package de.timetracker.ui;

import de.timetracker.database.TimeEntryDAO;
import de.timetracker.model.TimeEntry;
import de.timetracker.model.ConsolidatedEntry;
import de.timetracker.utils.TimeFormatter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog zur Anzeige historischer Zeiterfassungsdaten
 */
public class HistoricalViewDialog extends JDialog {
    private final TimeEntryDAO dao;
    private JTable timeTable;
    private JTable consolidatedTable;
    private JTabbedPane tabbedPane;
    private JLabel dateLabel;
    private JSpinner dateSpinner;
    private LocalDate selectedDate;
    private JLabel statisticsLabel;
    private JLabel statusLabel;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public HistoricalViewDialog(Frame owner, TimeEntryDAO dao) {
        super(owner, "Historische Zeiterfassung", true);
        this.dao = dao;
        this.selectedDate = LocalDate.now();

        setSize(800, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initializeComponents();
        layoutComponents();
        loadData();
    }

    private void initializeComponents() {
        // Date Spinner
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        dateSpinner.addChangeListener(e -> {
            selectedDate = ((java.util.Date) dateSpinner.getValue()).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            loadData();
        });

        // Date Label
        dateLabel = new JLabel("Datum:");
        dateLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Time Table
        timeTable = new JTable();
        timeTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        timeTable.setRowHeight(25);
        timeTable.setGridColor(new Color(230, 230, 230));
        timeTable.setShowGrid(true);
        timeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Consolidated Table
        consolidatedTable = new JTable();
        consolidatedTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        consolidatedTable.setRowHeight(25);
        consolidatedTable.setGridColor(new Color(230, 230, 230));
        consolidatedTable.setShowGrid(true);
        consolidatedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Status und Statistiken
        statisticsLabel = new JLabel("Gesamtzeit: --:-- | Nettozeit: --:-- | Pausen: --:--");
        statisticsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statisticsLabel.setForeground(new Color(52, 73, 94));

        statusLabel = new JLabel("Bereit");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        // Top Panel - Date Selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(new Color(245, 245, 245));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        topPanel.add(dateLabel);
        topPanel.add(dateSpinner);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel - Tables
        setupTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Bottom Panel - Statistiken und Status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(250, 250, 250));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        bottomPanel.add(statisticsLabel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupTabbedPane() {
        // Detaillierte Tabelle
        JScrollPane timeTableScrollPane = new JScrollPane(timeTable);
        timeTableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Detaillierte Zeiterfassung",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12)
        ));

        tabbedPane.addTab("Zeiterfassung", createTabIcon("⏱"), timeTableScrollPane);

        // Konsolidierte Tabelle
        JScrollPane consolidatedScrollPane = new JScrollPane(consolidatedTable);
        consolidatedScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Konsolidierte Einträge",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12)
        ));

        tabbedPane.addTab("Abschluss", createTabIcon("📊"), consolidatedScrollPane);
    }

    private Icon createTabIcon(String text) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.setColor(Color.GRAY);
                g.drawString(text, x, y + 12);
            }

            @Override
            public int getIconWidth() { return 16; }

            @Override
            public int getIconHeight() { return 16; }
        };
    }

    private void loadData() {
        // Lade Zeiteinträge
        List<TimeEntry> entries = dao.getTimeEntriesByDate(selectedDate);
        updateTimeTable(entries);

        // Lade konsolidierte Einträge
        List<ConsolidatedEntry> consolidated = dao.getConsolidatedEntriesByDate(selectedDate);
        updateConsolidatedTable(consolidated);

        // Aktualisiere Fenstertitel
        setTitle("Historische Zeiterfassung - " + TimeFormatter.formatDateForTitle(selectedDate));

        // Aktualisiere Statistiken
        updateStatistics(entries);

        // Status aktualisieren
        String dayName = TimeFormatter.getDayName(selectedDate.getDayOfWeek().getValue());
        boolean hasData = !entries.isEmpty() || !consolidated.isEmpty();
        boolean isCompleted = !consolidated.isEmpty();

        String status = String.format("%s, %s", dayName, selectedDate.format(DATE_FORMATTER));
        if (!hasData) {
            status += " - Keine Daten";
        } else if (isCompleted) {
            status += " - Tag abgeschlossen";
        } else {
            status += " - " + entries.size() + " Einträge";
        }

        statusLabel.setText(status);

        // Tab-Auswahl basierend auf verfügbaren Daten
        if (isCompleted) {
            tabbedPane.setSelectedIndex(1); // Abschluss-Tab
        } else {
            tabbedPane.setSelectedIndex(0); // Detail-Tab
        }

        System.out.println("Historische Daten geladen für " + selectedDate + ": " +
                entries.size() + " Einträge, " +
                consolidated.size() + " konsolidiert");
    }

    private void updateTimeTable(List<TimeEntry> entries) {
        String[] columnNames = {"Startzeit", "Endzeit", "Dauer", "Beschreibung", "Pause"};
        Object[][] data = new Object[entries.size()][5];

        for (int i = 0; i < entries.size(); i++) {
            TimeEntry entry = entries.get(i);
            data[i][0] = entry.getStartTimeFormatted();
            data[i][1] = entry.getEndTimeFormatted();
            data[i][2] = entry.getDurationFormatted();
            data[i][3] = entry.getDescription();
            data[i][4] = entry.isBreak() ? "Ja" : "Nein";
        }

        timeTable.setModel(new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only
            }
        });

        // Spaltenbreiten anpassen
        if (timeTable.getColumnModel().getColumnCount() > 0) {
            timeTable.getColumnModel().getColumn(0).setPreferredWidth(80);
            timeTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            timeTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            timeTable.getColumnModel().getColumn(3).setPreferredWidth(300);
            timeTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        }
    }

    private void updateConsolidatedTable(List<ConsolidatedEntry> entries) {
        String[] columnNames = {"Startzeit", "Endzeit", "Dauer", "Beschreibung"};
        Object[][] data = new Object[entries.size()][4];

        for (int i = 0; i < entries.size(); i++) {
            ConsolidatedEntry entry = entries.get(i);
            data[i][0] = entry.getStartTimeFormatted();
            data[i][1] = entry.getEndTimeFormatted();
            data[i][2] = entry.getDurationFormatted();
            data[i][3] = entry.getDescription();
        }

        consolidatedTable.setModel(new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only
            }
        });

        // Spaltenbreiten anpassen
        if (consolidatedTable.getColumnModel().getColumnCount() > 0) {
            consolidatedTable.getColumnModel().getColumn(0).setPreferredWidth(80);
            consolidatedTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            consolidatedTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            consolidatedTable.getColumnModel().getColumn(3).setPreferredWidth(300);
        }
    }

    private void updateStatistics(List<TimeEntry> entries) {
        long totalMinutes = entries.stream()
                .filter(e -> e.getEndTime() != null)
                .mapToLong(TimeEntry::getDurationMinutes)
                .sum();

        long workMinutes = entries.stream()
                .filter(e -> e.getEndTime() != null && !e.isBreak())
                .mapToLong(TimeEntry::getDurationMinutes)
                .sum();

        long breakMinutes = entries.stream()
                .filter(e -> e.getEndTime() != null && e.isBreak())
                .mapToLong(TimeEntry::getDurationMinutes)
                .sum();

        statisticsLabel.setText(String.format(
                "Gesamtzeit: %s | Nettozeit: %s | Pausen: %s",
                TimeFormatter.formatDuration(totalMinutes),
                TimeFormatter.formatDuration(workMinutes),
                TimeFormatter.formatDuration(breakMinutes)
        ));
    }

    /**
     * Setzt das Datum und lädt entsprechende Daten
     */
    public void setDate(LocalDate date) {
        this.selectedDate = date;
        dateSpinner.setValue(java.util.Date.from(date.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()));
        loadData();
    }

    /**
     * Gibt das aktuell angezeigte Datum zurück
     */
    public LocalDate getCurrentDate() {
        return selectedDate;
    }
}