package de.timetracker.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConsolidatedEntry {
    private int id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
    private int durationMinutes;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_DURATION_MINUTES = 120; // 2 Stunden

    // Konstruktoren
    public ConsolidatedEntry() {
        this.date = LocalDate.now();
    }

    public ConsolidatedEntry(LocalDate date, LocalTime startTime, LocalTime endTime,
                             String description, int durationMinutes) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.durationMinutes = durationMinutes;
    }

    public ConsolidatedEntry(int id, LocalDate date, LocalTime startTime, LocalTime endTime,
                             String description, int durationMinutes) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.durationMinutes = durationMinutes;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    // Hilfsmethoden
    public String getDurationFormatted() {
        long hours = durationMinutes / 60;
        long mins = durationMinutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    public String getStartTimeFormatted() {
        return startTime != null ? startTime.format(TIME_FORMATTER) : "";
    }

    public String getEndTimeFormatted() {
        return endTime != null ? endTime.format(TIME_FORMATTER) : "";
    }

    public boolean exceedsMaxDuration() {
        return durationMinutes > MAX_DURATION_MINUTES;
    }

    public static int getMaxDurationMinutes() {
        return MAX_DURATION_MINUTES;
    }

    // Statische Methode zur Konsolidierung von TimeEntries
    public static List<ConsolidatedEntry> consolidateTimeEntries(List<TimeEntry> timeEntries) {
        List<ConsolidatedEntry> consolidated = new ArrayList<>();

        // Gruppiere nach Beschreibung (case-insensitive)
        var groupedEntries = new java.util.HashMap<String, List<TimeEntry>>();

        for (TimeEntry entry : timeEntries) {
            if (entry.getEndTime() == null || entry.isBreak()) {
                continue; // Überspringe aktive Einträge und Pausen
            }

            String key = entry.getDescription().toLowerCase().trim();
            groupedEntries.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        // Erstelle konsolidierte Einträge
        for (var group : groupedEntries.entrySet()) {
            List<TimeEntry> entries = group.getValue();
            if (entries.isEmpty()) continue;

            // Sortiere nach Startzeit
            entries.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

            String description = entries.get(0).getDescription();
            int totalMinutes = entries.stream()
                    .mapToInt(e -> (int) e.getDurationMinutes())
                    .sum();

            // Teile in 2-Stunden-Blöcke auf
            LocalTime currentStart = entries.get(0).getStartTime();
            int remainingMinutes = totalMinutes;
            int blockNumber = 1;

            while (remainingMinutes > 0) {
                int blockDuration = Math.min(remainingMinutes, MAX_DURATION_MINUTES);
                LocalTime blockEnd = currentStart.plusMinutes(blockDuration);

                String blockDescription = description;
                if (totalMinutes > MAX_DURATION_MINUTES) {
                    blockDescription += " (Teil " + blockNumber + ")";
                }

                ConsolidatedEntry consolidatedEntry = new ConsolidatedEntry(
                        entries.get(0).getDate(),
                        currentStart,
                        blockEnd,
                        blockDescription,
                        blockDuration
                );

                consolidated.add(consolidatedEntry);

                currentStart = blockEnd;
                remainingMinutes -= blockDuration;
                blockNumber++;
            }
        }

        // Sortiere nach Startzeit
        consolidated.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        return consolidated;
    }

    @Override
    public String toString() {
        return String.format("ConsolidatedEntry{id=%d, date=%s, start=%s, end=%s, description='%s', duration=%d min}",
                id, date, getStartTimeFormatted(), getEndTimeFormatted(), description, durationMinutes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConsolidatedEntry that = (ConsolidatedEntry) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}