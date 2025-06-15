package de.timetracker.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom TableCellRenderer für Button-Spalten (Löschen) in der Zeiterfassungstabelle
 */
public class ButtonRenderer extends DefaultTableCellRenderer {
    private final JButton button;

    public ButtonRenderer() {
        button = new JButton();
        setupButton();
    }

    private void setupButton() {
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);

        // Font und Größe
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setPreferredSize(new Dimension(50, 25));

        // Border
        button.setBorder(BorderFactory.createRaisedBevelBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // Button-Text setzen
        button.setText("✕");

        // Prüfe ob die Zeile löschbar ist (nur wenn es EditableTable ist)
        boolean canDelete = true;
        if (table instanceof EditableTable editableTable) {
            // Zeile ist nur löschbar wenn sie abgeschlossen ist (Endzeit vorhanden)
            canDelete = !table.isCellEditable(row, column) ? false :
                    editableTable.getTimeEntries().size() > row ?
                            editableTable.getTimeEntries().get(row).getEndTime() != null : true;
        }

        // Farben basierend auf Löschbarkeit und Selektion
        if (!canDelete) {
            // Nicht löschbare Einträge (laufende Aktivitäten)
            button.setBackground(new Color(240, 240, 240)); // Hellgrau
            button.setForeground(Color.GRAY);
            button.setToolTipText("Laufende Aktivitäten können nicht gelöscht werden");
            button.setEnabled(false);
        } else if (isSelected) {
            button.setBackground(new Color(255, 150, 150)); // Helles Rot bei Selektion
            button.setForeground(Color.RED.darker());
            button.setToolTipText("Eintrag löschen");
            button.setEnabled(true);
        } else {
            button.setBackground(new Color(255, 220, 220)); // Sehr helles Rot
            button.setForeground(Color.RED);
            button.setToolTipText("Eintrag löschen");
            button.setEnabled(true);
        }

        // Hover-Effekt simulieren (falls gewünscht)
        if (hasFocus && canDelete) {
            button.setBackground(new Color(255, 180, 180)); // Mittleres Rot bei Focus
            button.setBorder(BorderFactory.createLoweredBevelBorder());
        } else {
            button.setBorder(BorderFactory.createRaisedBevelBorder());
        }

        return button;
    }

    /**
     * Erstellt einen alternativen Button mit anderem Symbol
     */
    public static ButtonRenderer createEditRenderer() {
        ButtonRenderer renderer = new ButtonRenderer();
        renderer.setButtonText("✎");
        renderer.setButtonColors(
                new Color(220, 220, 255), // Helles Blau
                new Color(0, 0, 150)       // Dunkles Blau
        );
        renderer.setTooltipText("Eintrag bearbeiten");
        return renderer;
    }

    /**
     * Erstellt einen Button-Renderer für andere Aktionen
     */
    public static ButtonRenderer createCustomRenderer(String text, Color bgColor, Color fgColor, String tooltip) {
        ButtonRenderer renderer = new ButtonRenderer();
        renderer.setButtonText(text);
        renderer.setButtonColors(bgColor, fgColor);
        renderer.setTooltipText(tooltip);
        return renderer;
    }

    // Hilfsmethoden für Konfiguration

    public void setButtonText(String text) {
        button.setText(text);
    }

    public void setButtonColors(Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
    }

    public void setTooltipText(String tooltip) {
        button.setToolTipText(tooltip);
    }

    public void setButtonFont(Font font) {
        button.setFont(font);
    }

    public JButton getButton() {
        return button;
    }
}