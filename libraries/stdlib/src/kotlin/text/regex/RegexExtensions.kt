@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

/**
 * Converts the string into a regular expression [Regex] with the default options.
 */
@kotlin.internal.InlineOnly
public inline fun String.toRegex(): Regex = Regex(this)

/**
 * Converts the string into a regular expression [Regex] with the specified single [option].
 */
@kotlin.internal.InlineOnly
public inline fun String.toRegex(option: RegexOption): Regex = Regex(this, option)

/**
 * Converts the string into a regular expression [Regex] with the specified set of [options].
 */
@kotlin.internal.InlineOnly
public inline fun String.toRegex(options: Set<RegexOption>): Regex = Regex(this, options)

/**
 * Converts this [java.util.regex.Pattern] to an instance of [Regex].
 *
 * Provides the way to use Regex API on the instances of [java.util.regex.Pattern].
 */
@JvmVersion
@kotlin.internal.InlineOnly
public inline fun java.util.regex.Pattern.toRegex(): Regex = Regex(this)