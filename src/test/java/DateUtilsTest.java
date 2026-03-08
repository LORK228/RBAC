import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class DateUtilsTest {

    @Test
    public void testCurrentDateFormats() {
        String date = DateUtils.getCurrentDate();
        String dt = DateUtils.getCurrentDateTime();

        assertEquals(10, date.length());
        assertEquals('-', date.charAt(4));
        assertEquals('-', date.charAt(7));

        assertEquals(19, dt.length());
        assertEquals(' ', dt.charAt(10));
    }

    @Test
    public void testBeforeAfter() {
        assertTrue(DateUtils.isBefore("2026-01-01", "2026-01-02"));
        assertTrue(DateUtils.isAfter("2026-01-03", "2026-01-02"));
        assertFalse(DateUtils.isAfter("2026-01-01", "2026-01-02"));
    }

    @Test
    public void testAddDays() {
        assertEquals("2026-01-11", DateUtils.addDays("2026-01-10", 1));
    }

    @Test
    public void testFormatRelativeTime() {
        String today = DateUtils.getCurrentDate();
        assertEquals("today", DateUtils.formatRelativeTime(today));
    }
}
