import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

class JvmDateTime(private val dateTime: LocalDateTime) : DateTime {

    override fun format(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return dateTime.format(formatter)
    }

    override fun addDays(days: Int): DateTime {
        return JvmDateTime(dateTime.plusDays(days.toLong()))
    }

    override fun isWeekend(): Boolean {
        return dateTime.dayOfWeek == DayOfWeek.SATURDAY || dateTime.dayOfWeek == DayOfWeek.SUNDAY
    }

    override fun toISO8601String(): String {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        return dateTime.format(formatter)
    }
}

actual fun createDateTimeInstance(): DateTime = JvmDateTime(LocalDateTime.now())

actual fun parseDateTime(dateString: String, pattern: String): DateTime {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return JvmDateTime(LocalDateTime.parse(dateString, formatter))
}