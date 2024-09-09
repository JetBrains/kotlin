import kotlin.js.Date

class JsDateTime(private val date: Date) : DateTime {

    override fun format(pattern: String): String {
        // Implement a simple format method or use a library like `moment.js`
        return date.toLocaleString()
    }

    override fun addDays(days: Int): DateTime {
        val newDate = Date(date.getTime() + days * 86400000)  // 86400000 ms in a day
        return JsDateTime(newDate)
    }

    override fun isWeekend(): Boolean {
        val day = date.getDay()
        return day == 0 || day == 6
    }

    override fun toISO8601String(): String {
        return date.toISOString()
    }
}

actual fun createDateTimeInstance(): DateTime = JsDateTime(Date())

actual fun parseDateTime(dateString: String, pattern: String): DateTime {
    return JsDateTime(Date(dateString))
}