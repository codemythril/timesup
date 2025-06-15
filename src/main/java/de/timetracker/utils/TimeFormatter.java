package de.timetracker.utils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utility-Klasse für Zeit- und Datumsformatierung in der Zeiterfassungsapplikation
 */
public class TimeFormatter {

    // Standard-Formatter
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter TIME_FORMAT_WITH_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("dd.MM.");
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Regex-Patterns für Zeiteingabe-Validierung
    private static final Pattern TIME_PATTERN_HMMS = Pattern.compile("^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$");
    private static final Pattern TIME_PATTERN_HMM = Pattern.compile("^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$");
    private static final Pattern TIME_PATTERN_FLEXIBLE = Pattern.compile("^([0-1]?[0-9]|2[0-3])[:\\.]([0-5][0-9])$");

    private TimeFormatter() {
        // Utility-Klasse - keine Instanziierung
    }

    // Zeit-Formatierung

    /**
     * Formatiert LocalTime zu HH:mm String
     */
    public static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMAT) : "";
    }

    /**
     * Formatiert LocalTime zu HH:mm:ss String
     */
    public static String formatTimeWithSeconds(LocalTime time) {
        return time != null ? time.format(TIME_FORMAT_WITH_SECONDS) : "";
    }

    /**
     * Formatiert aktuelle Zeit zu HH:mm String
     */
    public static String formatCurrentTime() {
        return formatTime(LocalTime.now());
    }

    /**
     * Formatiert Dauer in Minuten zu HH:mm String
     */
    public static String formatDuration(long minutes) {
        if (minutes < 0) {
            return "-" + formatDuration(-minutes);
        }

        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    /**
     * Formatiert Duration-Objekt zu HH:mm String
     */
    public static String formatDuration(Duration duration) {
        if (duration == null) return "00:00";
        return formatDuration(duration.toMinutes());
    }

    /**
     * Formatiert Dauer in Minuten zu lesbarem Text (z.B. "2h 30min")
     */
    public static String formatDurationText(long minutes) {
        if (minutes == 0) return "0 min";
        if (minutes < 0) return "-" + formatDurationText(-minutes);

        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours == 0) {
            return mins + " min";
        } else if (mins == 0) {
            return hours + "h";
        } else {
            return hours + "h " + mins + "min";
        }
    }

    // Datum-Formatierung

    /**
     * Formatiert LocalDate zu dd.MM.yyyy String
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "";
    }

    /**
     * Formatiert LocalDate zu dd.MM. String (ohne Jahr)
     */
    public static String formatDateShort(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT_SHORT) : "";
    }

    /**
     * Formatiert aktuelles Datum zu dd.MM.yyyy String
     */
    public static String formatCurrentDate() {
        return formatDate(LocalDate.now());
    }

    /**
     * Formatiert Datum für Fenstertitel (mit Wochentag)
     */
    public static String formatDateForTitle(LocalDate date) {
        if (date == null) date = LocalDate.now();

        String dayName = getDayName(date.getDayOfWeek().getValue());
        return dayName + ", " + formatDate(date);
    }

    // Parsing-Methoden

    /**
     * Parst Zeit-String zu LocalTime (flexibel: HH:mm oder HH.mm)
     */
    public static LocalTime parseTime(String timeStr) throws DateTimeParseException {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new DateTimeParseException("Leerer Zeit-String", "", 0);
        }

        String cleaned = timeStr.trim().replace('.', ':');

        if (!TIME_PATTERN_FLEXIBLE.matcher(cleaned).matches()) {
            throw new DateTimeParseException("Ungültiges Zeitformat", timeStr, 0);
        }

        return LocalTime.parse(cleaned, TIME_FORMAT);
    }

    /**
     * Versucht Zeit-String zu parsen, gibt null zurück bei Fehlern
     */
    public static LocalTime parseTimeSafe(String timeStr) {
        try {
            return parseTime(timeStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parst Datum-String zu LocalDate
     */
    public static LocalDate parseDate(String dateStr) throws DateTimeParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new DateTimeParseException("Leerer Datum-String", "", 0);
        }

        return LocalDate.parse(dateStr.trim(), DATE_FORMAT);
    }

    /**
     * Versucht Datum-String zu parsen, gibt null zurück bei Fehlern
     */
    public static LocalDate parseDateSafe(String dateStr) {
        try {
            return parseDate(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Validierung

    /**
     * Prüft ob Zeit-String gültig ist
     */
    public static boolean isValidTime(String timeStr) {
        return parseTimeSafe(timeStr) != null;
    }

    /**
     * Prüft ob Datum-String gültig ist
     */
    public static boolean isValidDate(String dateStr) {
        return parseDateSafe(dateStr) != null;
    }

    /**
     * Prüft ob Endzeit nach Startzeit liegt
     */
    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) return false;
        return endTime.isAfter(startTime);
    }

    /**
     * Prüft ob Endzeit nach Startzeit liegt (String-Version)
     */
    public static boolean isValidTimeRange(String startTimeStr, String endTimeStr) {
        LocalTime start = parseTimeSafe(startTimeStr);
        LocalTime end = parseTimeSafe(endTimeStr);
        return isValidTimeRange(start, end);
    }

    // Berechnungen

    /**
     * Berechnet Dauer zwischen zwei Zeiten in Minuten
     */
    public static long calculateDurationMinutes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) return 0;
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Berechnet Dauer zwischen zwei Zeiten (String-Version)
     */
    public static long calculateDurationMinutes(String startTimeStr, String endTimeStr) {
        LocalTime start = parseTimeSafe(startTimeStr);
        LocalTime end = parseTimeSafe(endTimeStr);
        return calculateDurationMinutes(start, end);
    }

    /**
     * Addiert Minuten zu einer Zeit
     */
    public static LocalTime addMinutes(LocalTime time, long minutes) {
        return time != null ? time.plusMinutes(minutes) : null;
    }

    /**
     * Subtrahiert Minuten von einer Zeit
     */
    public static LocalTime subtractMinutes(LocalTime time, long minutes) {
        return time != null ? time.minusMinutes(minutes) : null;
    }

    // Hilfsmethoden

    /**
     * Gibt deutschen Wochentag-Namen zurück
     */
    public static String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Montag";
            case 2 -> "Dienstag";
            case 3 -> "Mittwoch";
            case 4 -> "Donnerstag";
            case 5 -> "Freitag";
            case 6 -> "Samstag";
            case 7 -> "Sonntag";
            default -> "Unbekannt";
        };
    }

    /**
     * Gibt kurzen deutschen Wochentag-Namen zurück
     */
    public static String getDayNameShort(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Mo";
            case 2 -> "Di";
            case 3 -> "Mi";
            case 4 -> "Do";
            case 5 -> "Fr";
            case 6 -> "Sa";
            case 7 -> "So";
            default -> "??";
        };
    }

    /**
     * Prüft ob Zeit in der Arbeitszeit liegt (08:00 - 18:00)
     */
    public static boolean isBusinessHour(LocalTime time) {
        if (time == null) return false;
        return time.isAfter(LocalTime.of(7, 59)) && time.isBefore(LocalTime.of(18, 1));
    }

    /**
     * Rundet Zeit auf nächste 5-Minuten-Marke
     */
    public static LocalTime roundToNearest5Minutes(LocalTime time) {
        if (time == null) return null;

        int minutes = time.getMinute();
        int roundedMinutes = ((minutes + 2) / 5) * 5;

        if (roundedMinutes >= 60) {
            return time.withMinute(0).plusHours(1);
        } else {
            return time.withMinute(roundedMinutes);
        }
    }

    /**
     * Erstellt Zeit-String für CSV-Export
     */
    public static String formatForExport(LocalTime time) {
        return formatTime(time);
    }

    /**
     * Erstellt Datum-String für CSV-Export
     */
    public static String formatForExport(LocalDate date) {
        return formatDate(date);
    }

    /**
     * Formatiert aktuelle Zeit für Logs
     */
    public static String formatForLog() {
        return LocalTime.now().format(TIME_FORMAT_WITH_SECONDS);
    }

    /**
     * Erstellt benutzerfreundliche Relativzeit (heute, gestern, etc.)
     */
    public static String formatRelativeDate(LocalDate date) {
        if (date == null) return "";

        LocalDate today = LocalDate.now();
        long daysDiff = Duration.between(date.atStartOfDay(), today.atStartOfDay()).toDays();

        return switch ((int) daysDiff) {
            case 0 -> "Heute";
            case 1 -> "Gestern";
            case -1 -> "Morgen";
            default -> {
                if (daysDiff > 1 && daysDiff <= 7) {
                    yield "Vor " + daysDiff + " Tagen";
                } else if (daysDiff < -1 && daysDiff >= -7) {
                    yield "In " + (-daysDiff) + " Tagen";
                } else {
                    yield formatDate(date);
                }
            }
        };
    }
}
