package se.helmer.arctoolbox;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PositionReportHelperTest {

    @Test
    public void testSpeedConversionToKnots() {
        assertEquals(0.0f, PositionReportHelper.toKnots(0.0f), 0.001f);
        // 1 m/s is ~1.943844 knots
        assertEquals(1.943844f, PositionReportHelper.toKnots(1.0f), 0.001f);
        assertEquals(19.43844f, PositionReportHelper.toKnots(10.0f), 0.01f);
    }

    @Test
    public void testFormatCoordinate() {
        assertEquals("59.32932", PositionReportHelper.formatCoordinate(59.329323));
        assertEquals("-18.12346", PositionReportHelper.formatCoordinate(-18.123456));
    }

    @Test
    public void testFormatSpeedKnots() {
        assertEquals("0", PositionReportHelper.formatSpeedKnots(0.0f));
        assertEquals("5.7", PositionReportHelper.formatSpeedKnots(5.74f));
        assertEquals("12.8", PositionReportHelper.formatSpeedKnots(12.79f));
    }

    @Test
    public void testFormatCourse() {
        assertEquals("0", PositionReportHelper.formatCourse(0.0f));
        assertEquals("180", PositionReportHelper.formatCourse(180.2f));
        assertEquals("271", PositionReportHelper.formatCourse(270.8f));
    }

    @Test
    public void testDateFormattingUtc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.OCTOBER, 12, 14, 30, 45);
        Date testDate = cal.getTime();

        String utcTime = PositionReportHelper.formatUtcTime(testDate);
        assertEquals("2026-10-12 14:30", utcTime);

        String iso8601 = PositionReportHelper.formatIso8601(testDate);
        assertEquals("2026-10-12T14:30:45Z", iso8601);
    }

    @Test
    public void testBuildReport() {
        String report = PositionReportHelper.buildReport(
                "265123456",
                "59.32932",
                "18.06858",
                "7.5",
                "180",
                "2026-10-12 14:30"
        );

        String expected = "MMSI=265123456\n" +
                "LAT=59.32932\n" +
                "LONG=18.06858\n" +
                "SPEED=7.5\n" +
                "COURSE=180\n" +
                "TIMESTAMP=2026-10-12 14:30\n";

        assertEquals(expected, report);
    }

    @Test
    public void testBuildReportHandlesNulls() {
        String report = PositionReportHelper.buildReport(null, null, null, null, null, null);
        String expected = "MMSI=\nLAT=\nLONG=\nSPEED=\nCOURSE=\nTIMESTAMP=\n";
        assertEquals(expected, report);
    }

    @Test
    public void testIsValidMmsi() {
        assertTrue(PositionReportHelper.isValidMmsi("265123456"));
        assertTrue(PositionReportHelper.isValidMmsi(" 265123456 "));
        assertFalse(PositionReportHelper.isValidMmsi("12345"));
        assertFalse(PositionReportHelper.isValidMmsi("2651234567"));
        assertFalse(PositionReportHelper.isValidMmsi("26512345A"));
        assertFalse(PositionReportHelper.isValidMmsi(""));
        assertFalse(PositionReportHelper.isValidMmsi(null));
    }

    @Test
    public void testHasValidFix() {
        assertTrue(PositionReportHelper.hasValidFix("59.123", "18.456"));
        assertFalse(PositionReportHelper.hasValidFix("", "18.456"));
        assertFalse(PositionReportHelper.hasValidFix("59.123", ""));
        assertFalse(PositionReportHelper.hasValidFix(null, "18.456"));
        assertFalse(PositionReportHelper.hasValidFix("   ", "18.456"));
    }
}
