/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.internal.InlineOnly

/**
 * Represents hexadecimal format options for formatting and parsing byte arrays and integer numeric values,
 * both signed and unsigned.
 *
 * An instance of this class is passed to formatting and parsing functions and specifies how formatting and parsing
 * should be conducted. The options of the [bytes] property apply only when formatting and parsing byte arrays,
 * while the [number] property applies when formatting and parsing numeric values. The [upperCase] option affects both.
 *
 * This class is immutable and cannot be created or configured directly. To create a new format, use the
 * `HexFormat { }` builder function and configure the options inside the braces. For example, use
 * `val format = HexFormat { upperCase = true }` to enable upper-case formatting.
 *
 * Two predefined instances are provided by this class's companion object: [Default] and [UpperCase].
 * The [Default] instance has the [upperCase] option set to `false`, and the options of [bytes] and [number] properties
 * set to their default values as specified in [BytesHexFormat] and [NumberHexFormat], respectively.
 * The [UpperCase] instance has the [upperCase] option set to `true`, and the options of [bytes] and [number] properties
 * set to their default values.
 *
 * @sample samples.text.HexFormats.HexFormatClass.hexFormatBuilderFunction
 *
 * @see ByteArray.toHexString
 * @see String.hexToByteArray
 * @see Int.toHexString
 * @see String.hexToInt
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public class HexFormat internal constructor(
    /**
     * Specifies whether upper-case hexadecimal digits should be used for formatting, `false` by default.
     *
     * When this option is set to `true`, formatting functions will use upper-case hexadecimal digits (`0-9`, `A-F`)
     * to create the hexadecimal representation of the values being formatted. Otherwise, lower-case hexadecimal digits
     * (`0-9`, `a-f`) will be used.
     *
     * This option affects the formatting results for both byte arrays and numeric values. However, it **has no effect
     * on parsing**, which is always performed in a case-insensitive manner.
     *
     * Note: This option **affects only the case of hexadecimal digits** and does not influence other elements like
     * [BytesHexFormat.bytePrefix] or [NumberHexFormat.suffix].
     *
     * @sample samples.text.HexFormats.HexFormatClass.upperCase
     */
    public val upperCase: Boolean,

    /**
     * Specifies the hexadecimal format used for formatting and parsing byte arrays.
     *
     * This property defines how byte arrays are formatted into hexadecimal strings and parsed back from strings into
     * byte arrays. It is utilized by [ByteArray.toHexString] for formatting and [String.hexToByteArray] for parsing.
     * These functions are available for [UByteArray] as well.
     *
     * Refer to [BytesHexFormat] for details about the available format options, their impact on formatting and
     * parsing results, and their default settings.
     */
    public val bytes: BytesHexFormat,

    /**
     * Specifies the hexadecimal format used for formatting and parsing numeric values.
     *
     * This property defines how numeric values are formatted into hexadecimal strings and parsed back from strings
     * into numeric values. It is utilized by functions like [Int.toHexString] for formatting and [String.hexToInt] for
     * parsing. These functions are available for all integer numeric types.
     *
     * Refer to [NumberHexFormat] for details about the available format options, their impact on formatting and
     * parsing results, and their default settings.
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
     * Represents hexadecimal format options for formatting and parsing byte arrays.
     *
     * These options are utilized by [ByteArray.toHexString] and [String.hexToByteArray] functions for formatting and
     * parsing, respectively. The formatting and parsing functions are available for [UByteArray] as well.
     *
     * When formatting a byte array, one can assume the following steps:
     * 1. The bytes are split into lines with [bytesPerLine] bytes in each line,
     *    except for the last line, which may have fewer bytes.
     * 2. Each line is split into groups with [bytesPerGroup] bytes in each group,
     *    except for the last group in a line, which may have fewer bytes.
     * 3. All bytes are converted to their two-digit hexadecimal representation,
     *    each prefixed by [bytePrefix] and suffixed by [byteSuffix].
     * 4. Adjacent formatted bytes within each group are separated by [byteSeparator].
     * 5. Adjacent groups within each line are separated by [groupSeparator].
     * 6. Adjacent lines are separated by the line feed (LF) character `'\n'`.
     *
     * For example, consider the snippet below:
     * ```kotlin
     * val byteArray = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
     * val format = HexFormat {
     *     bytes {
     *         bytesPerLine = 6
     *         bytesPerGroup = 4
     *         groupSeparator = "|" // vertical bar
     *         byteSeparator = " " // one space
     *         bytePrefix = "0x"
     *         byteSuffix = "" // empty string
     *     }
     * }
     *
     * println(byteArray.toHexString(format))
     * ```
     * Given the `byteArray` and `format`, the formatting proceeds as follows:
     * 1. The 8 bytes are split into lines of 6 bytes each. The first line contains `0, 1, 2, 3, 4, 5`,
     *    and the second (and last) line contains `6, 7`.
     * 2. Each line is then divided into groups of 4 bytes. The first line forms two groups: `0, 1, 2, 3` and `4, 5`;
     *    the second line forms one group: `6, 7`.
     * 3. Each byte is converted to its hexadecimal representation, prefixed by `"0x"` and suffixed by an empty string.
     * 4. Bytes within each group are separated by a single space `" "`.
     * 5. Groups within each line are separated by a vertical bar character `"|"`.
     * 6. Lines are separated by the LF character `'\n'`.
     *
     * The `byteArray.toHexString(format)` call will result in `"0x00 0x01 0x02 0x03|0x04 0x05\n0x06 0x07"`,
     * and printing it to the console outputs:
     * ```
     * 0x00 0x01 0x02 0x03|0x04 0x05
     * 0x06 0x07
     * ```
     *
     * When parsing, the input string must conform to the format specified by these options.
     * However, parsing is somewhat lenient, allowing any of the char sequences CRLF, LF, or CR to be used as the line
     * separator. Additionally, parsing of [groupSeparator], [byteSeparator], [bytePrefix], [byteSuffix], and the
     * hexadecimal digits is performed in a case-insensitive manner.
     *
     * This class is immutable and cannot be created or configured directly. To create a new format, use the
     * `HexFormat { }` builder function and configure the options of the `bytes` property inside the braces. For example,
     * use `val format = HexFormat { bytes.bytesPerLine = 16 }` to set the [bytesPerLine]. The `bytes` property is of
     * type [BytesHexFormat.Builder], whose options are configurable and correspond to the options of this class.
     */
    public class BytesHexFormat internal constructor(
        /**
         * The maximum number of bytes per line, [Int.MAX_VALUE] by default.
         *
         * When formatting, bytes are split into lines with [bytesPerLine] bytes in each line, except for the last
         * line, which may have fewer bytes if the total number of bytes does not divide evenly by this value. Adjacent
         * lines are separated by the line feed (LF) character `'\n'`. Note that if this value is greater than or equal
         * to the size of the byte array, the entire array will be formatted as a single line without any line breaks.
         *
         * When parsing, the input string must be split into lines accordingly, with [bytesPerLine] bytes in each line,
         * except for the last line, which may have fewer bytes. Any of the char sequences CRLF, LF, or CR
         * is considered a valid line separator.
         *
         * @sample samples.text.HexFormats.ByteArrays.bytesPerLine
         */
        public val bytesPerLine: Int,

        /**
         * The maximum number of bytes per group in a line, [Int.MAX_VALUE] by default.
         *
         * The number of bytes in each line is determined by the [bytesPerLine] option.
         *
         * When formatting, each line is split into groups with [bytesPerGroup] bytes in each group, except for the
         * last group in a line, which may have fewer bytes if the number of bytes in the line does not divide evenly
         * by this value. Adjacent groups within each line are separated by [groupSeparator]. Note that if this value
         * is greater than or equal to the number of bytes in a line, the bytes are not split into groups.
         *
         * When parsing, each line in the input string must be split into groups of [bytesPerGroup] bytes, except for
         * the last group in a line, which may have fewer bytes. Adjacent groups within each line must be separated by
         * [groupSeparator]. The parsing of the separator is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.ByteArrays.bytesPerGroup
         */
        public val bytesPerGroup: Int,

        /**
         * The string used to separate adjacent groups in a line, two space characters (`"  "`) by default.
         *
         * The number of bytes in each line and each group is determined by the [bytesPerLine] and
         * [bytesPerGroup] options, respectively.
         *
         * When formatting, adjacent groups within each line are separated by this string.
         *
         * When parsing, adjacent groups within each line must be separated by this string.
         * The parsing of this separator is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.ByteArrays.bytesPerGroup
         */
        public val groupSeparator: String,

        /**
         * The string used to separate adjacent bytes within a group.
         *
         * The number of bytes in each group is determined by the [bytesPerGroup] option.
         *
         * When formatting, adjacent bytes within each group are separated by this string.
         *
         * When parsing, adjacent bytes within each group must be separated by this string
         * The parsing of this separator is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.ByteArrays.byteSeparator
         */
        public val byteSeparator: String,

        /**
         * The string that immediately precedes the two-digit hexadecimal representation of each byte.
         *
         * When formatting, this string is used as a prefix for the hexadecimal representation of each byte.
         *
         * When parsing, the hexadecimal representation of each byte must be prefixed by this string.
         * The parsing of this prefix is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.ByteArrays.bytePrefix
         */
        public val bytePrefix: String,

        /**
         * The string that immediately follows the two-digit hexadecimal representation of each byte.
         *
         * When formatting, this string is used as a suffix for the hexadecimal representation of each byte.
         *
         * When parsing, the hexadecimal representation of each byte must be suffixed by this string.
         * The parsing of this suffix is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.ByteArrays.byteSuffix
         */
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
         * Provides an API for building a [BytesHexFormat].
         *
         * This class is a [builder](https://en.wikipedia.org/wiki/Builder_pattern) for [BytesHexFormat], and
         * serves as the type of the `bytes` property when creating a new format using the `HexFormat { }` builder
         * function. Each option in this class corresponds to an option in [BytesHexFormat] and defines it in the
         * resulting format. For example, use `val format = HexFormat { bytes.byteSeparator = true }` to set
         * [BytesHexFormat.byteSeparator]. Refer to [BytesHexFormat] for details about how the configured
         * format options affect formatting and parsing results.
         */
        public class Builder internal constructor() {
            /**
             * Defines [BytesHexFormat.bytesPerLine] of the format being built, [Int.MAX_VALUE] by default.
             *
             * The value must be positive.
             *
             * Refer to [BytesHexFormat.bytesPerLine] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a non-positive value is assigned to this property.
             *
             * @sample samples.text.HexFormats.ByteArrays.bytesPerLine
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
             * Refer to [BytesHexFormat.bytesPerGroup] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a non-positive value is assigned to this property.
             *
             * @sample samples.text.HexFormats.ByteArrays.bytesPerGroup
             */
            public var bytesPerGroup: Int = Default.bytesPerGroup
                set(value) {
                    if (value <= 0)
                        throw IllegalArgumentException("Non-positive values are prohibited for bytesPerGroup, but was $value")
                    field = value
                }

            /**
             * Defines [BytesHexFormat.groupSeparator] of the format being built, two space characters (`"  "`) by default.
             *
             * Refer to [BytesHexFormat.groupSeparator] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @sample samples.text.HexFormats.ByteArrays.bytesPerGroup
             */
            public var groupSeparator: String = Default.groupSeparator

            /**
             * Defines [BytesHexFormat.byteSeparator] of the format being built, empty string by default.
             *
             * The string must not contain line feed (LF) and carriage return (CR) characters.
             *
             * Refer to [BytesHexFormat.byteSeparator] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             *
             * @sample samples.text.HexFormats.ByteArrays.byteSeparator
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
             * The string must not contain line feed (LF) and carriage return (CR) characters.
             *
             * Refer to [BytesHexFormat.bytePrefix] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             *
             * @sample samples.text.HexFormats.ByteArrays.bytePrefix
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
             * The string must not contain line feed (LF) and carriage return (CR) characters.
             *
             * Refer to [BytesHexFormat.byteSuffix] for details about how this format option affects
             * the formatting and parsing results.
             *
             * @throws IllegalArgumentException if a string containing LF or CR character is assigned to this property.
             *
             * @sample samples.text.HexFormats.ByteArrays.byteSuffix
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
     * When parsing, the input string must start with the [prefix] and end with the [suffix]. It must contain at least
     * one hexadecimal digit between them. If the number of hexadecimal digits exceeds the capacity of the type being
     * parsed, based on its bit size, the excess leading digits must be zeros. Parsing of the [prefix], [suffix], and
     * hexadecimal digits is performed in a case-insensitive manner. The [removeLeadingZeros] and [minLength] options
     * are ignored during parsing.
     *
     * This class is immutable and cannot be created or configured directly. To create a new format, use the
     * `HexFormat { }` builder function and configure the options of the `number` property inside the braces. For
     * example, use `val format = HexFormat { number.prefix = "0x" }` to set the [prefix]. The `number` property is of
     * type [NumberHexFormat.Builder], whose options are configurable and correspond to the options of this class.
     */
    public class NumberHexFormat internal constructor(
        /**
         * The string that immediately precedes the hexadecimal representation of a numeric value,
         * empty string by default.
         *
         * When formatting, this string is placed before the hexadecimal representation.
         * When parsing, the string being parsed must start with this string.
         * The parsing of this prefix is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.Numbers.prefix
         */
        public val prefix: String,

        /**
         * The string that immediately succeeds the hexadecimal representation of a numeric value,
         * empty string by default.
         *
         * When formatting, this string is placed after the hexadecimal representation.
         * When parsing, the string being parsed must end with this string.
         * The parsing of this suffix is performed in a case-insensitive manner.
         *
         * @sample samples.text.HexFormats.Numbers.suffix
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
         * @sample samples.text.HexFormats.Numbers.removeLeadingZeros
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
         * When parsing, this option is ignored. However, there must be at least one hexadecimal digit in the input
         * string. If the number of hexadecimal digits exceeds the capacity of the type being parsed, based on its bit
         * size, the excess leading digits must be zeros.
         *
         * @sample samples.text.HexFormats.Numbers.minLength
         */
        @SinceKotlin("2.0")
        public val minLength: Int
    ) {

        internal val isDigitsOnly: Boolean = prefix.isEmpty() && suffix.isEmpty()

        internal val isDigitsOnlyAndNoPadding: Boolean = isDigitsOnly && minLength == 1

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
            sb.append(indent).append("removeLeadingZeros = ").append(removeLeadingZeros).appendLine(',')
            sb.append(indent).append("minLength = ").append(minLength)
            return sb
        }

        /**
         * Provides an API for building a [NumberHexFormat].
         *
         * This class is a [builder](https://en.wikipedia.org/wiki/Builder_pattern) for [NumberHexFormat], and
         * serves as the type of the `number` property when creating a new format using the `HexFormat { }` builder
         * function. Each option in this class corresponds to an option in [NumberHexFormat] and defines it in the
         * resulting format. For example, use `val format = HexFormat { number.removeLeadingZeros = true }` to set
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
             * @sample samples.text.HexFormats.Numbers.prefix
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
             * @sample samples.text.HexFormats.Numbers.suffix
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
             * @sample samples.text.HexFormats.Numbers.removeLeadingZeros
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
             * @sample samples.text.HexFormats.Numbers.minLength
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
     * Provides an API for building a [HexFormat].
     *
     * This class is a [builder](https://en.wikipedia.org/wiki/Builder_pattern) for [HexFormat], and
     * serves as the receiver type of the lambda expression used in the `HexFormat { }` builder function to create a
     * new format. Each option in this class corresponds to an option in [HexFormat] and defines it in the resulting
     * format. For example, use `val format = HexFormat { bytes.byteSeparator = ":" }` to set
     * [BytesHexFormat.byteSeparator] option of the [HexFormat.bytes] property.
     *
     * Refer to [HexFormat] for details about how the configured format options affect formatting and parsing results.
     *
     * @sample samples.text.HexFormats.HexFormatClass.hexFormatBuilderFunction
     */
    public class Builder @PublishedApi internal constructor() {
        /**
         * Defines [HexFormat.upperCase] of the format being built, `false` by default.
         *
         * Refer to [HexFormat.upperCase] for details about how the format option affects the formatting results.
         *
         * @sample samples.text.HexFormats.HexFormatClass.upperCase
         */
        public var upperCase: Boolean = Default.upperCase

        /**
         * Defines [HexFormat.bytes] of the format being built.
         *
         * Refer to [BytesHexFormat] for details about the available format options, their impact on formatting and
         * parsing results, and their default settings.
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
         * Refer to [NumberHexFormat] for details about the available format options, their impact on formatting and
         * parsing results, and their default settings.
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
         * Provides a scope for configuring the `bytes` property.
         *
         * The receiver of the [builderAction] is the `bytes` property. Thus, inside the braces one can configure its
         * options directly. This convenience function is intended to enable the configuration of multiple options
         * within a single block. For example, the snippet:
         * ```kotlin
         * val format = HexFormat {
         *     bytes {
         *         bytesPerLine = 16
         *         bytesPerGroup = 8
         *         byteSeparator = " "
         *     }
         * }
         * ```
         * is equivalent to:
         * ```kotlin
         * val format = HexFormat {
         *     bytes.bytesPerLine = 16
         *     bytes.bytesPerGroup = 8
         *     bytes.byteSeparator = " "
         * }
         * ```
         *
         * Refer to [BytesHexFormat] for details about the available format options, their impact on formatting and
         * parsing results, and their default settings.
         *
         * @param builderAction The function that is applied to the `bytes` property, providing a scope for directly
         *   configuring its options.
         */
        @InlineOnly
        public inline fun bytes(builderAction: BytesHexFormat.Builder.() -> Unit) {
            bytes.builderAction()
        }

        /**
         * Provides a scope for configuring the `number` property.
         *
         * The receiver of the [builderAction] is the `number` property. Thus, inside the braces one can configure its
         * options directly. This convenience function is intended to enable the configuration of multiple options
         * within a single block. For example, the snippet:
         * ```kotlin
         * val format = HexFormat {
         *     number {
         *         prefix = "&#x"
         *         suffix = ";"
         *         removeLeadingZeros = true
         *     }
         * }
         * ```
         * is equivalent to:
         * ```kotlin
         * val format = HexFormat {
         *     number.prefix = "&#x"
         *     number.suffix = ";"
         *     number.removeLeadingZeros = true
         * }
         * ```
         *
         * Refer to [NumberHexFormat] for details about the available format options, their impact on formatting
         * and parsing results, and their default settings.
         *
         * @param builderAction The function that is applied to the `number` property, providing a scope for directly
         *   configuring its options.
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
         * This [HexFormat] instance adopts default values for all format options.
         *
         * Formatting functions use lower-case hexadecimal digits (`0-9`, `a-f`) for both byte arrays and
         * numeric values. Specifically, [upperCase] is set to `false`.
         *
         * No line separator, group separator, byte separator, byte prefix, or byte suffix is used
         * when formatting or parsing byte arrays. Specifically:
         *   * [bytes.bytesPerLine][BytesHexFormat.bytesPerLine] is set to `Int.MAX_VALUE`.
         *   * [bytes.bytesPerGroup][BytesHexFormat.bytesPerGroup] is set to `Int.MAX_VALUE`.
         *   * [bytes.byteSeparator][BytesHexFormat.byteSeparator], [bytes.bytePrefix][BytesHexFormat.bytePrefix],
         *   and [bytes.byteSuffix][BytesHexFormat.byteSuffix] are empty strings.
         *
         * No prefix or suffix is used, and leading zeros are not removed from the hexadecimal representation
         * when formatting or parsing numeric values. Specifically:
         *   * [number.prefix][NumberHexFormat.prefix] and [number.suffix][NumberHexFormat.suffix] are empty strings.
         *   * [number.removeLeadingZeros][NumberHexFormat.removeLeadingZeros] is set to `false`.
         */
        public val Default: HexFormat = HexFormat(
            upperCase = false,
            bytes = BytesHexFormat.Default,
            number = NumberHexFormat.Default,
        )

        /**
         * The hexadecimal format options configured to use upper-case hexadecimal digits.
         *
         * This [HexFormat] instance adopts default values for all format options, except for [upperCase],
         * which is set to `true`. As a result, formatting functions will use upper-case hexadecimal digits
         * (`0-9`, `A-F`) for both byte arrays and numeric values.
         *
         * In all other aspects, this format is identical to the [Default] format.
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
 * The resulting format can be passed to formatting and parsing functions to dictate how formatting and parsing
 * should be conducted. For example, `val format = HexFormat { number.prefix = "0x" }` creates a new [HexFormat] and
 * assigns it to the `format` variable. When this `format` is passed to a number formatting function, the resulting
 * string will include `"0x"` as the prefix of the hexadecimal representation of the numeric value being formatted.
 * For instance, calling `58.toHexString(format)` will produce `"0x0000003a"`.
 *
 * Refer to [HexFormat.Builder] for details on configuring format options, and see [HexFormat] for
 * information on how the configured format options affect formatting and parsing results.
 *
 * The builder provided as a receiver to the [builderAction] is valid only within that function.
 * Using it outside the function can produce an unspecified behavior.
 *
 * @param builderAction The function that configures the format options of the [HexFormat.Builder] receiver.
 * @return A new instance of [HexFormat] configured as specified by the [builderAction].
 *
 * @sample samples.text.HexFormats.HexFormatClass.hexFormatBuilderFunction
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