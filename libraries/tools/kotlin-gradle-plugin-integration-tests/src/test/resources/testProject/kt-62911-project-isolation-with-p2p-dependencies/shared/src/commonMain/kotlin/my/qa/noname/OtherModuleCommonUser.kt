package my.qa.noname

import DateTime

class OtherModuleCommonUser : DateTime {
    override fun format(pattern: String): String {
        return  "smth"
    }

    override fun addDays(days: Int): DateTime {
        return this
    }

    override fun isWeekend(): Boolean {
        return true
    }

    override fun toISO8601String(): String {
       return "true"
    }

}