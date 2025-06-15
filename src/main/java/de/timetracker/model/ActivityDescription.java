package de.timetracker.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityDescription {
    private int id;
    private String description;
    private int usageCount;
    private LocalDateTime lastUsed;

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Konstruktoren
    public ActivityDescription() {
        this.usageCount = 1;
        this.lastUsed = LocalDateTime.now();
    }

    public ActivityDescription(String description) {
        this.description = description;
        this.usageCount = 1;
        this.lastUsed = LocalDateTime.now();
    }

    public ActivityDescription(int id, String description, int usageCount, LocalDateTime lastUsed) {
        this.id = id;
        this.description = description;
        this.usageCount = usageCount;
        this.lastUsed = lastUsed;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    // Hilfsmethoden
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsed = LocalDateTime.now();
    }

    public String getLastUsedFormatted() {
        return lastUsed != null ? lastUsed.format(DATETIME_FORMATTER) : "";
    }

    public boolean isRecentlyUsed(int daysBack) {
        if (lastUsed == null) return false;
        return lastUsed.isAfter(LocalDateTime.now().minusDays(daysBack));
    }

    @Override
    public String toString() {
        return description; // Für Dropdown-Listen
    }

    public String toDetailString() {
        return String.format("ActivityDescription{id=%d, description='%s', usageCount=%d, lastUsed=%s}",
                id, description, usageCount, getLastUsedFormatted());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ActivityDescription that = (ActivityDescription) obj;
        return description != null ? description.equalsIgnoreCase(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        return description != null ? description.toLowerCase().hashCode() : 0;
    }
}