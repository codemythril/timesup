package de.timetracker.ui;

import de.timetracker.ui.components.AutoCompleteTextField;
import de.timetracker.utils.TimeFormatter;
import de.timetracker.database.TimeEntryDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;

/**
 * Dialog zum Starten einer neuen Aktivität
 */
public class StartActivityDialog extends JDialog {
    private AutoCompleteTextField descriptionField;
    private JTextField startTimeField;
    private JButton okButton;
    private JButton cancelButton;
    private boolean approved = false;
    private final TimeEntryDAO dao;

    public StartActivityDialog(Frame parent, TimeEntryDAO dao) {
        this(parent, dao, LocalTime.now());
    }

    public StartActivityDialog(Frame parent, TimeEntryDAO dao, LocalTime startTime) {
        super(parent, "Neue Aktivität starten", true);
        this.dao = dao;

        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setupKeyBindings();

        // Startzeit setzen
        startTimeField.setText(TimeFormatter.formatTime(startTime));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);

        // Focus auf Beschreibungsfeld setzen
        SwingUtilities.invokeLater(() -> descriptionField.requestFocusInWindow());
    }

    private void initializeComponents() {
        // Startzeit-Feld (readonly)
        startTimeField = new JTextField(8);
        startTimeField.setEditable(false);
        startTimeField.setHorizontalAlignment(JTextField.CENTER);
        startTimeField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        startTimeField.setBackground(new Color(245, 245, 245));
        startTimeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        // Beschreibungs-Feld mit AutoComplete (jetzt JTextField-basiert)
        descriptionField = new AutoCompleteTextField(dao);
        descriptionField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descriptionField.setPreferredSize(new Dimension(300, 30));

        // Border für das TextField
        descriptionField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        // Buttons
        okButton = new JButton("Starten");
        okButton.setPreferredSize(new Dimension(100, 35));
        okButton.setMinimumSize(new Dimension(100, 35));

        cancelButton = new JButton("Abbrechen");
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setMinimumSize(new Dimension(100, 35));

        // Einheitliche Button-Konfiguration
        JButton[] buttons = {okButton, cancelButton};
        for (JButton button : buttons) {
            button.setOpaque(true);
            button.setBorderPainted(true);
            button.setContentAreaFilled(true);
            button.setFocusPainted(false);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)
            ));
        }

        // Farben setzen
        okButton.setBackground(new Color(52, 152, 219));
        okButton.setForeground(Color.WHITE);

        cancelButton.setBackground(new Color(149, 165, 166));
        cancelButton.setForeground(Color.WHITE);

        // Tooltip-Texte
        startTimeField.setToolTipText("Aktuelle Startzeit (automatisch gesetzt)");
        descriptionField.setToolTipText("Beschreibung der Aktivität (mit AutoComplete)");
        okButton.setToolTipText("Aktivität starten (Enter)");
        cancelButton.setToolTipText("Dialog schließen (Escape)");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // Header-Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        headerPanel.setBackground(new Color(52, 152, 219));

        JLabel titleLabel = new JLabel("Neue Aktivität starten");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(createClockIcon());
        titleLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        titleLabel.setIconTextGap(10);

        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Content-Panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        contentPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Startzeit-Label und Feld
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel startTimeLabel = new JLabel("Startzeit:");
        startTimeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        contentPanel.add(startTimeLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(startTimeField, gbc);

        // Beschreibungs-Label und Feld
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel descriptionLabel = new JLabel("Beschreibung:");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        contentPanel.add(descriptionLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(descriptionField, gbc);

        // Hinweis-Text
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 5, 5);

        String hintText = "<html><i>Tippen Sie für Vorschläge oder geben Sie eine neue Beschreibung ein.<br>" +
                "Die Startzeit wurde automatisch aus dem letzten Eintrag berechnet.</i></html>";
        JLabel hintLabel = new JLabel(hintText);
        hintLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
        hintLabel.setForeground(Color.GRAY);
        contentPanel.add(hintLabel, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        // OK-Button
        okButton.addActionListener(e -> approveAndClose());

        // Cancel-Button
        cancelButton.addActionListener(e -> cancelAndClose());

        // Beschreibungsfeld Enter-Taste (jetzt mit der neuen API)
        descriptionField.addActionListener(e -> {
            if (!descriptionField.getText().trim().isEmpty()) {
                approveAndClose();
            }
        });

        // Beschreibungsfeld Änderungen (jetzt direkt mit getDocument())
        descriptionField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateOkButtonState();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateOkButtonState();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateOkButtonState();
            }
        });

        // Window-Events
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAndClose();
            }
        });

        // Initial OK-Button State
        updateOkButtonState();
    }

    private void setupKeyBindings() {
        JRootPane rootPane = getRootPane();

        // Enter für OK (wenn nicht im AutoComplete)
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "approve");
        rootPane.getActionMap().put("approve", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approveAndClose();
            }
        });

        // Escape für Cancel
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        rootPane.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelAndClose();
            }
        });

        // Standard OK-Button setzen
        rootPane.setDefaultButton(okButton);
    }

    private void updateOkButtonState() {
        boolean hasDescription = !descriptionField.getText().trim().isEmpty();
        okButton.setEnabled(hasDescription);

        if (hasDescription) {
            okButton.setBackground(new Color(52, 152, 219));
            okButton.setText("Starten");
        } else {
            okButton.setBackground(new Color(149, 165, 166));
            okButton.setText("Starten");
        }

        // macOS-spezifische Aktualisierung
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            okButton.repaint();
        }
    }

    private void approveAndClose() {
        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte geben Sie eine Beschreibung ein.",
                    "Fehlende Beschreibung", JOptionPane.WARNING_MESSAGE);
            descriptionField.requestFocusInWindow();
            return;
        }

        approved = true;
        dispose();
    }

    private void cancelAndClose() {
        approved = false;
        dispose();
    }

    private Icon createClockIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Uhr-Kreis
                g2d.setColor(Color.WHITE);
                g2d.fillOval(x, y, 16, 16);
                g2d.setColor(new Color(41, 128, 185));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawOval(x + 1, y + 1, 14, 14);

                // Zeiger
                g2d.setColor(new Color(41, 128, 185));
                g2d.setStroke(new BasicStroke(1.0f));
                int centerX = x + 8;
                int centerY = y + 8;
                g2d.drawLine(centerX, centerY, centerX, centerY - 4); // Stundenzeiger
                g2d.drawLine(centerX, centerY, centerX + 3, centerY); // Minutenzeiger

                g2d.dispose();
            }

            @Override
            public int getIconWidth() { return 16; }

            @Override
            public int getIconHeight() { return 16; }
        };
    }

    // Public getter methods
    public boolean isApproved() {
        return approved;
    }

    public String getDescription() {
        return descriptionField.getText().trim();
    }

    public LocalTime getStartTime() {
        return TimeFormatter.parseTimeSafe(startTimeField.getText());
    }

    public void updateStartTime() {
        startTimeField.setText(TimeFormatter.formatCurrentTime());
    }

    public void setStartTime(LocalTime startTime) {
        startTimeField.setText(TimeFormatter.formatTime(startTime));
    }
}