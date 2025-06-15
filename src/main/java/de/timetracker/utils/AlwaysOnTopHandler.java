package de.timetracker.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

/**
 * Handler-Klasse für Always-on-Top Funktionalität mit erweiterten Features
 * wie Position speichern, Transparenz und Minimierung-Verhalten
 */
public class AlwaysOnTopHandler {
    private static final String PREF_ALWAYS_ON_TOP = "alwaysOnTop";
    private static final String PREF_WINDOW_X = "windowX";
    private static final String PREF_WINDOW_Y = "windowY";
    private static final String PREF_WINDOW_WIDTH = "windowWidth";
    private static final String PREF_WINDOW_HEIGHT = "windowHeight";
    private static final String PREF_OPACITY = "opacity";
    private static final String PREF_MINIMIZED_TO_TRAY = "minimizedToTray";

    private final JFrame window;
    private final Preferences preferences;
    private boolean alwaysOnTop;
    private float opacity;
    private boolean minimizedToTray;
    private SystemTray systemTray;
    private TrayIcon trayIcon;

    public AlwaysOnTopHandler(JFrame window) {
        this.window = window;
        this.preferences = Preferences.userNodeForPackage(AlwaysOnTopHandler.class);
        this.alwaysOnTop = preferences.getBoolean(PREF_ALWAYS_ON_TOP, true);
        this.opacity = preferences.getFloat(PREF_OPACITY, 0.95f);
        this.minimizedToTray = preferences.getBoolean(PREF_MINIMIZED_TO_TRAY, false);

        initialize();
    }

    private void initialize() {
        // Always-on-top aktivieren
        setAlwaysOnTop(alwaysOnTop);

        // Transparenz setzen
        setOpacity(opacity);

        // Gespeicherte Position und Größe wiederherstellen
        restoreWindowBounds();

        // SystemTray initialisieren (falls unterstützt)
        initializeSystemTray();

        // Event-Listener hinzufügen
        addEventListeners();
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;

        // Sicherheitsprüfung für Always-on-top
        try {
            window.setAlwaysOnTop(alwaysOnTop);
            preferences.putBoolean(PREF_ALWAYS_ON_TOP, alwaysOnTop);
            System.out.println("Always-on-top " + (alwaysOnTop ? "aktiviert" : "deaktiviert"));
        } catch (SecurityException e) {
            System.err.println("Keine Berechtigung für Always-on-top: " + e.getMessage());
            this.alwaysOnTop = false;
        }
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void toggleAlwaysOnTop() {
        setAlwaysOnTop(!alwaysOnTop);
    }

    public void setOpacity(float opacity) {
        if (opacity < 0.1f) opacity = 0.1f;
        if (opacity > 1.0f) opacity = 1.0f;

        this.opacity = opacity;

        try {
            if (window.isDisplayable()) {
                window.setOpacity(opacity);
                preferences.putFloat(PREF_OPACITY, opacity);
            }
        } catch (UnsupportedOperationException e) {
            System.err.println("Transparenz wird nicht unterstützt: " + e.getMessage());
        }
    }

    public float getOpacity() {
        return opacity;
    }

    public void increaseOpacity() {
        setOpacity(opacity + 0.05f);
    }

    public void decreaseOpacity() {
        setOpacity(opacity - 0.05f);
    }

    public void setMinimizedToTray(boolean minimizedToTray) {
        this.minimizedToTray = minimizedToTray;
        preferences.putBoolean(PREF_MINIMIZED_TO_TRAY, minimizedToTray);
    }

    public boolean isMinimizedToTray() {
        return minimizedToTray;
    }

    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray wird nicht unterstützt");
            return;
        }

        try {
            systemTray = SystemTray.getSystemTray();

            // Icon erstellen (einfaches Zeiterfassungs-Icon)
            Image trayImage = createTrayIcon();

            // PopupMenu erstellen
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Anzeigen");
            showItem.addActionListener(e -> showWindow());

            MenuItem alwaysOnTopItem = new MenuItem("Always-on-top");
            alwaysOnTopItem.addActionListener(e -> toggleAlwaysOnTop());

            MenuItem exitItem = new MenuItem("Beenden");
            exitItem.addActionListener(e -> {
                saveWindowBounds();
                System.exit(0);
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(alwaysOnTopItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(trayImage, "Zeiterfassung", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> showWindow());

        } catch (Exception e) {
            System.err.println("Fehler beim Initialisieren des SystemTray: " + e.getMessage());
        }
    }

    private Image createTrayIcon() {
        // Erstelle ein einfaches 16x16 Icon für die Zeiterfassung
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Hintergrund
        g2d.setColor(new Color(52, 152, 219));
        g2d.fillRoundRect(1, 1, 14, 14, 4, 4);

        // Uhr-Symbol
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(4, 4, 8, 8);

        // Zeiger
        g2d.drawLine(8, 8, 8, 5);  // Stundenzeiger
        g2d.drawLine(8, 8, 11, 8); // Minutenzeiger

        g2d.dispose();
        return image;
    }

    private void addEventListeners() {
        // Fenster-Events
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (minimizedToTray && systemTray != null) {
                    minimizeToTray();
                } else {
                    saveWindowBounds();
                    System.exit(0);
                }
            }

            @Override
            public void windowIconified(WindowEvent e) {
                if (minimizedToTray && systemTray != null) {
                    minimizeToTray();
                }
            }
        });

        // Position und Größe speichern bei Änderungen
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                saveWindowBounds();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                saveWindowBounds();
            }
        });

        // Keyboard Shortcuts
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        JRootPane rootPane = window.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // Ctrl+T für Always-on-top Toggle
        KeyStroke ctrlT = KeyStroke.getKeyStroke("ctrl T");
        inputMap.put(ctrlT, "toggleAlwaysOnTop");
        actionMap.put("toggleAlwaysOnTop", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleAlwaysOnTop();
            }
        });

        // Ctrl+Plus für Transparenz erhöhen
        KeyStroke ctrlPlus = KeyStroke.getKeyStroke("ctrl PLUS");
        inputMap.put(ctrlPlus, "increaseOpacity");
        actionMap.put("increaseOpacity", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                increaseOpacity();
            }
        });

        // Ctrl+Minus für Transparenz verringern
        KeyStroke ctrlMinus = KeyStroke.getKeyStroke("ctrl MINUS");
        inputMap.put(ctrlMinus, "decreaseOpacity");
        actionMap.put("decreaseOpacity", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                decreaseOpacity();
            }
        });
    }

    private void minimizeToTray() {
        if (systemTray != null && trayIcon != null) {
            try {
                systemTray.add(trayIcon);
                window.setVisible(false);
                System.out.println("Fenster in System Tray minimiert");
            } catch (AWTException e) {
                System.err.println("Fehler beim Minimieren in System Tray: " + e.getMessage());
            }
        }
    }

    private void showWindow() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        window.setVisible(true);
        window.setExtendedState(JFrame.NORMAL);
        window.toFront();
        window.requestFocus();
    }

    private void saveWindowBounds() {
        if (window.getExtendedState() == JFrame.NORMAL) {
            Rectangle bounds = window.getBounds();
            preferences.putInt(PREF_WINDOW_X, bounds.x);
            preferences.putInt(PREF_WINDOW_Y, bounds.y);
            preferences.putInt(PREF_WINDOW_WIDTH, bounds.width);
            preferences.putInt(PREF_WINDOW_HEIGHT, bounds.height);
        }
    }

    private void restoreWindowBounds() {
        int x = preferences.getInt(PREF_WINDOW_X, -1);
        int y = preferences.getInt(PREF_WINDOW_Y, -1);
        int width = preferences.getInt(PREF_WINDOW_WIDTH, 600);
        int height = preferences.getInt(PREF_WINDOW_HEIGHT, 400);

        if (x >= 0 && y >= 0) {
            // Prüfe ob Position noch auf einem verfügbaren Monitor liegt
            if (isLocationOnScreen(x, y)) {
                window.setBounds(x, y, width, height);
            } else {
                centerOnScreen();
            }
        } else {
            centerOnScreen();
        }
    }

    private boolean isLocationOnScreen(int x, int y) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();

        for (GraphicsDevice device : devices) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            if (bounds.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private void centerOnScreen() {
        window.setLocationRelativeTo(null);
    }

    public void cleanup() {
        saveWindowBounds();
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }

    // Getter für UI-Integration
    public String getStatusText() {
        return String.format("Always-on-top: %s | Transparenz: %.0f%%",
                alwaysOnTop ? "EIN" : "AUS",
                opacity * 100);
    }
}
