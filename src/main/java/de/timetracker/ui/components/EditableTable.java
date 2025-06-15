package de.timetracker.ui.components;

import de.timetracker.model.TimeEntry;
import de.timetracker.ui.MainWindow;
import de.timetracker.utils.TimeFormatter;
import de.timetracker.database.TimeEntryDAO;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Editierbare Tabelle für Zeiteinträge mit automatischer 2-Stunden-Blockierung und Pausenverwaltung
 */
public class EditableTable extends JTable {
    private final TimeEntryTableModel tableModel;
    private final TimeEntryDAO dao;
    private final List<TimeEntryChangeListener> changeListeners;
    private boolean isDayCompleted = false; // Flag für Tagesabschluss-Status
    private boolean isStartButtonActive = true; // Flag für Start-Button-Status

    // Spalten-Indizes
    public static final int COL_START_TIME = 0;
    public static final int COL_END_TIME = 1;
    public static final int COL_DURATION = 2;
    public static final int COL_DESCRIPTION = 3;
    public static final int COL_DELETE = 4;

    // Konstanten
    private static final int MAX_DURATION_MINUTES = 120; // 2 Stunden

    public EditableTable(TimeEntryDAO dao) {
        this.dao = dao;
        this.tableModel = new TimeEntryTableModel();
        this.changeListeners = new ArrayList<>();

        setModel(tableModel);
        initializeTable();
        setupCellEditors();
        setupCellRenderers();
    }

    private void initializeTable() {
        // Tabellen-Eigenschaften
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(28);
        setGridColor(new Color(230, 230, 230));
        setShowGrid(true);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // Header-Font
        getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        getTableHeader().setBackground(new Color(240, 240, 240));
        getTableHeader().setOpaque(true);

        // Spaltenbreiten setzen
        setupColumnWidths();

        // Auto-Resize verhalten
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Zeilen-Highlighting
        setSelectionBackground(new Color(220, 240, 255));
        setSelectionForeground(Color.BLACK);
    }

    private void setupColumnWidths() {
        TableColumn column;

        // Startzeit
        column = getColumnModel().getColumn(COL_START_TIME);
        column.setPreferredWidth(80);
        column.setMinWidth(70);
        column.setMaxWidth(100);

        // Endzeit
        column = getColumnModel().getColumn(COL_END_TIME);
        column.setPreferredWidth(80);
        column.setMinWidth(70);
        column.setMaxWidth(100);

        // Dauer
        column = getColumnModel().getColumn(COL_DURATION);
        column.setPreferredWidth(80);
        column.setMinWidth(70);
        column.setMaxWidth(100);

        // Beschreibung
        column = getColumnModel().getColumn(COL_DESCRIPTION);
        column.setPreferredWidth(200);
        column.setMinWidth(150);

        // Löschen-Button
        column = getColumnModel().getColumn(COL_DELETE);
        column.setPreferredWidth(60);
        column.setMinWidth(50);
        column.setMaxWidth(70);
    }

    private void setupCellEditors() {
        // Zeit-Editor für Start und Endzeit
        TimeEditor timeEditor = new TimeEditor();
        getColumnModel().getColumn(COL_START_TIME).setCellEditor(timeEditor);
        getColumnModel().getColumn(COL_END_TIME).setCellEditor(timeEditor);

        // Description-Editor mit AutoComplete
        DescriptionEditor descriptionEditor = new DescriptionEditor();
        getColumnModel().getColumn(COL_DESCRIPTION).setCellEditor(descriptionEditor);

        // Button-Editor für Löschen
        ButtonEditor deleteEditor = new ButtonEditor();
        getColumnModel().getColumn(COL_DELETE).setCellEditor(deleteEditor);
    }

    private void setupCellRenderers() {
        // Zeit-Renderer für Zeit-Spalten
        TimeRenderer timeRenderer = new TimeRenderer();
        getColumnModel().getColumn(COL_START_TIME).setCellRenderer(timeRenderer);
        getColumnModel().getColumn(COL_END_TIME).setCellRenderer(timeRenderer);
        getColumnModel().getColumn(COL_DURATION).setCellRenderer(timeRenderer);

        // Beschreibung-Renderer
        DescriptionRenderer descriptionRenderer = new DescriptionRenderer();
        getColumnModel().getColumn(COL_DESCRIPTION).setCellRenderer(descriptionRenderer);

        // Button-Renderer
        ButtonRenderer buttonRenderer = new ButtonRenderer();
        getColumnModel().getColumn(COL_DELETE).setCellRenderer(buttonRenderer);
    }

    // Public Methods für externe Kontrolle

    public void setTimeEntries(List<TimeEntry> entries) {
        tableModel.setTimeEntries(entries);

        // Nach dem Setzen der Einträge: Prüfe und fülle alle Lücken
        SwingUtilities.invokeLater(() -> {
            checkAndFillAllGaps();
        });
    }

    public List<TimeEntry> getTimeEntries() {
        return tableModel.getTimeEntries();
    }

    public void addTimeEntry(TimeEntry entry) {
        tableModel.addTimeEntry(entry);
    }

    public void removeTimeEntry(int row) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            // Prüfe Tagesabschluss-Status
            if (isDayCompleted) {
                JOptionPane.showMessageDialog(this,
                        "Der Tag ist bereits abgeschlossen.\n" +
                                "Löschen von Einträgen ist nicht mehr möglich.\n\n" +
                                "Heben Sie den Tagesabschluss auf, um Änderungen vorzunehmen.",
                        "Tag abgeschlossen",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            TimeEntry entry = tableModel.getTimeEntry(row);

            // Prüfe ob Eintrag laufend ist - laufende Einträge können NICHT gelöscht werden
            if (entry.getEndTime() == null) {
                JOptionPane.showMessageDialog(this,
                        "Laufende Aktivitäten können nicht gelöscht werden.\nStoppen Sie die Aktivität zuerst.",
                        "Löschen nicht möglich", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Eintrag aus Datenbank löschen
            if (entry.getId() > 0) {
                dao.deleteTimeEntry(entry.getId());
            }

            // Eintrag aus Model entfernen
            tableModel.removeTimeEntry(row);

            // Nach dem Löschen: Zeiten anpassen und 2-Stunden-Regel durchsetzen
            adjustTimesAfterDeletion(row);

            notifyChangeListeners();
        }
    }

    /**
     * Setzt den Tagesabschluss-Status
     */
    public void setDayCompleted(boolean dayCompleted) {
        this.isDayCompleted = dayCompleted;
        tableModel.fireTableDataChanged();
    }

    /**
     * Gibt den Tagesabschluss-Status zurück
     */
    public boolean isDayCompleted() {
        return isDayCompleted;
    }

    /**
     * Passt Zeiten nach dem Löschen einer Zeile an und fügt bei Bedarf Pausen ein
     */
    private void adjustTimesAfterDeletion(int deletedRow) {
        List<TimeEntry> entries = tableModel.getTimeEntries();
        if (entries.isEmpty() || deletedRow >= entries.size()) return;

        System.out.println("Anpassung nach Löschung von Zeile " + (deletedRow + 1));

        // Prüfe und fülle Lücken ab der gelöschten Position
        fillGapsFromPosition(deletedRow);

        // Tabelle aktualisieren
        SwingUtilities.invokeLater(() -> {
            tableModel.fireTableDataChanged();
            notifyChangeListeners();
        });
    }

    /**
     * Füllt Lücken ab einer bestimmten Position mit Pausen-Zeilen
     */
    private void fillGapsFromPosition(int startPosition) {
        List<TimeEntry> entries = tableModel.getTimeEntries();

        for (int i = startPosition; i < entries.size(); i++) {
            TimeEntry currentEntry = entries.get(i);

            if (i > 0) {
                TimeEntry previousEntry = entries.get(i - 1);

                if (previousEntry.getEndTime() != null && currentEntry.getStartTime() != null) {
                    LocalTime expectedStartTime = previousEntry.getEndTime();
                    LocalTime actualStartTime = currentEntry.getStartTime();

                    // Prüfe ob eine Lücke existiert
                    if (!expectedStartTime.equals(actualStartTime)) {
                        long gapMinutes = TimeFormatter.calculateDurationMinutes(expectedStartTime, actualStartTime);

                        if (gapMinutes > 0) {
                            System.out.println("Lücke gefunden zwischen " +
                                    TimeFormatter.formatTime(expectedStartTime) + " und " +
                                    TimeFormatter.formatTime(actualStartTime) +
                                    " (" + gapMinutes + " Minuten)");

                            // Fülle die Lücke mit Pausen-Zeilen
                            fillGapWithPauses(i, expectedStartTime, actualStartTime, gapMinutes);

                            // Nach dem Einfügen: Liste neu laden und Schleife beenden
                            entries = tableModel.getTimeEntries();
                            break;
                        } else if (gapMinutes < 0) {
                            // Überlappung - passe aktuelle Startzeit an
                            System.out.println("Überlappung korrigiert: " +
                                    TimeFormatter.formatTime(actualStartTime) + " -> " +
                                    TimeFormatter.formatTime(expectedStartTime));

                            currentEntry.setStartTime(expectedStartTime);

                            // Endzeit entsprechend anpassen um Dauer zu erhalten
                            if (currentEntry.getEndTime() != null) {
                                long originalDuration = TimeFormatter.calculateDurationMinutes(actualStartTime, currentEntry.getEndTime());
                                LocalTime newEndTime = expectedStartTime.plusMinutes(originalDuration);
                                currentEntry.setEndTime(newEndTime);
                            }

                            updateEntryInDatabase(currentEntry);
                        }
                    }
                }
            }
        }
    }

    /**
     * Füllt eine Lücke zwischen zwei Zeiten mit Pausen-Zeilen auf
     */
    private void fillGapWithPauses(int insertPosition, LocalTime startTime, LocalTime endTime, long gapMinutes) {
        List<TimeEntry> entries = tableModel.getTimeEntries();
        TimeEntry referenceEntry = entries.get(insertPosition);

        LocalTime currentTime = startTime;
        long remainingMinutes = gapMinutes;
        int insertIndex = insertPosition;

        while (remainingMinutes > 0) {
            // Berechne Pause-Dauer (maximal 2 Stunden)
            long pauseDuration = Math.min(remainingMinutes, MAX_DURATION_MINUTES);
            LocalTime pauseEndTime = currentTime.plusMinutes(pauseDuration);

            // Erstelle Pause-Eintrag
            TimeEntry pauseEntry = new TimeEntry();
            pauseEntry.setDate(referenceEntry.getDate());
            pauseEntry.setStartTime(currentTime);
            pauseEntry.setEndTime(pauseEndTime);
            pauseEntry.setDescription("Pause (automatisch eingefügt)");
            pauseEntry.setBreak(true);

            // In Datenbank speichern
            boolean success = dao.insertTimeEntry(pauseEntry);
            if (success) {
                // In Model einfügen
                entries.add(insertIndex, pauseEntry);
                insertIndex++;

                System.out.println("Pause eingefügt: " +
                        TimeFormatter.formatTime(currentTime) + " - " +
                        TimeFormatter.formatTime(pauseEndTime) +
                        " (" + pauseDuration + " Minuten)");
            } else {
                System.err.println("Fehler beim Speichern der Pause-Zeile");
                break;
            }

            currentTime = pauseEndTime;
            remainingMinutes -= pauseDuration;
        }

        // Aktualisiere die Position der ursprünglichen Zeile
        if (insertIndex < entries.size()) {
            TimeEntry originalEntry = entries.get(insertIndex);
            if (!originalEntry.getStartTime().equals(endTime)) {
                originalEntry.setStartTime(endTime);
                updateEntryInDatabase(originalEntry);
            }
        }
    }

    /**
     * Prüft alle Zeiteinträge und füllt vorhandene Lücken mit Pausen
     */
    private void checkAndFillAllGaps() {
        List<TimeEntry> entries = tableModel.getTimeEntries();
        if (entries.size() < 2) return;

        boolean gapsFound = false;

        for (int i = 1; i < entries.size(); i++) {
            TimeEntry previousEntry = entries.get(i - 1);
            TimeEntry currentEntry = entries.get(i);

            if (previousEntry.getEndTime() != null && currentEntry.getStartTime() != null) {
                LocalTime expectedStartTime = previousEntry.getEndTime();
                LocalTime actualStartTime = currentEntry.getStartTime();

                if (!expectedStartTime.equals(actualStartTime)) {
                    long gapMinutes = TimeFormatter.calculateDurationMinutes(expectedStartTime, actualStartTime);

                    if (gapMinutes > 0) {
                        System.out.println("Existierende Lücke gefunden zwischen Zeile " + i + " und " + (i + 1) +
                                ": " + gapMinutes + " Minuten");

                        fillGapWithPauses(i, expectedStartTime, actualStartTime, gapMinutes);
                        gapsFound = true;
                        break; // Nach dem ersten Gap: Liste neu laden und erneut prüfen
                    }
                }
            }
        }

        if (gapsFound) {
            // Tabelle aktualisieren und erneut prüfen
            SwingUtilities.invokeLater(() -> {
                tableModel.fireTableDataChanged();
                notifyChangeListeners();

                // Rekursive Prüfung für weitere Lücken
                Timer timer = new Timer(100, e -> checkAndFillAllGaps());
                timer.setRepeats(false);
                timer.start();
            });
        }
    }

    /**
     * Teilt einen zu langen Eintrag auf und fügt bei Bedarf Pausen ein
     */
    private void splitLongEntry(int entryIndex, LocalTime startTime, LocalTime endTime, long totalDuration) {
        List<TimeEntry> entries = tableModel.getTimeEntries();
        TimeEntry originalEntry = entries.get(entryIndex);

        System.out.println("Teile zu langen Eintrag auf: " + totalDuration + " Minuten");

        // Ursprünglichen Eintrag auf 2 Stunden begrenzen
        LocalTime newEndTime = startTime.plusMinutes(MAX_DURATION_MINUTES);
        originalEntry.setStartTime(startTime);
        originalEntry.setEndTime(newEndTime);
        updateEntryInDatabase(originalEntry);

        // Berechne verbleibende Zeit
        long remainingMinutes = totalDuration - MAX_DURATION_MINUTES;
        LocalTime currentTime = newEndTime;

        // Füge Pause ein wenn nächste Zeile auch 2 Stunden hat
        boolean needsPause = false;
        if (entryIndex + 1 < entries.size()) {
            TimeEntry nextEntry = entries.get(entryIndex + 1);
            if (nextEntry.getEndTime() != null) {
                long nextEntryDuration = nextEntry.getDurationMinutes();
                if (nextEntryDuration >= MAX_DURATION_MINUTES) {
                    needsPause = true;
                }
            }
        }

        if (needsPause && remainingMinutes > 0) {
            // Füge Pause für verbleibende Zeit ein
            TimeEntry pauseEntry = new TimeEntry();
            pauseEntry.setDate(originalEntry.getDate());
            pauseEntry.setStartTime(currentTime);
            pauseEntry.setEndTime(currentTime.plusMinutes(remainingMinutes));
            pauseEntry.setDescription("Pause (automatisch)");
            pauseEntry.setBreak(true);

            // In Datenbank speichern
            dao.insertTimeEntry(pauseEntry);

            // In Model einfügen
            entries.add(entryIndex + 1, pauseEntry);

            System.out.println("Pause eingefügt: " + pauseEntry.getStartTimeFormatted() +
                    " - " + pauseEntry.getEndTimeFormatted() + " (" + remainingMinutes + " min)");
        }

        // Alle nachfolgenden Einträge neu positionieren
        adjustSubsequentTimesFromIndex(entryIndex + (needsPause ? 2 : 1));
    }

    /**
     * Passt nachfolgende Zeiten ab einem bestimmten Index an
     */
    private void adjustSubsequentTimesFromIndex(int startIndex) {
        List<TimeEntry> entries = tableModel.getTimeEntries();

        for (int i = startIndex; i < entries.size(); i++) {
            if (i == 0) continue;

            TimeEntry currentEntry = entries.get(i);
            TimeEntry previousEntry = entries.get(i - 1);

            if (previousEntry.getEndTime() != null && currentEntry.getEndTime() != null) {
                LocalTime oldStartTime = currentEntry.getStartTime();
                LocalTime newStartTime = previousEntry.getEndTime();

                // Behalte ursprüngliche Dauer bei
                long originalDuration = currentEntry.getDurationMinutes();
                LocalTime newEndTime = newStartTime.plusMinutes(originalDuration);

                currentEntry.setStartTime(newStartTime);
                currentEntry.setEndTime(newEndTime);
                updateEntryInDatabase(currentEntry);

                System.out.println("Nachfolgende Zeile " + (i + 1) + " angepasst: " +
                        currentEntry.getStartTimeFormatted() + " - " +
                        currentEntry.getEndTimeFormatted());
            }
        }
    }

    private void updateEntryInDatabase(TimeEntry entry) {
        if (entry.getId() > 0) {
            boolean success = dao.updateTimeEntry(entry);
            if (!success) {
                System.err.println("Fehler beim Speichern von Eintrag " + entry.getId());
            }
        } else {
            dao.insertTimeEntry(entry);
        }
    }

    public void addChangeListener(TimeEntryChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(TimeEntryChangeListener listener) {
        changeListeners.remove(listener);
    }

    private void notifyChangeListeners() {
        for (TimeEntryChangeListener listener : changeListeners) {
            listener.timeEntriesChanged();
        }
    }

    // Interface für Change-Events
    public interface TimeEntryChangeListener {
        void timeEntriesChanged();
    }

    // Table Model
    private class TimeEntryTableModel extends AbstractTableModel {
        private final String[] columnNames = {
                "Startzeit", "Endzeit", "Dauer", "Beschreibung", ""
        };
        private List<TimeEntry> entries = new ArrayList<>();

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case COL_DELETE -> JButton.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (row >= entries.size()) return false;

            // Wenn Tag abgeschlossen ist: NICHTS ist editierbar
            if (isDayCompleted) {
                return false;
            }

            // Wenn Start-Button nicht aktiv ist: NICHTS ist editierbar
            if (!isStartButtonActive) {
                return false;
            }

            TimeEntry entry = entries.get(row);

            // Dauer-Spalte ist immer read-only
            if (column == COL_DURATION) {
                return false;
            }

            // Löschen-Button: nur editierbar wenn Eintrag NICHT laufend ist UND Tag nicht abgeschlossen
            if (column == COL_DELETE) {
                return entry.getEndTime() != null; // Nur abgeschlossene Einträge können gelöscht werden
            }

            // Bei laufenden Aktivitäten: nur Beschreibung editierbar
            if (entry.getEndTime() == null) { // Laufende Aktivität
                return column == COL_DESCRIPTION;
            }

            // Abgeschlossene Aktivitäten: Start-, Endzeit und Beschreibung editierbar
            return column == COL_START_TIME || column == COL_END_TIME || column == COL_DESCRIPTION;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row >= entries.size()) return null;

            TimeEntry entry = entries.get(row);
            return switch (column) {
                case COL_START_TIME -> entry.getStartTimeFormatted();
                case COL_END_TIME -> entry.getEndTimeFormatted();
                case COL_DURATION -> entry.getDurationFormatted();
                case COL_DESCRIPTION -> entry.getDescription();
                case COL_DELETE -> "✕";
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (row >= entries.size()) return;

            TimeEntry entry = entries.get(row);
            boolean changed = false;

            switch (column) {
                case COL_START_TIME:
                    if (entry.getEndTime() != null) {
                        LocalTime newStartTime = TimeFormatter.parseTimeSafe((String) value);
                        if (newStartTime != null && !newStartTime.equals(entry.getStartTime())) {
                            LocalTime originalEndTime = entry.getEndTime();
                            long newDuration = TimeFormatter.calculateDurationMinutes(newStartTime, originalEndTime);

                            if (newDuration > MAX_DURATION_MINUTES) {
                                // Zu lange - begrenzen und nach Pause suchen
                                handleLongDurationEdit(row, newStartTime, originalEndTime, newDuration);
                                return;
                            } else {
                                entry.setStartTime(newStartTime);
                                changed = true;
                                adjustSubsequentTimes(row);
                            }
                        }
                    }
                    break;

                case COL_END_TIME:
                    if (entry.getEndTime() != null) {
                        LocalTime newEndTime = TimeFormatter.parseTimeSafe((String) value);
                        if (newEndTime != null && !newEndTime.equals(entry.getEndTime())) {
                            long newDuration = TimeFormatter.calculateDurationMinutes(entry.getStartTime(), newEndTime);

                            if (newDuration > MAX_DURATION_MINUTES) {
                                // Zu lange - begrenzen und nach Pause suchen
                                handleLongDurationEdit(row, entry.getStartTime(), newEndTime, newDuration);
                                return;
                            } else {
                                entry.setEndTime(newEndTime);
                                changed = true;
                                adjustSubsequentTimes(row);
                            }
                        }
                    }
                    break;

                case COL_DESCRIPTION:
                    String description = (String) value;
                    if (description != null && !description.equals(entry.getDescription())) {
                        entry.setDescription(description);
                        changed = true;
                    }
                    break;
            }

            if (changed) {
                updateEntryInDatabase(entry);
                fireTableDataChanged();
                notifyChangeListeners();
            }
        }

        /**
         * Behandelt Bearbeitung die zu einer zu langen Dauer führt
         */
        private void handleLongDurationEdit(int row, LocalTime startTime, LocalTime endTime, long totalDuration) {
            TimeEntry entry = entries.get(row);

            // Eintrag auf 2 Stunden begrenzen
            LocalTime limitedEndTime = startTime.plusMinutes(MAX_DURATION_MINUTES);
            entry.setStartTime(startTime);
            entry.setEndTime(limitedEndTime);
            updateEntryInDatabase(entry);

            // Verbleibende Zeit berechnen
            long remainingMinutes = totalDuration - MAX_DURATION_MINUTES;

            // Prüfe ob nächste Zeile auch 2 Stunden hat
            boolean nextEntryIsFull = false;
            if (row + 1 < entries.size()) {
                TimeEntry nextEntry = entries.get(row + 1);
                if (nextEntry.getEndTime() != null) {
                    long nextDuration = nextEntry.getDurationMinutes();
                    if (nextDuration >= MAX_DURATION_MINUTES) {
                        nextEntryIsFull = true;
                    }
                }
            }

            // Füge Pause ein wenn nächste Zeile voll ist
            if (nextEntryIsFull && remainingMinutes > 0) {
                TimeEntry pauseEntry = new TimeEntry();
                pauseEntry.setDate(entry.getDate());
                pauseEntry.setStartTime(limitedEndTime);
                pauseEntry.setEndTime(limitedEndTime.plusMinutes(remainingMinutes));
                pauseEntry.setDescription("Pause (automatisch)");
                pauseEntry.setBreak(true);

                dao.insertTimeEntry(pauseEntry);
                entries.add(row + 1, pauseEntry);

                // Nachfolgende Zeiten anpassen
                adjustSubsequentTimesFromIndex(row + 2);

                JOptionPane.showMessageDialog(EditableTable.this,
                        String.format("Eintrag auf 2 Stunden begrenzt.\nPause von %d Minuten automatisch eingefügt.",
                                remainingMinutes),
                        "2-Stunden-Regel", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(EditableTable.this,
                        "Einträge sind auf maximal 2 Stunden begrenzt.\nEintrag wurde entsprechend angepasst.",
                        "2-Stunden-Regel", JOptionPane.INFORMATION_MESSAGE);
            }

            SwingUtilities.invokeLater(() -> {
                fireTableDataChanged();
                notifyChangeListeners();
            });
        }

        public void setTimeEntries(List<TimeEntry> entries) {
            this.entries = new ArrayList<>(entries);
            fireTableDataChanged();
        }

        public List<TimeEntry> getTimeEntries() {
            return new ArrayList<>(entries);
        }

        public void addTimeEntry(TimeEntry entry) {
            entries.add(entry);
            int row = entries.size() - 1;
            fireTableRowsInserted(row, row);
        }

        public void removeTimeEntry(int row) {
            if (row >= 0 && row < entries.size()) {
                entries.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        public TimeEntry getTimeEntry(int row) {
            return row >= 0 && row < entries.size() ? entries.get(row) : null;
        }

        private void adjustSubsequentTimes(int changedRow) {
            if (changedRow >= entries.size() - 1) return;

            TimeEntry changedEntry = entries.get(changedRow);
            if (changedEntry.getEndTime() == null) return;

            System.out.println("Anpassung der nachfolgenden Zeiten ab Zeile " + (changedRow + 1));

            // Alle nachfolgenden Einträge anpassen
            for (int i = changedRow + 1; i < entries.size(); i++) {
                TimeEntry currentEntry = entries.get(i);
                TimeEntry previousEntry = entries.get(i - 1);

                if (previousEntry.getEndTime() != null) {
                    LocalTime oldStartTime = currentEntry.getStartTime();
                    LocalTime newStartTime = previousEntry.getEndTime();

                    // Nur anpassen wenn sich die Zeit ändert
                    if (!newStartTime.equals(oldStartTime)) {
                        currentEntry.setStartTime(newStartTime);

                        // Endzeit proportional anpassen um Dauer zu erhalten
                        if (currentEntry.getEndTime() != null) {
                            long originalDuration = TimeFormatter.calculateDurationMinutes(
                                    oldStartTime, currentEntry.getEndTime());

                            // Neue Endzeit basierend auf alter Dauer
                            LocalTime newEndTime = newStartTime.plusMinutes(originalDuration);
                            currentEntry.setEndTime(newEndTime);

                            System.out.println("Zeile " + (i + 1) + ": " +
                                    oldStartTime + "->" + newStartTime +
                                    " (Dauer: " + originalDuration + " min)");
                        }

                        updateEntryInDatabase(currentEntry);
                    }
                }
            }

            // Tabelle aktualisieren um Änderungen anzuzeigen
            SwingUtilities.invokeLater(() -> {
                fireTableDataChanged();
                notifyChangeListeners();
            });
        }
    }

    // Custom Cell Editors (unverändert von vorheriger Version)

    private class TimeEditor extends DefaultCellEditor {
        private JTextField textField;

        public TimeEditor() {
            super(new JTextField());
            textField = (JTextField) getComponent();
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setOpaque(true);
            textField.setBackground(Color.WHITE);
            textField.setForeground(Color.BLACK);
        }

        @Override
        public boolean stopCellEditing() {
            String value = textField.getText().trim();
            if (!value.isEmpty() && !TimeFormatter.isValidTime(value)) {
                JOptionPane.showMessageDialog(EditableTable.this,
                        "Ungültiges Zeitformat. Verwenden Sie HH:mm (z.B. 09:30)",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            textField.setText(value != null ? value.toString() : "");
            textField.selectAll();
            return textField;
        }
    }

    private class DescriptionEditor extends DefaultCellEditor {
        private AutoCompleteTextField autoCompleteField;
        private EditableTable parentTable;
        private boolean enterPressed = false;

        public DescriptionEditor() {
            super(new JTextField());
            autoCompleteField = new AutoCompleteTextField(dao);
            parentTable = EditableTable.this;
            setClickCountToStart(1);

            autoCompleteField.setMaximumRowCount(10);

            autoCompleteField.addActionListener(e -> {
                enterPressed = true;
                stopCellEditing();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {

            enterPressed = false;
            String currentValue = value != null ? value.toString() : "";
            autoCompleteField.setText(currentValue);

            // Bereite AutoComplete für Tabellen-Editing vor
            autoCompleteField.prepareForTableEditing();

            // Lade Suggestions falls leer
            if (currentValue.isEmpty()) {
                autoCompleteField.refreshSuggestions();
            }

            return autoCompleteField;
        }

        @Override
        public Object getCellEditorValue() {
            return autoCompleteField.getText();
        }

        @Override
        public boolean stopCellEditing() {
            boolean result = super.stopCellEditing();

            if (result) {
                autoCompleteField.refreshSuggestions();

                if (enterPressed) {
                    SwingUtilities.invokeLater(() -> {
                        Container parent = parentTable.getParent();
                        while (parent != null && !(parent instanceof MainWindow)) {
                            parent = parent.getParent();
                        }
                        if (parent instanceof MainWindow mainWindow) {
                            mainWindow.focusOnStopButton();
                        }
                    });
                }
            }

            return result;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int editingRow;

        public ButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            setupButton();

            button.addActionListener(e -> {
                fireEditingStopped();
                removeTimeEntry(editingRow);
            });
        }

        private void setupButton() {
            button.setOpaque(true);
            button.setBorderPainted(true);
            button.setContentAreaFilled(true);
            button.setFocusPainted(false);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            editingRow = row;

            TimeEntry entry = tableModel.getTimeEntry(row);
            boolean canDelete = entry != null && entry.getEndTime() != null && !isDayCompleted;

            button.setText("✕");
            if (canDelete) {
                button.setBackground(new Color(255, 200, 200));
                button.setForeground(Color.RED.darker());
                button.setToolTipText("Eintrag löschen");
                button.setEnabled(true);
            } else {
                button.setBackground(new Color(240, 240, 240));
                button.setForeground(Color.GRAY);

                if (isDayCompleted) {
                    button.setToolTipText("Tag ist abgeschlossen - Löschen nicht möglich");
                } else {
                    button.setToolTipText("Laufende Aktivitäten können nicht gelöscht werden");
                }

                button.setEnabled(false);
            }

            button.setBorder(BorderFactory.createLoweredBevelBorder());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "✕";
        }
    }

    public void setStartButtonActive(boolean active) {
        this.isStartButtonActive = active;
        tableModel.fireTableDataChanged();
    }

    public boolean hasEmptyDescriptions() {
        return tableModel.getTimeEntries().stream()
                .anyMatch(entry -> entry.getDescription() == null || entry.getDescription().trim().isEmpty());
    }

    public List<Integer> getRowsWithEmptyDescriptions() {
        List<Integer> rows = new ArrayList<>();
        List<TimeEntry> entries = tableModel.getTimeEntries();
        
        for (int i = 0; i < entries.size(); i++) {
            TimeEntry entry = entries.get(i);
            if (entry.getDescription() == null || entry.getDescription().trim().isEmpty()) {
                rows.add(i + 1); // 1-basiert für Benutzerfreundlichkeit
            }
        }
        
        return rows;
    }
}