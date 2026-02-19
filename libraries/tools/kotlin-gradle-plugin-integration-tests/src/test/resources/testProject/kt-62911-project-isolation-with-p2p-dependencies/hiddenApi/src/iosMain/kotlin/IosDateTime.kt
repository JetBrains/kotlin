import platform.Foundation.*

class IosDateTime(private val date: NSDate) : DateTime {

    private lateinit var wednesda: Wednesda

    override fun format(pattern: String): String {
        val formatter = NSDateFormatter().apply { dateFormat = pattern }
        return formatter.stringFromDate(date)
    }

    override fun addDays(days: Int): DateTime {
        val newDate = NSCalendar.currentCalendar.dateByAddingUnit(
            NSCalendarUnitDay,
            value = days.toLong(),
            toDate = date,
            options = 0u
        )!!
        return IosDateTime(newDate)
    }

    override fun isWeekend(): Boolean {
        val components = NSCalendar.currentCalendar.components(NSCalendarUnitWeekday, fromDate = date)
        return components.weekday == 1L || components.weekday == 7L
    }

    override fun toISO8601String(): String {
        val formatter = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd'T'HH:mm:ssZZZZZ" }
        return formatter.stringFromDate(date)
    }
}

actual fun createDateTimeInstance(): DateTime = IosDateTime(NSDate())

actual fun parseDateTime(dateString: String, pattern: String): DateTime {
    val formatter = NSDateFormatter().apply { dateFormat = pattern }
    return IosDateTime(formatter.dateFromString(dateString)!!)
}