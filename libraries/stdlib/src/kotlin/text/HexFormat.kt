/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.internal.InlineOnly

/**
 * Represents hexadecimal format options.
 *
 * To create a new [HexFormat] use `HexFormat` function.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public class HexFormat internal constructor(
    /**
     * Specifies whether upper case hexadecimal digits `0-9`, `A-F` should be used for formatting.
     * If `false`, lower case hexadecimal digits `0-9`, `a-f` will be used.
     *
     * Affects both `ByteArray` and numeric value formatting.
     */
    public val upperCase: Boolean,
    /**
     * Specifies hexadecimal format used for formatting and parsing `ByteArray`.
     */
    public val bytes: BytesHexFormat,
    /**
     * Specifies hexadecimal format used for formatting and parsing a numeric value.
     */
    public val number: NumberHexFormat
) {

    override fun toString(): String = buildString {
        append("HexFormat(").appendLine()
        append("    upperCase = ").append(upperCase).appendLine(",")
        append("    bytes = BytesHexFormat(").appendLine()
        bytes.appendOptionsTo(this, indent = "        ").appendLine()
        append("    ),").appendLine()
        append("    number = NumberHexFormat(").appendLine()
        number.appendOptionsTo(this, indent = "        ").appendLine()
        append("    )").appendLine()
        append(")")
    }

    /**
     * Represents hexadecimal format options for formatting and parsing `ByteArray`.
     *
     * When formatting one can assume that bytes are firstly separated using LF character (`'\n'`) into lines
     * with [bytesPerLine] bytes in each line. The last line may have fewer bytes.
     * Then each line is separated into groups using [groupSeparator] with [bytesPerGroup] bytes in each group,
     * except the last group in the line, which may have fewer bytes.
     * All bytes in a group are separated using [byteSeparator].
     * Each byte is converted to its two-digit hexadecimal representation,
     * immediately preceded by [bytePrefix] and immediately succeeded by [byteSuffix].
     *
     * When parsing the input string is required to be in the format described above.
     * However, any of the char sequences CRLF, LF and CR is considered a valid line separator,
     * and parsing is performed in case-insensitive manner.
     *
     * See [BytesHexFormat.Builder] to find out how the options are configured,
     * and what is the default value of each option.
     */
    public class BytesHexFormat internal constructor(
        /** The maximum number of bytes per line. */
        public val bytesPerLine: Int,

        /** The maximum number of bytes per group. */
        public val bytesPerGroup: Int,
        /** The string used to separate adjacent groups in a line. */
        public val groupSeparator: String,

        /** The string used to separate adjacent bytes in a group. */
        public val byteSeparator: String,
        /** The string that immediately precedes two-digit hexadecimal representation of each byte. */
        public val bytePrefix: String,
        /** The string that immediately succeeds two-digit hexadecimal representation of each byte. */
        public val byteSuffix: String
    ) {

        internal val noLineAndGroupSeparator: Boolean =
            bytesPerLine == Int.MAX_VALUE && bytesPerGroup == Int.MAX_VALUE

        internal val shortByteSeparatorNoPrefixAndSuffix: Boolean =
            bytePrefix.isEmpty() && byteSuffix.isEmpty() && byteSeparator.length <= 1

        /**
         * Whether to ignore case when parsing format strings.
         * If false, case-sensitive parsing is conducted, which is faster.
         */
        internal val ignoreCase: Boolean =
            groupSeparator.isCaseSensitive() ||
                    byteSeparator.isCaseSensitive() ||
                    bytePrefix.isCaseSensitive() ||
                    byteSuffix.isCaseSensitive()

        override fun toString(): String = buildString {
            append("BytesHexFormat(").appendLine()
            appendOptionsTo(this, indent = "    ").appendLine()
            append(")")
        }

        internal fun appendOptionsTo(sb: StringBuilder, indent: String): StringBuilder {
            sb.append(indent).append("bytesPerLine = ").append(bytesPerLine).appendLine(",")
            sb.append(indent).append("bytesPerGroup = ").append(bytesPerGroup).appendLine(",")
            sb.append(indent).append("groupSeparator = \"").append(groupSeparator).appendLine("\",")
            sb.append(indent).append("byteSeparator = \"").append(byteSeparator).appendLine("\",")
            sb.append(indent).append("bytePrefix = \"").append(bytePrefix).appendLine("\",")
            sb.append(indent).append("byteSuffix = \"").append(byteSuffix).append("\"")
            return sb
        }

        /**
         * A context for building a [BytesHexFormat]. Provides API for configuring format options.
         */
        public class Builder internal constructor() {
            /**
             * Defines [BytesHexFormat.bytesPerLine] of the format being built, [Int.MAX_VALUE] by default.
             *
             * The value must be positive.
             *
             * @throws IllegalArgumentException if a non-positive value is assigned to this property.
             */
            public var bytesPerLine: Int = Default.bytesPerLine
                set(value) {
                    if (value <= 0)
                        throw IllegalArgumentException("Non-positive values are prohibited for bytesPerLine, but was $value")
                    field = value
                }

            /**
             * Defines [BytesHexFormat.bytesPerGroup] of the format being built, [Int.MAX_VALUE] by default.
             *
             * The value must be positive.
             *
             * @throws IllegalArgumentException if a non-positive value is assigned to this property.
             */
            public var bytesPerGroup: Int = Default.bytesPerGroup
                set(value) {
                    if (value <= 0)
                        throw IllegalArgumentException("Non-positive values are prohibited for bytesPerGroup, but was $value")
                    field = value
                }

            /** Defines [BytesHexFormat.groupSeparator] of the format being built, two space characters (`"  "`) by default. */
            public var groupSeparator: String = Default.groupSeparator

            /**
             * Defines [BytesHexFormat.byteSeparator] of the format being built, empty string by default.
             *
             * The string must not contain LF and CR characters.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             */
            public var byteSeparator: String = Default.byteSeparator
                set(value) {
                    if (value.contains('\n') || value.contains('\r'))
                        throw IllegalArgumentException("LF and CR characters are prohibited in byteSeparator, but was $value")
                    field = value
                }

            /**
             * Defines [BytesHexFormat.bytePrefix] of the format being built, empty string by default.
             *
             * The string must not contain LF and CR characters.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             */
            public var bytePrefix: String = Default.bytePrefix
                set(value) {
                    if (value.contains('\n') || value.contains('\r'))
                        throw IllegalArgumentException("LF and CR characters are prohibited in bytePrefix, but was $value")
                    field = value
                }

            /**
             * Defines [BytesHexFormat.byteSuffix] of the format being built, empty string by default.
             *
             * The string must not contain LF and CR characters.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             */
            public var byteSuffix: String = Default.byteSuffix
                set(value) {
                    if (value.contains('\n') || value.contains('\r'))
                        throw IllegalArgumentException("LF and CR characters are prohibited in byteSuffix, but was $value")
                    field = value
                }

            internal fun build(): BytesHexFormat {
                return BytesHexFormat(bytesPerLine, bytesPerGroup, groupSeparator, byteSeparator, bytePrefix, byteSuffix)
            }
        }

        internal companion object {
            internal val Default = BytesHexFormat(
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparator = "  ",
                byteSeparator = "",
                bytePrefix = "",
                byteSuffix = ""
            )
        }
    }

    /**
     * Represents hexadecimal format options for formatting and parsing numeric values.
     *
     * These options are utilized by functions like [Int.toHexString] for formatting and [String.hexToInt] for parsing.
     * The formatting and parsing functions are available for all integer numeric types.
     *
     * When formatting, the result consists of a [prefix] string, the hexadecimal representation of the numeric value,
     * and a [suffix] string. The hexadecimal representation of a value is calculated by mapping each four-bit chunk
     * of its binary representation to the corresponding hexadecimal digit, starting with the most significant bits.
     * The [upperCase] option determines the case (`A-F` or `a-f`) of the hexadecimal digits.
     * If [removeLeadingZeros] is `true` and the hexadecimal representation is longer than [minLength], leading zeros
     * are removed until the length matches [minLength]. However, if [minLength] exceeds the length of the hexadecimal
     * representation, [removeLeadingZeros] is ignored, and zeros are added to the start of the representation to
     * achieve the specified [minLength].
     *
     * For example, the binary representation of the `Int` value `58` (32-bit long `00000000000000000000000000111010`)
     * converts to the hexadecimal representation `0000003a` or `0000003A`, depending on [upperCase].
     * With [removeLeadingZeros] set to `true`, it shortens to `3a`. However, if [minLength] is set to `6`, the removal of
     * leading zeros stops once the length is reduced to `00003a`. Setting [minLength] to `12` results in
     * `00000000003a`, where the [removeLeadingZeros] option is ignored due to the minimum length requirement.
     *
     * To format a value into a hexadecimal string of a particular length, start by converting the value to a type with
     * the suitable bit size. For instance, to format an `Int` value into a 4-digit hexadecimal string, convert the value
     * using `toShort()` before hexadecimal formatting. To obtain a maximum of 4 digits without leading zeros,
     * additionally set [removeLeadingZeros] to `true`.
     *
     * When parsing, the input string must start with the [prefix] and end with the [suffix]. The number of hexadecimal
     * digits in the input string must either exactly match [minLength] or not exceed the value's bit size divided
     * by four. Parsing of [prefix], [suffix], and the hexadecimal digits is performed in a case-insensitive manner.
     * Also, the [removeLeadingZeros] option is ignored when parsing.
     *
     * This class is immutable and cannot be created or configured directly. To create a new format, use the
     * `HexFormat { }` function and configure the options of the `number` property inside the braces. For example,
     * use `val format = HexFormat { number.prefix = "0x" }` to set the [prefix]. The `number` property is of type
     * [NumberHexFormat.Builder], whose options are configurable and correspond to the options of this class.
     */
    public class NumberHexFormat internal constructor(
        /**
         * The string that immediately precedes the hexadecimal representation of a numeric value,
         * empty string by default.
         *
         * When formatting, this string is placed before the hexadecimal representation.
         * When parsing, the string being parsed must start with this string, ignoring character case.
         * Otherwise, the parsing function will fail.
         *
         * @sample samples.text.HexFormats.Numbers.Formatting.prefix
         * @sample samples.text.HexFormats.Numbers.Parsing.prefix
         */
        public val prefix: String,

        /**
         * The string that immediately succeeds the hexadecimal representation of a numeric value,
         * empty string by default.
         *
         * When formatting, this string is placed after the hexadecimal representation.
         * When parsing, the string being parsed must end with this string, ignoring character case.
         * Otherwise, the parsing function will fail.
         *
         * @sample samples.text.HexFormats.Numbers.Formatting.suffix
         * @sample samples.text.HexFormats.Numbers.Parsing.suffix
         */
        public val suffix: String,

        /**
         * Specifies whether to remove leading zeros in the hexadecimal representation of a numeric value,
         * `false` by default.
         *
         * The hexadecimal representation of a value is calculated by mapping each four-bit chunk of its binary
         * representation to the corresponding hexadecimal digit, starting with the most significant bits.
         *
         * When formatting, if this option is `true` and the length of the hexadecimal representation exceeds
         * [minLength], leading zeros are removed until the length matches [minLength]. If the length
         * does not exceed [minLength], this option has no effect on the formatting result.
         *
         * When parsing, this option is ignored.
         *
         * @sample samples.text.HexFormats.Numbers.Formatting.removeLeadingZeros
         * @sample samples.text.HexFormats.Numbers.Parsing.removeLeadingZeros
         */
        public val removeLeadingZeros: Boolean,

        /**
         * Specifies the minimum number of hexadecimal digits to be used in the representation of a numeric value,
         * `1` by default.
         *
         * The hexadecimal representation of a value is calculated by mapping each four-bit chunk of its binary
         * representation to the corresponding hexadecimal digit, starting with the most significant bits.
         *
         * When formatting:
         *   - If this option is less than the length of the hexadecimal representation:
         *       - If [removeLeadingZeros] is `true`, leading zeros are removed until the length matches [minLength].
         *       - If [removeLeadingZeros] is `false`, the representation remains unchanged, as no leading zeros are removed.
         *   - If this option is greater than the length of the hexadecimal representation, the
         *     representation is padded with zeros at the start to reach the specified [minLength].
         *   - If this option matches the length of the hexadecimal representation, the representation remains unchanged.
         *
         * When parsing:
         *   - If this option is less than the length of the hexadecimal representation, the number of hexadecimal
         *     digits in the input string must be at least [minLength] and not exceed the representation's length.
         *   - If this option is greater than or equal to the length of the hexadecimal representation,
         *     the number of hexadecimal digits in the input string must exactly match [minLength].
         *
         * The parsing function fails if these conditions are not met.
         *
         * @sample samples.text.HexFormats.Numbers.Formatting.minLength
         * @sample samples.text.HexFormats.Numbers.Parsing.minLength
         */
        @SinceKotlin("2.0")
        public val minLength: Int
    ) {

        internal val isDigitsOnly: Boolean = prefix.isEmpty() && suffix.isEmpty()

        /**
         * Whether to ignore case when parsing format strings.
         * If false, case-sensitive parsing is conducted, which is faster.
         */
        internal val ignoreCase: Boolean = prefix.isCaseSensitive() || suffix.isCaseSensitive()

        override fun toString(): String = buildString {
            append("NumberHexFormat(").appendLine()
            appendOptionsTo(this, indent = "    ").appendLine()
            append(")")
        }

        internal fun appendOptionsTo(sb: StringBuilder, indent: String): StringBuilder {
            sb.append(indent).append("prefix = \"").append(prefix).appendLine("\",")
            sb.append(indent).append("suffix = \"").append(suffix).appendLine("\",")
            sb.append(indent).append("removeLeadingZeros = ").append(removeLeadingZeros)
            return sb
        }

        /**
         * Provides an API for building a [NumberHexFormat].
         *
         * This class is a [builder](https://en.wikipedia.org/wiki/Builder_pattern) for [NumberHexFormat], and
         * serves as the type of the `number` property when creating a new format using the `HexFormat { }` function.
         * Each option in this class corresponds to an option in [NumberHexFormat] and defines it in the resulting
         * format. For example, use `val format = HexFormat { number.removeLeadingZeros = true }` to set
         * [NumberHexFormat.removeLeadingZeros]. Refer to [NumberHexFormat] for details about how the configured
         * format options affect formatting and parsing results.
         */
        public class Builder internal constructor() {
            /**
             * Defines [NumberHexFormat.prefix] of the format being built, empty string by default.
             *
             * The string must not contain line feed (LF) and carriage return (CR) characters.
             *
             * Refer to [NumberHexFormat.prefix] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             *
             * @sample samples.text.HexFormats.Numbers.Formatting.prefix
             * @sample samples.text.HexFormats.Numbers.Parsing.prefix
             */
            public var prefix: String = Default.prefix
                set(value) {
                    if (value.contains('\n') || value.contains('\r'))
                        throw IllegalArgumentException("LF and CR characters are prohibited in prefix, but was $value")
                    field = value
                }

            /**
             * Defines [NumberHexFormat.suffix] of the format being built, empty string by default.
             *
             * The string must not contain line feed (LF) and carriage return (CR) characters.
             *
             * Refer to [NumberHexFormat.suffix] for details about how the format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             *
             * @sample samples.text.HexFormats.Numbers.Formatting.suffix
             * @sample samples.text.HexFormats.Numbers.Parsing.suffix
             */
            public var suffix: String = Default.suffix
                set(value) {
                    if (value.contains('\n') || value.contains('\r'))
                        throw IllegalArgumentException("LF and CR characters are prohibited in suffix, but was $value")
                    field = value
                }

            /**
             * Defines [NumberHexFormat.removeLeadingZeros] of the format being built, `false` by default.
             *
             * Refer to [NumberHexFormat.removeLeadingZeros] for details about how the format option affects
             * the formatting and parsing results.
             *
             * @sample samples.text.HexFormats.Numbers.Formatting.removeLeadingZeros
             * @sample samples.text.HexFormats.Numbers.Parsing.removeLeadingZeros
             */
            public var removeLeadingZeros: Boolean = Default.removeLeadingZeros

            /**
             * Defines [NumberHexFormat.minLength] of the format being built, `1` by default.
             *
             * The value must be positive.
             *
             * Refer to [NumberHexFormat.minLength] for details about how the format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a non-positive value is assigned to this property.
             *
             * @sample samples.text.HexFormats.Numbers.Formatting.minLength
             * @sample samples.text.HexFormats.Numbers.Parsing.minLength
             */
            @SinceKotlin("2.0")
            public var minLength: Int = Default.minLength
                set(value) {
                    require(value > 0) { "Non-positive values are prohibited for minLength, but was $value" }
                    field = value
                }

            internal fun build(): NumberHexFormat {
                return NumberHexFormat(prefix, suffix, removeLeadingZeros, minLength)
            }
        }

        internal companion object {
            internal val Default = NumberHexFormat(
                prefix = "",
                suffix = "",
                removeLeadingZeros = false,
                minLength = 1
            )
        }
    }


    /**
     * A context for building a [HexFormat]. Provides API for configuring format options.
     */
    public class Builder @PublishedApi internal constructor() {
        /** Defines [HexFormat.upperCase] of the format being built, `false` by default. */
        public var upperCase: Boolean = Default.upperCase

        /**
         * Defines [HexFormat.bytes] of the format being built.
         *
         * See [BytesHexFormat.Builder] for default values of the options.
         */
        public val bytes: BytesHexFormat.Builder
            get() {
                if (_bytes == null) {
                    _bytes = BytesHexFormat.Builder()
                }
                return _bytes!!
            }

        private var _bytes: BytesHexFormat.Builder? = null

        /**
         * Defines [HexFormat.number] of the format being built.
         *
         * See [NumberHexFormat.Builder] for default values of the options.
         */
        public val number: NumberHexFormat.Builder
            get() {
                if (_number == null) {
                    _number = NumberHexFormat.Builder()
                }
                return _number!!
            }

        private var _number: NumberHexFormat.Builder? = null

        /**
         * Provides a scope for configuring the [HexFormat.bytes] format options.
         *
         * See [BytesHexFormat.Builder] for default values of the options.
         */
        @InlineOnly
        public inline fun bytes(builderAction: BytesHexFormat.Builder.() -> Unit) {
            bytes.builderAction()
        }

        /**
         * Provides a scope for configuring the [HexFormat.number] format options.
         *
         * See [NumberHexFormat.Builder] for default values of the options.
         */
        @InlineOnly
        public inline fun number(builderAction: NumberHexFormat.Builder.() -> Unit) {
            number.builderAction()
        }

        @PublishedApi
        internal fun build(): HexFormat {
            return HexFormat(
                upperCase,
                _bytes?.build() ?: BytesHexFormat.Default,
                _number?.build() ?: NumberHexFormat.Default
            )
        }
    }


    public companion object {
        /**
         * The default hexadecimal format options.
         *
         * Uses lower case hexadecimal digits `0-9`, `a-f` when formatting
         * both `ByteArray` and numeric values. That is [upperCase] is `false`.
         *
         * No line separator, group separator, byte separator, byte prefix or byte suffix is used
         * when formatting or parsing `ByteArray`. That is:
         *   * [BytesHexFormat.bytesPerLine] is `Int.MAX_VALUE`.
         *   * [BytesHexFormat.bytesPerGroup] is `Int.MAX_VALUE`.
         *   * [BytesHexFormat.byteSeparator], [BytesHexFormat.bytePrefix] and [BytesHexFormat.byteSuffix] are empty strings.
         *
         * No prefix or suffix is used, and no leading zeros in hexadecimal representation are removed
         * when formatting or parsing a numeric value. That is:
         *   * [NumberHexFormat.prefix] and [NumberHexFormat.suffix] are empty strings.
         *   * [NumberHexFormat.removeLeadingZeros] is `false`.
         */
        public val Default: HexFormat = HexFormat(
            upperCase = false,
            bytes = BytesHexFormat.Default,
            number = NumberHexFormat.Default,
        )

        /**
         * Uses upper case hexadecimal digits `0-9`, `A-F` when formatting
         * both `ByteArray` and numeric values. That is [upperCase] is `true`.
         *
         * The same as [Default] format in other aspects.
         */
        public val UpperCase: HexFormat = HexFormat(
            upperCase = true,
            bytes = BytesHexFormat.Default,
            number = NumberHexFormat.Default,
        )
    }
}

/**
 * Builds a new [HexFormat] by configuring its format options using the specified [builderAction],
 * and returns the resulting format.
 *
 * The builder passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside the function produces an unspecified behavior.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun HexFormat(builderAction: HexFormat.Builder.() -> Unit): HexFormat {
    return HexFormat.Builder().apply(builderAction).build()
}

// --- private functions ---

private fun String.isCaseSensitive(): Boolean {
    return this.any { it >= '\u0080' || it.isLetter() }
}