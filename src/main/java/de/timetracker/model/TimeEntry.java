package de.timetracker.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class TimeEntry {
    private int id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
    private boolean isBreak;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Konstruktoren
    public TimeEntry() {
        this.date = LocalDate.now();
    }

    public TimeEntry(LocalDate date, LocalTime startTime, String description) {
        this.date = date;
        this.startTime = startTime;
        this.description = description;
        this.isBreak = isBreakActivity(description);
    }

    public TimeEntry(int id, LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.isBreak = isBreakActivity(description);
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
        this.isBreak = isBreakActivity(description);
    }

    public boolean isBreak() {
        return isBreak;
    }

    public void setBreak(boolean isBreak) {
        this.isBreak = isBreak;
    }

    // Hilfsmethoden
    public boolean isActive() {
        return endTime == null;
    }

    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toMinutes();
    }

    public String getDurationFormatted() {
        long minutes = getDurationMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    public String getStartTimeFormatted() {
        return startTime != null ? startTime.format(TIME_FORMATTER) : "";
    }

    public String getEndTimeFormatted() {
        return endTime != null ? endTime.format(TIME_FORMATTER) : "";
    }

    private static boolean isBreakActivity(String description) {
        if (description == null) return false;
        String desc = description.toLowerCase().trim();
        return desc.contains("pause") || desc.equals("break") ||
                desc.contains("mittagspause") || desc.contains("kaffeepause");
    }

    public void stopActivity() {
        if (endTime == null) {
            endTime = LocalTime.now();
        }
    }

    @Override
    public String toString() {
        return String.format("TimeEntry{id=%d, date=%s, start=%s, end=%s, description='%s', isBreak=%s}",
                id, date, getStartTimeFormatted(), getEndTimeFormatted(), description, isBreak);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TimeEntry that = (TimeEntry) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}