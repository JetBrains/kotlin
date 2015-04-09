package kotlin

import kotlin.text.*
/**
 * Converts the string into a regular expression [Regex] with the specified [options].
 */
public fun String.toRegex(vararg options: RegexOption): Regex = Regex(this, *options)

/**
 * Converts the string into a regular expression [Regex] with the specified set of [options].
 */
public fun String.toRegex(options: Set<RegexOption>): Regex = Regex(this, options)
