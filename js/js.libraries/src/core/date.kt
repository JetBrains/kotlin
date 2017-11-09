package kotlin.js

/**
 * Exposes the [Date API](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date) to Kotlin.
 */
public external class Date() {
    public constructor(milliseconds: Number)

    public constructor(dateString: String)

    public constructor(year: Int, month: Int)

    public constructor(year: Int, month: Int, day: Int)

    public constructor(year: Int, month: Int, day: Int, hour: Int)

    public constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int)

    public constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int)

    public constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, millisecond: Number)

    public fun getDate(): Int

    public fun getDay(): Int

    public fun getFullYear(): Int

    public fun getHours(): Int

    public fun getMilliseconds(): Int

    public fun getMinutes(): Int

    public fun getMonth(): Int

    public fun getSeconds(): Int

    public fun getTime(): Double

    public fun getTimezoneOffset(): Int

    public fun getUTCDate(): Int

    public fun getUTCDay(): Int

    public fun getUTCFullYear(): Int

    public fun getUTCHours(): Int

    public fun getUTCMilliseconds(): Int

    public fun getUTCMinutes(): Int

    public fun getUTCMonth(): Int

    public fun getUTCSeconds(): Int

    public fun toDateString(): String

    public fun toISOString(): String

    public fun toJSON(): Json

    public fun toLocaleDateString(locales: Array<String> = definedExternally, options: LocaleOptions = definedExternally): String

    public fun toLocaleDateString(locales: String, options: LocaleOptions = definedExternally): String

    public fun toLocaleString(locales: Array<String> = definedExternally, options: LocaleOptions = definedExternally): String

    public fun toLocaleString(locales: String, options: LocaleOptions = definedExternally): String

    public fun toLocaleTimeString(locales: Array<String> = definedExternally, options: LocaleOptions = definedExternally): String

    public fun toLocaleTimeString(locales: String, options: LocaleOptions = definedExternally): String

    public fun toTimeString(): String

    public fun toUTCString(): String

    public companion object {
        public fun now(): Double

        public fun parse(dateString: String): Double

        public fun UTC(year: Int, month: Int): Double

        public fun UTC(year: Int, month: Int, day: Int): Double

        public fun UTC(year: Int, month: Int, day: Int, hour: Int): Double

        public fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int): Double

        public fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Double

        public fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, millisecond: Number): Double
    }

    public interface LocaleOptions {
        public var localeMatcher: String?

        public var timeZone: String?

        public var hour12: Boolean?

        public var formatMatcher: String?

        public var weekday: String?

        public var era: String?

        public var year: String?

        public var month: String?

        public var day: String?

        public var hour: String?

        public var minute: String?

        public var second: String?

        public var timeZoneName: String?
    }
}

public inline fun dateLocaleOptions(init: Date.LocaleOptions.() -> Unit): Date.LocaleOptions {
    val result = js("new Object()").unsafeCast<Date.LocaleOptions>()
    init(result)
    return result
}