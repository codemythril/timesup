package de.timetracker.ui;

import de.timetracker.database.DatabaseManager;
import de.timetracker.database.TimeEntryDAO;
import de.timetracker.model.TimeEntry;
import de.timetracker.model.ConsolidatedEntry;
import de.timetracker.ui.components.EditableTable;
import de.timetracker.utils.AlwaysOnTopHandler;
import de.timetracker.utils.TimeFormatter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Hauptfenster der Zeiterfassungsapplikation
 */
public class MainWindow extends JFrame {
    // Components
    private JButton startStopButton;
    private JButton completeButton;
    private EditableTable timeTable;
    private JTable consolidatedTable;
    private JTabbedPane tabbedPane;
    private JLabel totalTimeLabel;
    private JLabel netTimeLabel;
    private JLabel breakTimeLabel;
    private JLabel statusLabel;
    private JCheckBoxMenuItem alwaysOnTopMenuItem; // Referenz für spätere Updates

    // Data and Logic
    private final TimeEntryDAO dao;
    private AlwaysOnTopHandler alwaysOnTopHandler; // Nicht final, da später initialisiert
    private TimeEntry currentActivity;
    private Timer clockTimer;
    private Timer activityMonitorTimer; // Neuer Timer für Aktivitätsüberwachung
    private boolean isRunning = false;
    private boolean isDayCompleted = false; // Neues Flag für Tagesabschluss-Status

    // Constants
    private static final Color RUNNING_COLOR = new Color(46, 204, 113);
    private static final Color STOPPED_COLOR = new Color(52, 152, 219);
    private static final Color COMPLETE_COLOR = new Color(241, 196, 15);

    public MainWindow() {
        System.out.println("Initialisiere TimeTracker MainWindow...");

        this.dao = new TimeEntryDAO();

        setTitle(createWindowTitle());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setMinimumSize(new Dimension(650, 450));

        System.out.println("Initialisiere GUI-Komponenten...");
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupMenuBar();

        // Always-on-top Handler NACH GUI-Initialisierung
        System.out.println("Initialisiere Always-on-top Handler...");
        this.alwaysOnTopHandler = new AlwaysOnTopHandler(this);

        // Menu-Item Status nach Handler-Initialisierung aktualisieren
        if (alwaysOnTopMenuItem != null) {
            alwaysOnTopMenuItem.setSelected(alwaysOnTopHandler.isAlwaysOnTop());
        }

        // Daten laden (kann länger dauern)
        System.out.println("Lade Anwendungsdaten...");
        loadTodaysData();
        updateStatistics();

        // Clock Timer starten
        System.out.println("Starte Uhr-Timer...");
        startClockTimer();

        setLocationRelativeTo(null);
        System.out.println("MainWindow-Initialisierung abgeschlossen");
    }

    private void initializeComponents() {
        // Start/Stop Button
        startStopButton = new JButton("▶ Starten");
        startStopButton.setPreferredSize(new Dimension(140, 35));
        startStopButton.setMinimumSize(new Dimension(140, 35));
        startStopButton.setMaximumSize(new Dimension(140, 35));

        // Complete Button - Text wird dynamisch angepasst
        completeButton = new JButton("Tag abschließen");
        completeButton.setPreferredSize(new Dimension(140, 35));
        completeButton.setMinimumSize(new Dimension(140, 35));
        completeButton.setMaximumSize(new Dimension(140, 35));

        // Einheitliche Button-Konfiguration
        JButton[] buttons = {startStopButton, completeButton};
        for (JButton button : buttons) {
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            button.setOpaque(true);
            button.setBorderPainted(true);
            button.setContentAreaFilled(true);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createRaisedBevelBorder());
        }

        // Individuelle Farben
        startStopButton.setBackground(STOPPED_COLOR);
        startStopButton.setForeground(Color.WHITE);

        completeButton.setBackground(COMPLETE_COLOR);
        completeButton.setForeground(Color.WHITE);

        // Time Table
        timeTable = new EditableTable(dao);
        timeTable.addChangeListener(this::updateStatistics);

        // Consolidated Table (einfache JTable)
        consolidatedTable = new JTable();
        consolidatedTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        consolidatedTable.setRowHeight(25);
        consolidatedTable.setGridColor(new Color(230, 230, 230));
        consolidatedTable.setShowGrid(true);
        consolidatedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Status Labels
        totalTimeLabel = new JLabel("Gesamtzeit: 00:00");
        netTimeLabel = new JLabel("Nettozeit: 00:00");
        breakTimeLabel = new JLabel("Pausen: 00:00");
        statusLabel = new JLabel("Bereit");

        // Label-Formatierung
        Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        totalTimeLabel.setFont(labelFont);
        netTimeLabel.setFont(labelFont);
        breakTimeLabel.setFont(labelFont);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));

        totalTimeLabel.setForeground(new Color(52, 73, 94));
        netTimeLabel.setForeground(new Color(39, 174, 96));
        breakTimeLabel.setForeground(new Color(230, 126, 34));
        statusLabel.setForeground(Color.GRAY);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        // Top Panel - Buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(new Color(245, 245, 245));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        topPanel.add(startStopButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(completeButton);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel - Tables
        setupTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Bottom Panel - Statistics
        JPanel bottomPanel = createStatisticsPanel();
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

        tabbedPane.addTab("Zeiterfassung", createTabIcon("⏱"), timeTableScrollPane, "Detaillierte Zeiteinträge bearbeiten");

        // Konsolidierte Tabelle
        JScrollPane consolidatedScrollPane = new JScrollPane(consolidatedTable);
        consolidatedScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Konsolidierte Einträge",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12)
        ));

        tabbedPane.addTab("Abschluss", createTabIcon("📊"), consolidatedScrollPane, "Konsolidierte Tagesübersicht");
    }

    private JPanel createStatisticsPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(250, 250, 250));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        // Statistiken Links
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        statsPanel.setBackground(new Color(250, 250, 250));
        statsPanel.add(totalTimeLabel);
        statsPanel.add(createSeparator());
        statsPanel.add(netTimeLabel);
        statsPanel.add(createSeparator());
        statsPanel.add(breakTimeLabel);

        // Status Rechts
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBackground(new Color(250, 250, 250));
        statusPanel.add(statusLabel);

        bottomPanel.add(statsPanel, BorderLayout.WEST);
        bottomPanel.add(statusPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    private JLabel createSeparator() {
        JLabel separator = new JLabel("|");
        separator.setForeground(Color.LIGHT_GRAY);
        return separator;
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

    private void setupEventHandlers() {
        // Start/Stop Button
        startStopButton.addActionListener(e -> toggleActivity());

        // Complete Button
        completeButton.addActionListener(e -> completeDay());

        // Window Closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentActivity != null && currentActivity.getEndTime() == null) {
                    int option = JOptionPane.showConfirmDialog(
                            MainWindow.this,
                            "Es läuft noch eine Aktivität. Soll sie automatisch beendet werden?",
                            "Aktivität läuft",
                            JOptionPane.YES_NO_CANCEL_OPTION
                    );

                    if (option == JOptionPane.YES_OPTION) {
                        stopCurrentActivity();
                        closeApplication();
                    } else if (option == JOptionPane.NO_OPTION) {
                        closeApplication();
                    }
                    // Bei CANCEL_OPTION: Fenster bleibt offen
                } else {
                    closeApplication();
                }
            }
        });
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Ansicht-Menü
        JMenu viewMenu = new JMenu("Ansicht");

        // Always-on-top Item - ohne Handler-Zugriff bei Initialisierung
        JCheckBoxMenuItem alwaysOnTopItem = new JCheckBoxMenuItem("Always-on-top", true);
        alwaysOnTopItem.addActionListener(e -> {
            if (alwaysOnTopHandler != null) {
                alwaysOnTopHandler.toggleAlwaysOnTop();
                alwaysOnTopItem.setSelected(alwaysOnTopHandler.isAlwaysOnTop());
            }
        });

        JMenuItem opacityItem = new JMenuItem("Transparenz...");
        opacityItem.addActionListener(e -> showOpacityDialog());

        JMenuItem historicalItem = new JMenuItem("Historische Zeiterfassung...");
        historicalItem.addActionListener(e -> showHistoricalView());

        viewMenu.add(alwaysOnTopItem);
        viewMenu.add(opacityItem);
        viewMenu.addSeparator();
        viewMenu.add(historicalItem);

        // Extras-Menü
        JMenu extrasMenu = new JMenu("Extras");

        JMenuItem exportItem = new JMenuItem("Exportieren...");
        exportItem.addActionListener(e -> exportData());

        JMenuItem aboutItem = new JMenuItem("Über...");
        aboutItem.addActionListener(e -> showAboutDialog());

        extrasMenu.add(exportItem);
        extrasMenu.addSeparator();
        extrasMenu.add(aboutItem);

        menuBar.add(viewMenu);
        menuBar.add(extrasMenu);

        setJMenuBar(menuBar);

        // Referenz auf MenuItem speichern für spätere Updates
        this.alwaysOnTopMenuItem = alwaysOnTopItem;
    }

    private void toggleActivity() {
        if (isRunning) {
            stopCurrentActivity();
        } else {
            startNewActivity();
        }
    }

    private void startNewActivity() {
        // Berechne Startzeit basierend auf vorherigen Einträgen
        LocalTime startTime = calculateNextStartTime();

        System.out.println("Berechnete Startzeit für neue Aktivität: " + TimeFormatter.formatTime(startTime));

        // Erstelle neue Aktivität direkt ohne Dialog
        currentActivity = new TimeEntry(
                LocalDate.now(),
                startTime,
                "" // Leere Beschreibung initial
        );

        System.out.println("Neue Aktivität wird gestartet um " + currentActivity.getStartTimeFormatted());

        // In Datenbank speichern
        boolean success = dao.insertTimeEntry(currentActivity);
        if (success) {
            isRunning = true;
            updateStartStopButton();
            updateStatus("Läuft: (neue Aktivität)");

            // Tabelle aktualisieren und neuen Eintrag fokussieren
            loadTodaysData();

            // Fokus auf Beschreibungsfeld der neuen Zeile setzen
            SwingUtilities.invokeLater(() -> {
                int lastRow = timeTable.getRowCount() - 1;
                if (lastRow >= 0) {
                    timeTable.setRowSelectionInterval(lastRow, lastRow);
                    timeTable.editCellAt(lastRow, EditableTable.COL_DESCRIPTION);
                    Component editor = timeTable.getEditorComponent();
                    if (editor != null) {
                        editor.requestFocusInWindow();
                    }
                }
            });

            System.out.println("Neue Aktivität erfolgreich gestartet");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern der neuen Aktivität!",
                    "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Berechnet die nächste Startzeit basierend auf dem letzten Eintrag
     */
    private LocalTime calculateNextStartTime() {
        List<TimeEntry> todaysEntries = dao.getTimeEntriesByDate(LocalDate.now());

        if (todaysEntries.isEmpty()) {
            // Keine Einträge vorhanden - verwende aktuelle Zeit
            return LocalTime.now();
        }

        // Finde den Eintrag mit der spätesten Startzeit
        TimeEntry latestEntry = todaysEntries.stream()
                .filter(entry -> entry.getStartTime() != null)
                .max((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .orElse(null);

        if (latestEntry == null) {
            return LocalTime.now();
        }

        // Wenn der späteste Eintrag noch läuft (keine Endzeit), verwende aktuelle Zeit
        if (latestEntry.getEndTime() == null) {
            return LocalTime.now();
        }

        // Verwende Endzeit des spätesten Eintrags als neue Startzeit
        System.out.println("Nächste Startzeit basierend auf Eintrag: " +
                latestEntry.getDescription() + " (Ende: " +
                latestEntry.getEndTimeFormatted() + ")");

        return latestEntry.getEndTime();
    }

    private void stopCurrentActivity() {
        if (currentActivity != null && isRunning) {
            // Endzeit setzen
            currentActivity.stopActivity();

            // In Datenbank aktualisieren
            boolean success = dao.updateTimeEntry(currentActivity);
            if (success) {
                System.out.println("Aktivität gestoppt: " + currentActivity.getDescription() +
                        " (" + currentActivity.getDurationFormatted() + ")");

                isRunning = false;
                currentActivity = null;
                updateStartStopButton();
                updateStatus("Gestoppt um " + TimeFormatter.formatCurrentTime());

                // Tabelle komplett neu laden für korrekte Anzeige
                SwingUtilities.invokeLater(() -> {
                    loadTodaysData();
                    updateStatistics();

                    // Fokus auf Start-Button beibehalten
                    startStopButton.requestFocusInWindow();
                });
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Speichern der Aktivität!",
                        "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void completeDay() {
        if (isDayCompleted) {
            // Tag ist bereits abgeschlossen - frage nach Aufhebung
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "Der Tagesabschluss ist bereits erstellt.\n" +
                            "Möchten Sie den Abschluss aufheben und erneut erstellen?\n\n" +
                            "Warnung: Dadurch werden die konsolidierten Einträge gelöscht!",
                    "Tagesabschluss aufheben?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                removeDayCompletion();
            }
            return;
        }

        // Prüfe ob Tagesabschluss möglich ist
        if (isRunning) {
            JOptionPane.showMessageDialog(
                    this,
                    "Der Tag kann nicht abgeschlossen werden, solange eine Aktivität läuft.\n" +
                            "Bitte beenden Sie zuerst die laufende Aktivität.",
                    "Aktivität läuft",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!startStopButton.isEnabled()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Der Tag kann nicht abgeschlossen werden, solange neue Aktivitäten möglich sind.\n" +
                            "Bitte fügen Sie alle Aktivitäten hinzu, bevor Sie den Tag abschließen.",
                    "Neue Aktivitäten möglich",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Prüfe auf leere Beschreibungen
        if (timeTable.hasEmptyDescriptions()) {
            List<Integer> emptyRows = timeTable.getRowsWithEmptyDescriptions();
            String rowList = emptyRows.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            JOptionPane.showMessageDialog(
                    this,
                    "Der Tag kann nicht abgeschlossen werden, da folgende Zeilen keine Beschreibung haben:\n" +
                            "Zeile(n): " + rowList + "\n\n" +
                            "Bitte tragen Sie für alle Aktivitäten eine Beschreibung ein.",
                    "Fehlende Beschreibungen",
                    JOptionPane.WARNING_MESSAGE
            );

            // Fokussiere die erste Zeile ohne Beschreibung
            if (!emptyRows.isEmpty()) {
                int firstEmptyRow = emptyRows.get(0) - 1; // Zurück zu 0-basiert
                timeTable.setRowSelectionInterval(firstEmptyRow, firstEmptyRow);
                timeTable.editCellAt(firstEmptyRow, EditableTable.COL_DESCRIPTION);
                Component editor = timeTable.getEditorComponent();
                if (editor != null) {
                    editor.requestFocusInWindow();
                }
            }
            return;
        }

        // Bestätigung für neuen Abschluss
        int option = JOptionPane.showConfirmDialog(
                this,
                "Möchten Sie den Tag abschließen?\n\n" +
                        "Dies konsolidiert alle Einträge und verhindert weitere Änderungen.",
                "Tag abschließen",
                JOptionPane.YES_NO_OPTION
        );

        if (option == JOptionPane.YES_OPTION) {
            consolidateEntries();
            tabbedPane.setSelectedIndex(1); // Wechsel zu Abschluss-Tab
        }
    }

    /**
     * Hebt den Tagesabschluss auf und ermöglicht erneute Bearbeitung
     */
    private void removeDayCompletion() {
        try {
            // Lösche konsolidierte Einträge
            boolean success = dao.deleteConsolidatedEntriesByDate(LocalDate.now());

            if (success) {
                isDayCompleted = false;
                updateCompleteButton();
                updateTableEditability();

                // Leere konsolidierte Tabelle
                updateConsolidatedTable(new ArrayList<>());

                // Wechsel zurück zur Zeiterfassung
                tabbedPane.setSelectedIndex(0);

                JOptionPane.showMessageDialog(this,
                        "Tagesabschluss wurde aufgehoben.\n" +
                                "Sie können nun wieder Änderungen vornehmen.",
                        "Abschluss aufgehoben",
                        JOptionPane.INFORMATION_MESSAGE);

                System.out.println("Tagesabschluss aufgehoben");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Aufheben des Tagesabschlusses!",
                        "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Aufheben des Tagesabschlusses: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Unerwarteter Fehler beim Aufheben des Abschlusses:\n" + e.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void consolidateEntries() {
        List<TimeEntry> entries = dao.getTimeEntriesByDate(LocalDate.now());
        List<ConsolidatedEntry> consolidated = ConsolidatedEntry.consolidateTimeEntries(entries);

        // Alte konsolidierte Einträge löschen
        dao.deleteConsolidatedEntriesByDate(LocalDate.now());

        // Neue konsolidierte Einträge speichern
        for (ConsolidatedEntry entry : consolidated) {
            dao.insertConsolidatedEntry(entry);
        }

        // Status auf abgeschlossen setzen
        isDayCompleted = true;
        updateCompleteButton();
        updateTableEditability();

        // Konsolidierte Tabelle aktualisieren
        updateConsolidatedTable(consolidated);

        JOptionPane.showMessageDialog(this,
                String.format("Tag erfolgreich abgeschlossen!\n%d Einträge konsolidiert.\n\n" +
                                "Zeiteinträge können nun nicht mehr bearbeitet werden.",
                        consolidated.size()),
                "Abschluss erfolgreich",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Aktualisiert den Text und die Farbe des Complete-Buttons
     */
    private void updateCompleteButton() {
        if (isDayCompleted) {
            completeButton.setText("Abschluss aufheben");
            completeButton.setBackground(new Color(231, 76, 60)); // Rot für Aufheben
            completeButton.setToolTipText("Tagesabschluss aufheben und erneut bearbeiten");
            completeButton.setEnabled(true);
        } else {
            completeButton.setText("Tag abschließen");
            completeButton.setBackground(COMPLETE_COLOR); // Original gelb
            
            // Debug-Logging
            System.out.println("Status für Tagesabschluss-Button:");
            System.out.println("- isRunning: " + isRunning);
            System.out.println("- startStopButton.isEnabled(): " + startStopButton.isEnabled());
            System.out.println("- hasEmptyDescriptions: " + timeTable.hasEmptyDescriptions());
            
            // Button nur aktivieren wenn:
            // 1. Keine laufende Aktivität
            // 2. Keine leeren Beschreibungen
            boolean canComplete = !isRunning && !timeTable.hasEmptyDescriptions();
            
            System.out.println("- canComplete: " + canComplete);
            
            completeButton.setEnabled(canComplete);
            
            if (canComplete) {
                completeButton.setToolTipText("Tag abschließen und konsolidieren");
            } else if (isRunning) {
                completeButton.setToolTipText("Tag kann nicht abgeschlossen werden, solange eine Aktivität läuft");
            } else if (timeTable.hasEmptyDescriptions()) {
                List<Integer> emptyRows = timeTable.getRowsWithEmptyDescriptions();
                String rowList = emptyRows.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                completeButton.setToolTipText("Tag kann nicht abgeschlossen werden - Fehlende Beschreibungen in Zeile(n): " + rowList);
            }
        }
    }

    /**
     * Aktualisiert die Editierbarkeit der Tabelle basierend auf dem Abschluss-Status
     */
    private void updateTableEditability() {
        // Debug-Logging
        System.out.println("updateTableEditability:");
        System.out.println("- isDayCompleted: " + isDayCompleted);
        System.out.println("- isRunning: " + isRunning);
        
        if (isDayCompleted) {
            // Tag ist abgeschlossen - keine neuen Aktivitäten möglich
            startStopButton.setEnabled(false);
            startStopButton.setToolTipText("Neue Aktivitäten nach Tagesabschluss nicht möglich");
            timeTable.setStartButtonActive(false);
        } else if (isRunning) {
            // Aktivität läuft - nur Stoppen möglich
            startStopButton.setEnabled(true);
            startStopButton.setToolTipText("Aktuelle Aktivität stoppen");
            timeTable.setStartButtonActive(false);
        } else {
            // Keine Aktivität läuft - neue Aktivität möglich
            startStopButton.setEnabled(true);
            startStopButton.setToolTipText("Neue Aktivität starten");
            timeTable.setStartButtonActive(true);
        }
        
        // Aktualisiere auch den Complete-Button
        updateCompleteButton();
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

        consolidatedTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames) {
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

    private void loadTodaysData() {
        try {
            System.out.println("Lade heutige Zeiteinträge...");
            List<TimeEntry> entries = dao.getTimeEntriesByDate(LocalDate.now());
            timeTable.setTimeEntries(entries);

            // Prüfe Tagesabschluss-Status
            List<ConsolidatedEntry> consolidated = dao.getConsolidatedEntriesByDate(LocalDate.now());
            isDayCompleted = !consolidated.isEmpty();

            // UI entsprechend aktualisieren
            updateCompleteButton();
            updateTableEditability();

            // Prüfe auf laufende Aktivität (nur wenn Tag nicht abgeschlossen)
            currentActivity = null;
            isRunning = false;

            if (!isDayCompleted) {
                // Suche nach aktiver (unvollständiger) Aktivität
                for (TimeEntry entry : entries) {
                    if (entry.getEndTime() == null) {
                        currentActivity = entry;
                        isRunning = true;
                        break;
                    }
                }
            }

            updateStartStopButton();

            if (isRunning && currentActivity != null) {
                updateStatus("Läuft: " + currentActivity.getDescription());
            } else if (isDayCompleted) {
                updateStatus("Tag abgeschlossen");
            } else {
                updateStatus("Bereit");
            }

            // Lade konsolidierte Einträge falls vorhanden
            if (!consolidated.isEmpty()) {
                updateConsolidatedTable(consolidated);
            }

            // Teile der Tabelle den Abschluss-Status mit
            timeTable.setDayCompleted(isDayCompleted);

            System.out.println("Heutige Daten geladen: " + entries.size() + " Einträge" +
                    (isDayCompleted ? " (Tag abgeschlossen)" : ""));

        } catch (Exception e) {
            System.err.println("Fehler beim Laden der heutigen Daten: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        long totalMinutes = dao.getTotalWorkMinutesToday() + dao.getTotalBreakMinutesToday();
        long netMinutes = dao.getTotalWorkMinutesToday();
        long breakMinutes = dao.getTotalBreakMinutesToday();

        totalTimeLabel.setText("Gesamtzeit: " + TimeFormatter.formatDuration(totalMinutes));
        netTimeLabel.setText("Nettozeit: " + TimeFormatter.formatDuration(netMinutes));
        breakTimeLabel.setText("Pausen: " + TimeFormatter.formatDuration(breakMinutes));
    }

    private void updateStartStopButton() {
        if (isRunning) {
            startStopButton.setText("⏹ Stoppen");
            startStopButton.setBackground(RUNNING_COLOR);
            startStopButton.setToolTipText("Aktuelle Aktivität stoppen");
        } else {
            startStopButton.setText("▶ Starten");
            startStopButton.setBackground(STOPPED_COLOR);
            startStopButton.setToolTipText("Neue Aktivität starten");
        }
    }

    private void updateStatus(String status) {
        String timeInfo = TimeFormatter.formatCurrentTime();

        // Füge Informationen über verbleibende Zeit hinzu wenn Aktivität läuft
        if (isRunning && currentActivity != null) {
            LocalTime startTime = currentActivity.getStartTime();
            LocalTime currentTime = LocalTime.now();
            long durationMinutes = TimeFormatter.calculateDurationMinutes(startTime, currentTime);
            long remainingMinutes = 120 - durationMinutes; // 2 Stunden = 120 Minuten

            if (remainingMinutes > 0) {
                timeInfo += String.format(" | Verbleibend: %s", TimeFormatter.formatDuration(remainingMinutes));
            } else {
                timeInfo += " | ⚠️ 2h-Grenze überschritten!";
            }
        }

        statusLabel.setText(status + " - " + timeInfo);

        // Farbe der Statusleiste anpassen je nach verbleibender Zeit
        if (isRunning && currentActivity != null) {
            LocalTime startTime = currentActivity.getStartTime();
            LocalTime currentTime = LocalTime.now();
            long durationMinutes = TimeFormatter.calculateDurationMinutes(startTime, currentTime);

            if (durationMinutes >= 120) {
                statusLabel.setForeground(Color.RED); // Rot wenn überschritten
            } else if (durationMinutes >= 110) {
                statusLabel.setForeground(new Color(255, 140, 0)); // Orange als Warnung
            } else if (durationMinutes >= 100) {
                statusLabel.setForeground(new Color(255, 200, 0)); // Gelb als Vorstufe
            } else {
                statusLabel.setForeground(Color.GRAY); // Normal
            }
        } else {
            statusLabel.setForeground(Color.GRAY); // Normal wenn nicht aktiv
        }
    }

    private void startClockTimer() {
        clockTimer = new Timer(30000, e -> { // Update alle 30 Sekunden
            if (isRunning) {
                updateStatus("Läuft: " + (currentActivity != null ? currentActivity.getDescription() : ""));
                updateStatistics();
            }
        });
        clockTimer.start();

        // Aktivitätsüberwachungs-Timer starten
        startActivityMonitorTimer();
    }

    /**
     * Startet den Timer zur Überwachung der Aktivitätsdauer
     */
    private void startActivityMonitorTimer() {
        // Timer der alle 60 Sekunden prüft ob aktive Aktivität 2 Stunden überschreitet
        activityMonitorTimer = new Timer(60000, e -> checkActivityDuration());
        activityMonitorTimer.start();
    }

    /**
     * Prüft ob die aktuelle Aktivität 2 Stunden überschreitet
     */
    private void checkActivityDuration() {
        if (!isRunning || currentActivity == null) {
            return;
        }

        LocalTime startTime = currentActivity.getStartTime();
        LocalTime currentTime = LocalTime.now();
        long durationMinutes = TimeFormatter.calculateDurationMinutes(startTime, currentTime);

        // Prüfe ob 2 Stunden überschritten wurden
        if (durationMinutes >= 120) { // 2 Stunden = 120 Minuten
            handleActivityTimeLimit(durationMinutes);
        } else if (durationMinutes >= 110) { // 10 Minuten vor Ablauf warnen
            showPreWarning(120 - durationMinutes);
        }
    }

    /**
     * Zeigt eine Vorwarnung an wenn die 2-Stunden-Grenze bald erreicht wird
     */
    private void showPreWarning(long remainingMinutes) {
        String message = String.format(
                "Die aktuelle Aktivität läuft seit fast 2 Stunden.\n" +
                        "Sie wird in %d Minuten automatisch beendet.\n\n" +
                        "Aktivität: %s\n" +
                        "Startzeit: %s",
                remainingMinutes,
                currentActivity.getDescription(),
                currentActivity.getStartTimeFormatted()
        );

        // Nicht-blockierender Hinweis
        SwingUtilities.invokeLater(() -> {
            JOptionPane optionPane = new JOptionPane(
                    message,
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,
                    new Object[]{"OK", "Jetzt beenden"},
                    "OK"
            );

            JDialog dialog = optionPane.createDialog(this, "2-Stunden-Warnung");
            dialog.setAlwaysOnTop(true);
            dialog.setModal(false); // Nicht-modal für bessere Usability
            dialog.setVisible(true);

            // Prüfe Benutzerantwort
            Timer responseTimer = new Timer(100, evt -> {
                Object selectedValue = optionPane.getValue();
                if (selectedValue != null) {
                    if ("Jetzt beenden".equals(selectedValue)) {
                        stopCurrentActivity();
                    }
                    dialog.dispose();
                    ((Timer) evt.getSource()).stop();
                }
            });
            responseTimer.start();
        });
    }

    /**
     * Behandelt das Überschreiten der 2-Stunden-Grenze
     */
    private void handleActivityTimeLimit(long actualDuration) {
        String message = String.format(
                "Die 2-Stunden-Grenze wurde überschritten!\n\n" +
                        "Aktivität: %s\n" +
                        "Startzeit: %s\n" +
                        "Aktuelle Dauer: %s\n\n" +
                        "Die Aktivität wird jetzt automatisch beendet.",
                currentActivity.getDescription(),
                currentActivity.getStartTimeFormatted(),
                TimeFormatter.formatDuration(actualDuration)
        );

        // Zeige Warnung
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    this,
                    message,
                    "2-Stunden-Grenze überschritten",
                    JOptionPane.WARNING_MESSAGE
            );
        });

        // Aktivität automatisch beenden (auf genau 2 Stunden begrenzen)
        LocalTime endTime = currentActivity.getStartTime().plusMinutes(120);
        currentActivity.setEndTime(endTime);

        // In Datenbank speichern
        boolean success = dao.updateTimeEntry(currentActivity);
        if (success) {
            System.out.println("Aktivität automatisch nach 2 Stunden beendet: " +
                    currentActivity.getDescription());

            // Prüfe ob eine neue Aktivität gestartet werden soll
            offerNewActivity();

            // Status zurücksetzen
            isRunning = false;
            currentActivity = null;
            updateStartStopButton();
            updateStatus("Automatisch gestoppt nach 2 Stunden");

            // Tabelle aktualisieren
            SwingUtilities.invokeLater(() -> {
                loadTodaysData();
                updateStatistics();
            });
        } else {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim automatischen Beenden der Aktivität!",
                    "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Bietet an, eine neue Aktivität zu starten nach der 2-Stunden-Beendigung
     */
    private void offerNewActivity() {
        String message = "Die Aktivität wurde nach 2 Stunden automatisch beendet.\n\n" +
                "Möchten Sie eine neue Aktivität starten?";

        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Neue Aktivität starten?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                // Neue Aktivität starten (wie beim normalen Start-Button)
                startNewActivity();
            }
        });
    }

    private String createWindowTitle() {
        return "Zeiterfassung - " + TimeFormatter.formatDateForTitle(LocalDate.now());
    }

    private void showOpacityDialog() {
        float currentOpacity = alwaysOnTopHandler.getOpacity();
        String input = JOptionPane.showInputDialog(
                this,
                "Transparenz eingeben (10-100%):",
                Math.round(currentOpacity * 100)
        );

        if (input != null) {
            try {
                float opacity = Float.parseFloat(input) / 100f;
                alwaysOnTopHandler.setOpacity(opacity);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Ungültiger Wert. Bitte eine Zahl zwischen 10 und 100 eingeben.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Daten exportieren");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV-Dateien", "csv"));
        fileChooser.setSelectedFile(new java.io.File("zeiterfassung_" + LocalDate.now() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                exportToCSV(fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(this,
                        "Daten erfolgreich exportiert!",
                        "Export erfolgreich", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Exportieren: " + e.getMessage(),
                        "Export-Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportToCSV(java.io.File file) throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
            // Header
            writer.println("Datum,Startzeit,Endzeit,Dauer,Beschreibung,Pause");

            // Daten
            List<TimeEntry> entries = dao.getTimeEntriesByDate(LocalDate.now());
            for (TimeEntry entry : entries) {
                writer.printf("%s,%s,%s,%s,\"%s\",%s%n",
                        TimeFormatter.formatForExport(entry.getDate()),
                        TimeFormatter.formatForExport(entry.getStartTime()),
                        entry.getEndTime() != null ? TimeFormatter.formatForExport(entry.getEndTime()) : "",
                        entry.getDurationFormatted(),
                        entry.getDescription().replace("\"", "\"\""), // CSV-Escaping
                        entry.isBreak() ? "Ja" : "Nein"
                );
            }
        }
    }

    /**
     * Zeigt die historische Zeiterfassungsansicht
     */
    private void showHistoricalView() {
        try {
            HistoricalViewDialog dialog = new HistoricalViewDialog(this, dao);

            // Setze auf gestern als Standard (interessanter als heute)
            LocalDate yesterday = LocalDate.now().minusDays(1);
            dialog.setDate(yesterday);

            dialog.setVisible(true);

        } catch (Exception e) {
            System.err.println("Fehler beim Öffnen der historischen Ansicht: " + e.getMessage());
            e.printStackTrace();

            JOptionPane.showMessageDialog(this,
                    "Fehler beim Öffnen der historischen Ansicht:\n" + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAboutDialog() {
        String message = """
            <html>
            <center>
            <h2>Zeiterfassungsapplikation</h2>
            <p>Version 1.0</p>
            <br>
            <p>Eine einfache und effektive Lösung für die Zeiterfassung</p>
            <br>
            <p><b>Features:</b></p>
            <ul>
            <li>Always-on-top Funktionalität</li>
            <li>Automatische Lücken-Vermeidung</li>
            <li>AutoComplete für Beschreibungen</li>
            <li>Tageskonsolidierung</li>
            <li>CSV-Export</li>
            <li>Historische Zeiterfassung</li>
            </ul>
            <br>
            <p><b>Keyboard Shortcuts:</b></p>
            <ul>
            <li>Ctrl+T: Always-on-top umschalten</li>
            <li>Ctrl++: Transparenz erhöhen</li>
            <li>Ctrl+-: Transparenz verringern</li>
            </ul>
            </center>
            </html>
            """;

        JOptionPane.showMessageDialog(this, message, "Über Zeiterfassung", JOptionPane.INFORMATION_MESSAGE);
    }

    private void closeApplication() {
        // Timer stoppen
        if (clockTimer != null) {
            clockTimer.stop();
        }

        if (activityMonitorTimer != null) {
            activityMonitorTimer.stop();
        }

        // Always-on-top Handler cleanup
        if (alwaysOnTopHandler != null) {
            alwaysOnTopHandler.cleanup();
        }

        // Datenbank schließen
        DatabaseManager.getInstance().close();

        System.exit(0);
    }

    // Main-Methode für Testing
    public static void main(String[] args) {
        // System Properties für bessere Java 17 Kompatibilität
        System.setProperty("java.awt.headless", "false");

        // Look and Feel setzen - KORREKTE SYNTAX für Java 17
        try {
            // System Look and Feel über String-Name setzen
            String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(systemLookAndFeel);
            System.out.println("System Look and Feel gesetzt: " + systemLookAndFeel);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("Konnte System Look and Feel nicht setzen: " + e.getMessage());
            try {
                // Fallback auf Cross-Platform Look and Feel
                String crossPlatformLookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
                UIManager.setLookAndFeel(crossPlatformLookAndFeel);
                System.out.println("Cross-Platform Look and Feel gesetzt als Fallback: " + crossPlatformLookAndFeel);
            } catch (Exception e2) {
                System.err.println("Konnte auch Cross-Platform Look and Feel nicht setzen: " + e2.getMessage());
                // Alternative: Explizit Nimbus setzen
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                    System.out.println("Nimbus Look and Feel gesetzt als Fallback");
                } catch (Exception e3) {
                    System.err.println("Verwende Standard Look and Feel");
                }
            }
        }

        // GUI im Event Dispatch Thread starten
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow();
                window.setVisible(true);
                System.out.println("Zeiterfassungsapplikation erfolgreich gestartet");
            } catch (Exception e) {
                System.err.println("Fehler beim Starten der Anwendung:");
                e.printStackTrace();

                JOptionPane.showMessageDialog(null,
                        "Fehler beim Starten der Anwendung:\n" + e.getMessage() +
                                "\n\nDetails siehe Konsole.",
                        "Startfehler", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /**
     * Setzt den Fokus auf den Start/Stop-Button
     */
    public void focusOnStopButton() {
        SwingUtilities.invokeLater(() -> {
            // Stelle sicher dass das Fenster im Vordergrund ist
            if (!isActive()) {
                toFront();
                requestFocus();
            }

            // Setze Fokus auf den Button
            startStopButton.requestFocusInWindow();

            // Zusätzliche Sicherstellung für macOS
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                Timer focusTimer = new Timer(50, e -> {
                    startStopButton.requestFocusInWindow();
                });
                focusTimer.setRepeats(false);
                focusTimer.start();
            }

            System.out.println("Fokus auf Start/Stop-Button angefordert");
        });
    }
}