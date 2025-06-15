package de.timetracker.ui.components;

import de.timetracker.model.ActivityDescription;
import de.timetracker.database.TimeEntryDAO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Vereinfachtes TextField mit Autocomplete-Funktionalität
 * Basiert auf JTextField statt JComboBox um Fokus-Probleme zu vermeiden
 */
public class AutoCompleteTextField extends JTextField {
    private final TimeEntryDAO dao;
    private List<ActivityDescription> allSuggestions;
    private JPopupMenu suggestionPopup;
    private boolean isUpdating = false;
    private ActionListener enterListener;
    private int maxSuggestions = 5; // Anzahl der maximal angezeigten Suggestions

    public AutoCompleteTextField(TimeEntryDAO dao) {
        super();
        this.dao = dao;
        this.allSuggestions = new ArrayList<>();
        this.suggestionPopup = new JPopupMenu();

        initializeComponent();
        loadSuggestions();
        setupEventListeners();
    }

    private void initializeComponent() {
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    private void setupEventListeners() {
        // Document Listener für Text-Änderungen
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isUpdating) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }
        });

        // Key Listener für Enter und Escape
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    hideSuggestions();

                    // Feuere Enter-Event an Listener
                    if (enterListener != null) {
                        ActionEvent event = new ActionEvent(AutoCompleteTextField.this,
                                ActionEvent.ACTION_PERFORMED, "enterPressed");
                        enterListener.actionPerformed(event);
                    }

                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideSuggestions();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestionPopup.isVisible()) {
                    // Navigation in Suggestions mit Pfeiltasten
                    Component[] components = suggestionPopup.getComponents();
                    if (components.length > 0 && components[0] instanceof JMenuItem) {
                        ((JMenuItem) components[0]).requestFocusInWindow();
                    }
                }
            }
        });

        // Focus Listener
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Popup nur schließen wenn Fokus nicht zu Suggestion geht
                if (!e.isTemporary()) {
                    Timer timer = new Timer(100, evt -> {
                        if (!hasFocus()) {
                            hideSuggestions();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }
        });
    }

    private void updateSuggestions() {
        String text = getText().trim();

        if (text.isEmpty()) {
            // Zeige häufigste Beschreibungen bei leerem Text
            showSuggestions(allSuggestions.stream()
                    .limit(maxSuggestions)
                    .map(ActivityDescription::getDescription)
                    .toArray(String[]::new));
            return;
        }

        // Filter Suggestions
        String[] filtered = allSuggestions.stream()
                .filter(desc -> desc.getDescription().toLowerCase()
                        .contains(text.toLowerCase()))
                .limit(maxSuggestions)
                .map(ActivityDescription::getDescription)
                .toArray(String[]::new);

        if (filtered.length > 0) {
            showSuggestions(filtered);
        } else {
            hideSuggestions();
        }
    }

    private void showSuggestions(String[] suggestions) {
        if (!isDisplayable() || !isShowing()) {
            return;
        }

        suggestionPopup.removeAll();

        for (String suggestion : suggestions) {
            JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(e -> {
                isUpdating = true;
                setText(suggestion);
                setCaretPosition(suggestion.length());
                isUpdating = false;
                hideSuggestions();
                requestFocusInWindow(); // Fokus zurück zum TextField
            });
            suggestionPopup.add(item);
        }

        if (suggestionPopup.getComponentCount() > 0) {
            try {
                Point location = getLocationOnScreen();
                suggestionPopup.show(this, 0, getHeight());
            } catch (Exception e) {
                // Ignore - kann nicht angezeigt werden
            }
        }
    }

    private void hideSuggestions() {
        if (suggestionPopup.isVisible()) {
            suggestionPopup.setVisible(false);
        }
    }

    private void loadSuggestions() {
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Lade AutoComplete-Suggestions...");
                allSuggestions = dao.getActivityDescriptions(50);
                System.out.println("AutoComplete-Suggestions geladen: " + allSuggestions.size() + " Einträge");
            } catch (Exception e) {
                System.err.println("Fehler beim Laden der Suggestions: " + e.getMessage());
                allSuggestions = new ArrayList<>();
            }
        });
    }

    public void refreshSuggestions() {
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Aktualisiere AutoComplete-Suggestions...");
                allSuggestions = dao.getActivityDescriptions(50);
            } catch (Exception e) {
                System.err.println("Fehler beim Aktualisieren der Suggestions: " + e.getMessage());
            }
        });
    }

    /**
     * Setzt einen ActionListener der nur bei Enter-Taste aufgerufen wird
     */
    public void setEnterActionListener(ActionListener listener) {
        this.enterListener = listener;
    }

    /**
     * Kompatibilitätsmethode für ActionListener (ersetzt addActionListener)
     */
    public void addActionListener(ActionListener listener) {
        setEnterActionListener(listener);
    }

    /**
     * Kompatibilitätsmethode für getDocument() - bereits vorhanden da JTextField
     */
    // getDocument() ist bereits von JTextField verfügbar

    /**
     * Setzt die maximale Anzahl der angezeigten Suggestions (Ersatz für setMaximumRowCount)
     */
    public void setMaxSuggestions(int maxSuggestions) {
        this.maxSuggestions = Math.max(1, Math.min(maxSuggestions, 10)); // Zwischen 1 und 10
    }

    /**
     * Kompatibilitätsmethode für setMaximumRowCount (aus ComboBox-API)
     */
    public void setMaximumRowCount(int rowCount) {
        setMaxSuggestions(rowCount);
    }

    /**
     * Bereitet das Feld für Tabellen-Editing vor
     */
    public void prepareForTableEditing() {
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();

            // Zeige Suggestions wenn Text leer ist
            if (getText().trim().isEmpty() && allSuggestions.size() > 0) {
                Timer timer = new Timer(100, e -> {
                    if (hasFocus()) {
                        updateSuggestions();
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Sicherstellen dass Suggestions geladen sind wenn Komponente hinzugefügt wird
        if (allSuggestions.isEmpty()) {
            loadSuggestions();
        }
    }

    @Override
    public void removeNotify() {
        hideSuggestions();
        super.removeNotify();
    }
}