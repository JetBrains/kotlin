package kotlin.js

/**
 * Exposes the [Date API](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date) to Kotlin.
 */
public external class Date() {

    constructor(timestamp: Long)
    constructor(dateString: String)
    constructor(year: Int,
                month: Int = definedExternally,
                date: Int = definedExternally,
                hours: Int = definedExternally,
                minutes: Int = definedExternally,
                seconds: Int = definedExternally,
                millis: Int = definedExternally)

    public fun getDate(): Int = definedExternally
    public fun getDay(): Int = definedExternally
    public fun getFullYear(): Int = definedExternally
    public fun getHours(): Int = definedExternally
    public fun getMilliseconds(): Int = definedExternally
    public fun getMinutes(): Int = definedExternally
    public fun getSeconds(): Int = definedExternally
    public fun getTime(): Long = definedExternally

    public fun getTimezoneOffset(): Int = definedExternally
    public fun getUTCDate(): Int = definedExternally
    public fun getUTCDay(): Int = definedExternally
    public fun getUTCFullYear(): Int = definedExternally
    public fun getUTCHours(): Int = definedExternally
    public fun getUTCMilliseconds(): Int = definedExternally
    public fun getUTCMinutes(): Int = definedExternally
    public fun getUTCMonth(): Int = definedExternally
    public fun getUTCSeconds(): Int = definedExternally

    @Deprecated("Returns the year (usually 2-3 digits) in the specified date according to local time.",
                ReplaceWith("getFullYear"))
    public fun getYear(): Int = definedExternally



    public fun setDate(date: Int): Long = definedExternally
    public fun setFullYear(year: Int,
                           month: Int = definedExternally,
                           day: Int = definedExternally): Long = definedExternally
    public fun setHours(hours: Int,
                        minutes: Int = definedExternally,
                        seconds: Int = definedExternally,
                        millis: Int = definedExternally): Long = definedExternally
    public fun setMilliseconds(millis: Int): Long = definedExternally
    public fun setMinutes(minutes: Int,
                          seconds: Int = definedExternally,
                          millis: Int = definedExternally): Long = definedExternally
    public fun setMonth(month: Int,
                        day: Int = definedExternally)
    public fun setSeconds(seconds: Int,
                          millis: Int = definedExternally): Long = definedExternally
    public fun setTime(timeMillis: Long): Long = definedExternally
    public fun setUTCDate(day: Int): Long = definedExternally
    public fun setUTCFullYear(year: Int,
                              month: Int,
                              day: Int): Long = definedExternally
    public fun setUTCHours(hours: Int,
                           minutes: Int = definedExternally,
                           seconds: Int = definedExternally,
                           millis: Int): Long = definedExternally
    public fun setUTCMilliseconds(millis: Int): Long = definedExternally
    public fun setUTCMinutes(minutes: Int,
                             seconds: Int = definedExternally,
                             millis: Int = definedExternally): Long = definedExternally
    public fun setUTCMonth(month: Int,
                           day: Int = definedExternally): Long = definedExternally
    public fun setUTCSeconds(seconds: Int,
                             millis: Int = definedExternally): Long = definedExternally

    @Deprecated("Be aware that this feature may cease to work at any time.", ReplaceWith("setFullYear"))
    public fun setYear(year: Int): Long = definedExternally



    public fun toDateString(): String = definedExternally
    @Deprecated("Be aware that this feature may cease to work at any time.", ReplaceWith("toUTCString"))
    public fun toGMTString(): String = definedExternally
    public fun toISOString(): String = definedExternally
    public fun toJSON(): String = definedExternally
    public fun toLocaleDateString(locales: dynamic = definedExternally, /* String | Array<String> */
                                  options: dynamic = definedExternally): String = definedExternally
    public fun toLocaleString(locales: dynamic = definedExternally, /* String | Array<String> */
                              options: dynamic = definedExternally): String = definedExternally
    public fun toLocaleTimeString(locales: dynamic = definedExternally, /* String | Array<String> */
                                  options: dynamic = definedExternally): String = definedExternally
    public override fun toString(): String = definedExternally
    public fun toTimeString(): String = definedExternally
    public fun toUTCString(): String = definedExternally
    public fun valueOf(): Long = definedExternally

    companion object {
        fun now(): Long = definedExternally
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Parsing of strings is strongly discouraged due to browser differences and inconsistencies.")
        fun parse(date: String): Long = definedExternally
        fun UTC(year: Int,
                month: Int = definedExternally,
                date: Int = definedExternally,
                hours: Int = definedExternally,
                minutes: Int = definedExternally,
                seconds: Int = definedExternally,
                millis: Int = definedExternally): Long = definedExternally
    }

}
