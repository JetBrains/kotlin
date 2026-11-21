/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Represents a date and time using the [JavaScript Date API](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date).
 *
 * On an invalid date, JavaScript methods that read date and time components return `NaN`.
 *
 * @constructor Creates a date representing the current time. See the [JavaScript constructor](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
 */
public external class Date() {
    /**
     * Creates a date from [milliseconds] since the Unix epoch.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(milliseconds: Number)

    /**
     * Creates a date by parsing [dateString] according to the JavaScript date parsing rules.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(dateString: String)

    /**
     * Creates a date in local time from [year] and [month]. [month] is zero-based.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(year: Int, month: Int)

    /**
     * Creates a date in local time from [year], [month] and [day]. [month] is zero-based.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(year: Int, month: Int, day: Int)

    /**
     * Creates a date in local time from [year], [month], [day] and [hour]. [month] is zero-based.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(year: Int, month: Int, day: Int, hour: Int)

    /**
     * Creates a date in local time from [year], [month], [day], [hour] and [minute]. [month] is zero-based.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int)

    /**
     * Creates a date in local time from [year], [month], [day], [hour], [minute] and [second]. [month] is zero-based.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int)

    /**
     * Creates a date in local time from [year], [month], [day], [hour], [minute], [second] and [millisecond]. [month] is zero-based.
     *
     * See [Date()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date).
     */
    public constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, millisecond: Number)

    /**
     * Returns the day of the month (1–31) in local time.
     *
     * See [Date.prototype.getDate()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getDate).
     */
    public fun getDate(): Int

    /**
     * Returns the day of the week (0 for Sunday through 6 for Saturday) in local time.
     *
     * See [Date.prototype.getDay()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getDay).
     */
    public fun getDay(): Int

    /**
     * Returns the year in local time.
     *
     * See [Date.prototype.getFullYear()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getFullYear).
     */
    public fun getFullYear(): Int

    /**
     * Returns the hour (0–23) in local time.
     *
     * See [Date.prototype.getHours()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getHours).
     */
    public fun getHours(): Int

    /**
     * Returns the millisecond (0–999) in local time.
     *
     * See [Date.prototype.getMilliseconds()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getMilliseconds).
     */
    public fun getMilliseconds(): Int

    /**
     * Returns the minute (0–59) in local time.
     *
     * See [Date.prototype.getMinutes()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getMinutes).
     */
    public fun getMinutes(): Int

    /**
     * Returns the month (0 for January through 11 for December) in local time.
     *
     * See [Date.prototype.getMonth()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getMonth).
     */
    public fun getMonth(): Int

    /**
     * Returns the second (0–59) in local time.
     *
     * See [Date.prototype.getSeconds()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getSeconds).
     */
    public fun getSeconds(): Int

    /**
     * Returns the number of milliseconds since the Unix epoch, or `NaN` for an invalid date.
     *
     * See [Date.prototype.getTime()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getTime).
     */
    public fun getTime(): Double

    /**
     * Returns the difference in minutes between UTC and local time for this date. It is negative east of UTC.
     *
     * See [Date.prototype.getTimezoneOffset()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getTimezoneOffset).
     */
    public fun getTimezoneOffset(): Int

    /**
     * Returns the day of the month (1–31) in UTC.
     *
     * See [Date.prototype.getUTCDate()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCDate).
     */
    public fun getUTCDate(): Int

    /**
     * Returns the day of the week (0 for Sunday through 6 for Saturday) in UTC.
     *
     * See [Date.prototype.getUTCDay()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCDay).
     */
    public fun getUTCDay(): Int

    /**
     * Returns the year in UTC.
     *
     * See [Date.prototype.getUTCFullYear()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCFullYear).
     */
    public fun getUTCFullYear(): Int

    /**
     * Returns the hour (0–23) in UTC.
     *
     * See [Date.prototype.getUTCHours()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCHours).
     */
    public fun getUTCHours(): Int

    /**
     * Returns the millisecond (0–999) in UTC.
     *
     * See [Date.prototype.getUTCMilliseconds()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCMilliseconds).
     */
    public fun getUTCMilliseconds(): Int

    /**
     * Returns the minute (0–59) in UTC.
     *
     * See [Date.prototype.getUTCMinutes()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCMinutes).
     */
    public fun getUTCMinutes(): Int

    /**
     * Returns the month (0 for January through 11 for December) in UTC.
     *
     * See [Date.prototype.getUTCMonth()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCMonth).
     */
    public fun getUTCMonth(): Int

    /**
     * Returns the second (0–59) in UTC.
     *
     * See [Date.prototype.getUTCSeconds()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getUTCSeconds).
     */
    public fun getUTCSeconds(): Int

    /**
     * Returns the date portion in the local time zone as an English-language string.
     *
     * See [Date.prototype.toDateString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toDateString).
     */
    public fun toDateString(): String

    /**
     * Returns an ISO 8601 date and time string in UTC. An invalid date causes a JavaScript `RangeError`.
     *
     * See [Date.prototype.toISOString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString).
     */
    public fun toISOString(): String

    /**
     * Returns the JavaScript JSON representation: an ISO date and time string in UTC, or `null` for an invalid date.
     *
     * See [Date.prototype.toJSON()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toJSON).
     */
    public fun toJSON(): Json

    /**
     * Formats the date using the requested [locales], in order of preference, and the formatting [options].
     *
     * See [Date.prototype.toLocaleDateString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleDateString).
     */
    public fun toLocaleDateString(locales: Array<String> = definedExternally, options: LocaleOptions = definedExternally): String

    /**
     * Formats the date using the locale specified by [locales] and the formatting [options].
     *
     * See [Date.prototype.toLocaleDateString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleDateString).
     */
    public fun toLocaleDateString(locales: String, options: LocaleOptions = definedExternally): String

    /**
     * Formats the date and time using the requested [locales], in order of preference, and the formatting [options].
     *
     * See [Date.prototype.toLocaleString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleString).
     */
    public fun toLocaleString(locales: Array<String> = definedExternally, options: LocaleOptions = definedExternally): String

    /**
     * Formats the date and time using the locale specified by [locales] and the formatting [options].
     *
     * See [Date.prototype.toLocaleString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleString).
     */
    public fun toLocaleString(locales: String, options: LocaleOptions = definedExternally): String

    /**
     * Formats the time using the requested [locales], in order of preference, and the formatting [options].
     *
     * See [Date.prototype.toLocaleTimeString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleTimeString).
     */
    public fun toLocaleTimeString(locales: Array<String> = definedExternally, options: LocaleOptions = definedExternally): String

    /**
     * Formats the time using the locale specified by [locales] and the formatting [options].
     *
     * See [Date.prototype.toLocaleTimeString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleTimeString).
     */
    public fun toLocaleTimeString(locales: String, options: LocaleOptions = definedExternally): String

    /**
     * Returns the time portion in the local time zone, including the time zone offset.
     *
     * See [Date.prototype.toTimeString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toTimeString).
     */
    public fun toTimeString(): String

    /**
     * Returns an English-language string representing this date and time in UTC.
     *
     * See [Date.prototype.toUTCString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toUTCString).
     */
    public fun toUTCString(): String

    /**
     * Provides access to the static methods of the JavaScript Date object.
     *
     * See [Date static methods](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date#static_methods).
     */
    public companion object {
        /**
         * Returns the current time in milliseconds since the Unix epoch.
         *
         * See [Date.now()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/now).
         */
        public fun now(): Double

        /**
         * Parses [dateString] and returns milliseconds since the Unix epoch, or `NaN` if parsing fails.
         *
         * See [Date.parse()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/parse).
         */
        public fun parse(dateString: String): Double

        /**
         * Returns milliseconds since the Unix epoch for the given UTC components. [month] is zero-based.
         *
         * See [Date.UTC()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/UTC).
         */
        public fun UTC(year: Int, month: Int): Double

        /**
         * Returns milliseconds since the Unix epoch for the given UTC components. [month] is zero-based.
         *
         * See [Date.UTC()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/UTC).
         */
        public fun UTC(year: Int, month: Int, day: Int): Double

        /**
         * Returns milliseconds since the Unix epoch for the given UTC components. [month] is zero-based.
         *
         * See [Date.UTC()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/UTC).
         */
        public fun UTC(year: Int, month: Int, day: Int, hour: Int): Double

        /**
         * Returns milliseconds since the Unix epoch for the given UTC components. [month] is zero-based.
         *
         * See [Date.UTC()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/UTC).
         */
        public fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int): Double

        /**
         * Returns milliseconds since the Unix epoch for the given UTC components. [month] is zero-based.
         *
         * See [Date.UTC()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/UTC).
         */
        public fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Double

        /**
         * Returns milliseconds since the Unix epoch for the given UTC components. [month] is zero-based.
         *
         * See [Date.UTC()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/UTC).
         */
        public fun UTC(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, millisecond: Number): Double
    }

    /**
     * Options for locale-sensitive date and time formatting.
     *
     * See [Intl.DateTimeFormat options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#options).
     */
    public interface LocaleOptions {
        /**
         * The algorithm used to match the requested locales to a supported locale.
         *
         * See [localeMatcher option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#localematcher).
         */
        public var localeMatcher: String?

        /**
         * The time zone used for formatting, such as `UTC` or `Europe/Paris`.
         *
         * See [timeZone option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#timezone).
         */
        public var timeZone: String?

        /**
         * Whether to use a 12-hour clock.
         *
         * See [hour12 option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#hour12).
         */
        public var hour12: Boolean?

        /**
         * The algorithm used to match the requested date and time components to a supported format.
         *
         * See [formatMatcher option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#formatmatcher).
         */
        public var formatMatcher: String?

        /**
         * The display style for the day of the week.
         *
         * See [weekday option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#weekday).
         */
        public var weekday: String?

        /**
         * The display style for the era.
         *
         * See [era option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#era).
         */
        public var era: String?

        /**
         * The display style for the year.
         *
         * See [year option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#year).
         */
        public var year: String?

        /**
         * The display style for the month.
         *
         * See [month option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#month).
         */
        public var month: String?

        /**
         * The display style for the day of the month.
         *
         * See [day option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#day).
         */
        public var day: String?

        /**
         * The display style for the hour.
         *
         * See [hour option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#hour).
         */
        public var hour: String?

        /**
         * The display style for the minute.
         *
         * See [minute option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#minute).
         */
        public var minute: String?

        /**
         * The display style for the second.
         *
         * See [second option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#second).
         */
        public var second: String?

        /**
         * The display style for the time zone name.
         *
         * See [timeZoneName option](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#timezonename).
         */
        public var timeZoneName: String?
    }
}

/**
 * Creates formatting options for the locale-sensitive methods of [Date] and configures them with [init].
 *
 * See [Intl.DateTimeFormat options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#options).
 */
public inline fun dateLocaleOptions(init: Date.LocaleOptions.() -> Unit): Date.LocaleOptions {
    val result = js("new Object()").unsafeCast<Date.LocaleOptions>()
    init(result)
    return result
}