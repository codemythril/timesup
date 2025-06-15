package de.timetracker.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom TableCellRenderer für Zeit-Spalten in der Zeiterfassungstabelle
 */
public class TimeRenderer extends DefaultTableCellRenderer {

    // Spalten-Konstanten (müssen mit EditableTable übereinstimmen)
    private static final int COL_START_TIME = 0;
    private static final int COL_END_TIME = 1;
    private static final int COL_DURATION = 2;

    public TimeRenderer() {
        setHorizontalAlignment(CENTER);
        setOpaque(true); // Wichtig für Hintergrundfarbe auf macOS
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // Basis-Rendering
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Prüfe ob Zelle editierbar ist
        boolean cellEditable = table.isCellEditable(row, column);

        // Hintergrund- und Vordergrundfarben basierend auf Editierbarkeit
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else if (!cellEditable) {
            // Nicht editierbare Zellen grau hinterlegen
            setBackground(new Color(245, 245, 245));
            setForeground(Color.DARK_GRAY);
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }

        // Spezielle Formatierung basierend auf Spalte und Wert
        String displayText = formatTimeValue(value, column);
        setText(displayText);

        // Spezielle Farbgebung für bestimmte Zustände (nur wenn editierbar)
        if (cellEditable) {
            applySpecialFormatting(value, column, isSelected);
        }

        return this;
    }

    /**
     * Formatiert den Zeitwert basierend auf der Spalte
     */
    private String formatTimeValue(Object value, int column) {
        if (value == null) {
            return switch (column) {
                case COL_END_TIME -> "--:--";
                case COL_DURATION -> "laufend";
                default -> "";
            };
        }

        String stringValue = value.toString().trim();

        if (stringValue.isEmpty()) {
            return switch (column) {
                case COL_END_TIME -> "--:--";
                case COL_DURATION -> "laufend";
                default -> "";
            };
        }

        // Spezielle Behandlung für Dauer-Spalte
        if (column == COL_DURATION && stringValue.equals("00:00")) {
            return "laufend";
        }

        return stringValue;
    }

    /**
     * Wendet spezielle Farbformatierung an
     */
    private void applySpecialFormatting(Object value, int column, boolean isSelected) {
        if (isSelected) {
            return; // Bei Selektion keine speziellen Farben
        }

        String stringValue = value != null ? value.toString().trim() : "";

        switch (column) {
            case COL_END_TIME:
                if (stringValue.isEmpty() || stringValue.equals("--:--")) {
                    setForeground(Color.GRAY);
                }
                break;

            case COL_DURATION:
                if (stringValue.isEmpty() || stringValue.equals("00:00") || stringValue.equals("laufend")) {
                    setForeground(new Color(0, 150, 0)); // Grün für laufende Aktivitäten
                } else {
                    // Normale Zeit-Darstellung
                    setForeground(Color.BLACK);
                }
                break;

            default:
                setForeground(Color.BLACK);
                break;
        }
    }

    /**
     * Hilfsmethode zur Validierung von Zeitformaten
     */
    private boolean isValidTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return false;
        }

        return timeStr.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    }

    /**
     * Formatiert Minuten zu HH:mm Format
     */
    private String formatMinutesToTime(long minutes) {
        if (minutes <= 0) {
            return "00:00";
        }

        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }
}