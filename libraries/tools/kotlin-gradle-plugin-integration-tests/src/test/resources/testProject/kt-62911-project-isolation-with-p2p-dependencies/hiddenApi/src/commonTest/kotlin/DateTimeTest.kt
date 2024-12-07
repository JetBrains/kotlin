import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateTimeTest {

    @Test
    fun testDateTimeNow() {
        val now = DateTime.now()
        // Assuming we have a method toISO8601String to check the output
        assertTrue(now.toISO8601String().isNotEmpty(), "DateTime.now() should return a valid date string")
    }

    @Test
    @Ignore
    fun testFormat() {
        val date = DateTime.parse("2024-08-09T15:30:00", "yyyy-MM-dd'T'HH:mm:ss")
        val formattedDate = date.format("dd-MM-yyyy")
        assertEquals("09-08-2024", formattedDate, "Date should be formatted correctly")
    }

    @Test
    @Ignore
    fun testAddDays() {
        val date = DateTime.parse("2024-08-09T15:30:00", "yyyy-MM-dd'T'HH:mm:ss")
        val newDate = date.addDays(5)
        val formattedDate = newDate.format("yyyy-MM-dd")
        assertEquals("2024-08-14", formattedDate, "Adding 5 days should result in the correct date")
    }

    @Test
    fun testIsWeekend() {
        val saturday = DateTime.parse("2024-08-10T00:00:00", "yyyy-MM-dd'T'HH:mm:ss")
        assertTrue(saturday.isWeekend(), "Date should be recognized as a weekend")

        val tuesday = DateTime.parse("2024-08-13T00:00:00", "yyyy-MM-dd'T'HH:mm:ss")
        assertTrue(!tuesday.isWeekend(), "Date should not be recognized as a weekend")
    }
}