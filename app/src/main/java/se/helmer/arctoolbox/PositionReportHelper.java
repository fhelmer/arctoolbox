package se.helmer.arctoolbox;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class PositionReportHelper {

    public static final float METERS_PER_SECOND_TO_KNOTS = 1.943844f;

    private PositionReportHelper() {
        // Utility class
    }

    public static float toKnots(float metersPerSecond) {
        return metersPerSecond * METERS_PER_SECOND_TO_KNOTS;
    }

    public static String formatCoordinate(double value) {
        DecimalFormat format = new DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(value);
    }

    public static String formatSpeedKnots(float knots) {
        DecimalFormat format = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(knots);
    }

    public static String formatCourse(float bearing) {
        DecimalFormat format = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(bearing);
    }

    public static String formatUtcTime(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        df.setTimeZone(tz);
        return df.format(date);
    }

    public static String formatIso8601(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        df.setTimeZone(tz);
        return df.format(date);
    }

    public static String buildReport(String mmsi, String latitude, String longitude,
                                     String speed, String course, String timestamp) {
        return "MMSI=" + (mmsi != null ? mmsi.trim() : "") + "\n" +
                "LAT=" + (latitude != null ? latitude.trim() : "") + "\n" +
                "LONG=" + (longitude != null ? longitude.trim() : "") + "\n" +
                "SPEED=" + (speed != null ? speed.trim() : "") + "\n" +
                "COURSE=" + (course != null ? course.trim() : "") + "\n" +
                "TIMESTAMP=" + (timestamp != null ? timestamp.trim() : "") + "\n";
    }

    public static boolean isValidMmsi(String mmsi) {
        if (mmsi == null) {
            return false;
        }
        String trimmed = mmsi.trim();
        return trimmed.matches("\\d{9}");
    }

    public static boolean hasValidFix(String latitude, String longitude) {
        return latitude != null && !latitude.trim().isEmpty() &&
                longitude != null && !longitude.trim().isEmpty();
    }
}
