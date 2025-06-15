package de.timetracker.database;

import de.timetracker.model.ActivityDescription;
import de.timetracker.model.ConsolidatedEntry;
import de.timetracker.model.TimeEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimeEntryDAO {
    private final DatabaseManager dbManager;

    public TimeEntryDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // TimeEntry CRUD Operationen
    public boolean insertTimeEntry(TimeEntry entry) {
        String sql = "INSERT INTO time_entries (date, start_time, end_time, description, is_break) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entry.getDate().toString());
            pstmt.setString(2, entry.getStartTime().toString());
            pstmt.setString(3, entry.getEndTime() != null ? entry.getEndTime().toString() : null);
            pstmt.setString(4, entry.getDescription());
            pstmt.setBoolean(5, entry.isBreak());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // SQLite-kompatible ID-Abfrage
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        entry.setId(rs.getInt(1));
                    }
                }

                // Aktivitätsbeschreibung für Autocomplete speichern
                saveOrUpdateActivityDescription(entry.getDescription());

                return true;
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Einfügen des TimeEntry: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTimeEntry(TimeEntry entry) {
        String sql = "UPDATE time_entries SET date=?, start_time=?, end_time=?, description=?, is_break=? WHERE id=?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entry.getDate().toString());
            pstmt.setString(2, entry.getStartTime().toString());
            pstmt.setString(3, entry.getEndTime() != null ? entry.getEndTime().toString() : null);
            pstmt.setString(4, entry.getDescription());
            pstmt.setBoolean(5, entry.isBreak());
            pstmt.setInt(6, entry.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                saveOrUpdateActivityDescription(entry.getDescription());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Aktualisieren des TimeEntry: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTimeEntry(int id) {
        String sql = "DELETE FROM time_entries WHERE id=?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen des TimeEntry: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<TimeEntry> getTimeEntriesByDate(LocalDate date) {
        String sql = "SELECT * FROM time_entries WHERE date=? ORDER BY start_time";
        List<TimeEntry> entries = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TimeEntry entry = mapResultSetToTimeEntry(rs);
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der TimeEntries: " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    public TimeEntry getActiveTimeEntry() {
        String sql = "SELECT * FROM time_entries WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return mapResultSetToTimeEntry(rs);
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden des aktiven TimeEntry: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ConsolidatedEntry CRUD Operationen
    public boolean insertConsolidatedEntry(ConsolidatedEntry entry) {
        String sql = "INSERT INTO consolidated_entries (date, start_time, end_time, description, duration_minutes) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entry.getDate().toString());
            pstmt.setString(2, entry.getStartTime().toString());
            pstmt.setString(3, entry.getEndTime().toString());
            pstmt.setString(4, entry.getDescription());
            pstmt.setInt(5, entry.getDurationMinutes());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // SQLite-kompatible ID-Abfrage
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        entry.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Einfügen des ConsolidatedEntry: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<ConsolidatedEntry> getConsolidatedEntriesByDate(LocalDate date) {
        String sql = "SELECT * FROM consolidated_entries WHERE date=? ORDER BY start_time";
        List<ConsolidatedEntry> entries = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ConsolidatedEntry entry = mapResultSetToConsolidatedEntry(rs);
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der ConsolidatedEntries: " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    public boolean deleteConsolidatedEntriesByDate(LocalDate date) {
        String sql = "DELETE FROM consolidated_entries WHERE date=?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());
            return pstmt.executeUpdate() >= 0; // >= 0 weil auch 0 gelöschte Einträge OK sind

        } catch (SQLException e) {
            System.err.println("Fehler beim Löschen der ConsolidatedEntries: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ActivityDescription Operationen
    public void saveOrUpdateActivityDescription(String description) {
        if (description == null || description.trim().isEmpty()) return;

        String selectSql = "SELECT id, usage_count FROM activity_descriptions WHERE LOWER(description) = LOWER(?)";
        String insertSql = "INSERT INTO activity_descriptions (description, usage_count, last_used) VALUES (?, 1, ?)";
        String updateSql = "UPDATE activity_descriptions SET usage_count = usage_count + 1, last_used = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            // Prüfe ob Beschreibung bereits existiert
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, description.trim());

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        // Update existing
                        int id = rs.getInt("id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, LocalDateTime.now().toString());
                            updateStmt.setInt(2, id);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Insert new
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, description.trim());
                            insertStmt.setString(2, LocalDateTime.now().toString());
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Speichern der ActivityDescription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ActivityDescription> getActivityDescriptions(int limit) {
        if (limit <= 0) limit = 10; // Sicherheitscheck

        String sql = "SELECT * FROM activity_descriptions ORDER BY usage_count DESC, last_used DESC LIMIT ?";
        List<ActivityDescription> descriptions = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ActivityDescription desc = mapResultSetToActivityDescription(rs);
                    descriptions.add(desc);
                }
            }

            System.out.println("ActivityDescriptions geladen: " + descriptions.size() + " von maximal " + limit);

        } catch (SQLException e) {
            System.err.println("Fehler beim Laden der ActivityDescriptions: " + e.getMessage());
            // Keine weitere Fehlermeldung - return empty list
        }

        return descriptions;
    }

    // Hilfsmethoden für ResultSet Mapping
    private TimeEntry mapResultSetToTimeEntry(ResultSet rs) throws SQLException {
        TimeEntry entry = new TimeEntry();
        entry.setId(rs.getInt("id"));
        entry.setDate(LocalDate.parse(rs.getString("date")));
        entry.setStartTime(LocalTime.parse(rs.getString("start_time")));

        String endTimeStr = rs.getString("end_time");
        if (endTimeStr != null) {
            entry.setEndTime(LocalTime.parse(endTimeStr));
        }

        entry.setDescription(rs.getString("description"));
        entry.setBreak(rs.getBoolean("is_break"));

        return entry;
    }

    private ConsolidatedEntry mapResultSetToConsolidatedEntry(ResultSet rs) throws SQLException {
        return new ConsolidatedEntry(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("date")),
                LocalTime.parse(rs.getString("start_time")),
                LocalTime.parse(rs.getString("end_time")),
                rs.getString("description"),
                rs.getInt("duration_minutes")
        );
    }

    private ActivityDescription mapResultSetToActivityDescription(ResultSet rs) throws SQLException {
        return new ActivityDescription(
                rs.getInt("id"),
                rs.getString("description"),
                rs.getInt("usage_count"),
                LocalDateTime.parse(rs.getString("last_used"))
        );
    }

    // Statistik-Methoden
    public long getTotalWorkMinutesToday() {
        LocalDate today = LocalDate.now();
        List<TimeEntry> entries = getTimeEntriesByDate(today);

        return entries.stream()
                .filter(e -> !e.isBreak() && e.getEndTime() != null)
                .mapToLong(TimeEntry::getDurationMinutes)
                .sum();
    }

    public long getTotalBreakMinutesToday() {
        LocalDate today = LocalDate.now();
        List<TimeEntry> entries = getTimeEntriesByDate(today);

        return entries.stream()
                .filter(e -> e.isBreak() && e.getEndTime() != null)
                .mapToLong(TimeEntry::getDurationMinutes)
                .sum();
    }
}