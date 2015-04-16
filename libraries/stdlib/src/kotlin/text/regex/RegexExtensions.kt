package kotlin

import kotlin.text.*

/**
 * Converts the string into a regular expression [Regex] with the default options.
 */
public fun String.toRegex(): Regex = Regex(this)

/**
 * Converts the string into a regular expression [Regex] with the specified single [option].
 */
public fun String.toRegex(option: RegexOption): Regex = Regex(this, option)

/**
 * Converts the string into a regular expression [Regex] with the specified set of [options].
 */
public fun String.toRegex(options: Set<RegexOption>): Regex = Regex(this, options)
