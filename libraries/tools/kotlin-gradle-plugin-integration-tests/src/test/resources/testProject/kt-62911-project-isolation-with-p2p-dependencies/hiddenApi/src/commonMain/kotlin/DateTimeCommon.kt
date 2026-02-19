interface DateTime {
    fun format(pattern: String): String
    fun addDays(days: Int): DateTime
    fun isWeekend(): Boolean
    fun toISO8601String(): String

    companion object {
        fun now(): DateTime = createDateTimeInstance()
        fun parse(dateString: String, pattern: String): DateTime = parseDateTime(dateString, pattern)
        fun foo(): String = ""
    }
}

expect fun createDateTimeInstance(): DateTime
expect fun parseDateTime(dateString: String, pattern: String): DateTime