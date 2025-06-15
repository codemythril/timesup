package de.timetracker.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_NAME = "timetracker.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;
    private static DatabaseManager instance;
    private static boolean isInitialized = false;

    private DatabaseManager() {
        if (!isInitialized) {
            // SQLite JDBC Treiber explizit laden
            try {
                Class.forName("org.sqlite.JDBC");
                System.out.println("SQLite JDBC Treiber geladen");
                initializeDatabase();
                isInitialized = true;
            } catch (ClassNotFoundException e) {
                System.err.println("SQLite JDBC Treiber nicht gefunden: " + e.getMessage());
                System.err.println("Bitte prüfen Sie, ob sqlite-jdbc im Classpath enthalten ist");
            }
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            // Debugging nur beim ersten Mal
            if (!isInitialized) {
                System.out.println("Erste Datenbankverbindung hergestellt zu: " + DB_URL);
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("Fehler beim Verbinden zur Datenbank: " + e.getMessage());
            System.err.println("Versuche Datenbankdatei zu erstellen...");

            // Versuche Datenbankdatei zu erstellen
            File dbFile = new File(DB_NAME);
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile();
                    System.out.println("Datenbankdatei erstellt: " + dbFile.getAbsolutePath());
                } catch (Exception ex) {
                    System.err.println("Konnte Datenbankdatei nicht erstellen: " + ex.getMessage());
                }
            }

            throw e;
        }
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            System.out.println("Datenbank erfolgreich initialisiert: " + DB_NAME);
        } catch (SQLException e) {
            System.err.println("Fehler beim Initialisieren der Datenbank: " + e.getMessage());
            System.err.println("Die Anwendung wird ohne Datenbankfunktionalität fortgesetzt.");
            e.printStackTrace();
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String createTimeEntriesTable = """
            CREATE TABLE IF NOT EXISTS time_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
                description TEXT NOT NULL,
                is_break BOOLEAN DEFAULT 0,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createConsolidatedEntriesTable = """
            CREATE TABLE IF NOT EXISTS consolidated_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                description TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createActivityDescriptionsTable = """
            CREATE TABLE IF NOT EXISTS activity_descriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT UNIQUE NOT NULL,
                usage_count INTEGER DEFAULT 1,
                last_used TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTimeEntriesTable);
            stmt.execute(createConsolidatedEntriesTable);
            stmt.execute(createActivityDescriptionsTable);
            System.out.println("Alle Datenbanktabellen erfolgreich erstellt/verifiziert");
        }
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Datenbankverbindung fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        // SQLite schließt Verbindungen automatisch
        System.out.println("Datenbankverbindungen geschlossen");
    }
}
