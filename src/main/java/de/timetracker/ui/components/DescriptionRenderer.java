package de.timetracker.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom TableCellRenderer für Beschreibungs-Spalte in der Zeiterfassungstabelle
 */
public class DescriptionRenderer extends DefaultTableCellRenderer {

    public DescriptionRenderer() {
        setOpaque(true); // Wichtig für Hintergrundfarbe auf macOS
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // Basis-Rendering
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Text setzen
        String text = value != null ? value.toString() : "";
        setText(text);

        // Farben basierend auf Selektion und Inhalt
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            applyContentBasedFormatting(text);
        }

        // Tooltip für längere Beschreibungen
        if (text.length() > 30) {
            setToolTipText(text);
        } else {
            setToolTipText(null);
        }

        return this;
    }

    /**
     * Wendet inhaltbasierte Formatierung an
     */
    private void applyContentBasedFormatting(String text) {
        if (text == null || text.trim().isEmpty()) {
            setBackground(Color.WHITE);
            setForeground(Color.LIGHT_GRAY);
            setText("(Keine Beschreibung)");
            return;
        }

        String lowerText = text.toLowerCase().trim();

        // Pausen-Einträge speziell markieren
        if (isPauseActivity(lowerText)) {
            setBackground(new Color(255, 248, 220)); // Helles Orange/Gelb
            setForeground(new Color(200, 100, 0));   // Dunkles Orange
        }
        // Meeting-Einträge
        else if (isMeetingActivity(lowerText)) {
            setBackground(new Color(230, 240, 255)); // Helles Blau
            setForeground(new Color(0, 50, 150));    // Dunkles Blau
        }
        // Entwicklungs-/Arbeits-Einträge
        else if (isWorkActivity(lowerText)) {
            setBackground(new Color(240, 255, 240)); // Helles Grün
            setForeground(new Color(0, 100, 0));     // Dunkles Grün
        }
        // Standard-Einträge
        else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }
    }

    /**
     * Prüft ob es sich um eine Pause handelt
     */
    private boolean isPauseActivity(String text) {
        return text.contains("pause") ||
                text.contains("break") ||
                text.contains("mittagspause") ||
                text.contains("kaffeepause") ||
                text.contains("mittagessen") ||
                text.equals("pause") ||
                text.equals("break");
    }

    /**
     * Prüft ob es sich um ein Meeting handelt
     */
    private boolean isMeetingActivity(String text) {
        return text.contains("meeting") ||
                text.contains("besprechung") ||
                text.contains("call") ||
                text.contains("termin") ||
                text.contains("conference") ||
                text.contains("standup") ||
                text.contains("retrospektive") ||
                text.contains("planning");
    }

    /**
     * Prüft ob es sich um Entwicklungsarbeit handelt
     */
    private boolean isWorkActivity(String text) {
        return text.contains("entwicklung") ||
                text.contains("programmierung") ||
                text.contains("coding") ||
                text.contains("implementation") ||
                text.contains("bugfix") ||
                text.contains("feature") ||
                text.contains("refactoring") ||
                text.contains("testing") ||
                text.contains("review") ||
                text.contains("dokumentation");
    }

    /**
     * Kürzt zu lange Texte für die Anzeige
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }
}
