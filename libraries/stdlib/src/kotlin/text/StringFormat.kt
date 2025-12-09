/**
 * # Kotlin Common Formatting Library
 *
 * A lightweight, Kotlin Multiplatform-compatible string formatting utility
 * for common use cases. Provides simplified printf-style formatting.
 *
 * ## Overview
 * This library offers basic string formatting functionality similar to `printf`
 * in C and `String.format` in Java, but with a minimal footprint and focus on
 * common use cases in Kotlin Multiplatform projects.
 *
 * ## Quick Start
 * ```kotlin
 * // Basic usage
 * "Hello %s".format("World")                     // "Hello World"
 * "Value: %d".format(42)                         // "Value: 42"
 * "Price: %.2f".format(12.3456)                  // "Price: 12.35"
 * ```
 *
 * @see String.formatImpl for the main entry point
 */
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.round

// ============ Data Model ============

/**
 * Represents a parsed format specifier from a format string.
 *
 * This data class captures the essential components of a format specifier
 * in the pattern `%[width][.precision]type`.
 *
 * @property width Minimum field width for the formatted value.
 *   Positive integer or 0 for no minimum width. Default is 0.
 * @property precision Precision for floating-point numbers or maximum
 *   characters for strings. -1 indicates no precision specified.
 * @property type The conversion type character: 's', 'd', 'f', or 'c'.
 *
 * @throws IllegalArgumentException if type is not one of the supported types
 */
internal data class Specifier(
    val width: Int = 0,
    val precision: Int = -1,
    val type: Char
)

// ============ Parser ============

/**
 * Parses format strings into text segments and format specifiers.
 *
 * This parser processes format strings containing %-specifiers and extracts
 * them for later formatting. It supports a subset of printf-style format
 * specifiers commonly used in applications.
 *
 * ## Format String Syntax
 * Format strings may contain:
 * - Literal text: Passed through unchanged
 * - Format specifiers: `%[width][.precision]type`
 * - Escaped percent: `%%` produces a literal `%`
 *
 * ## Examples
 * ```
 * Input: "Hello %s, score: %d"
 * Output: [Text("Hello "), Specifier(0, -1, 's'), Text(", score: "), Specifier(0, -1, 'd')]
 * ```
 *
 * @param format The format string to parse
 * @throws IllegalArgumentException for malformed format strings
 *
 * @see Specifier for details on the parsed specifiers
 */
internal class FormatParser(private val format: String) {
    private var position = 0

    /**
     * Parses the format string into a list of text strings and specifiers.
     *
     * This method scans the format string sequentially, identifying
     * literal text and format specifiers. It returns a mixed list where
     * each element is either a String (literal text) or a Specifier.
     *
     * @return List of parsed parts in order of appearance
     * @throws IllegalArgumentException if the format string contains
     *   an incomplete or invalid specifier
     */
    fun parseParts(): List<Any> {
        val parts = mutableListOf<Any>()
        var textStart = 0

        while (position < format.length) {
            val char = format[position]

            if (char == '%') {
                // Save any text before this placeholder
                if (position > textStart) {
                    parts.add(format.substring(textStart, position))
                }
                position++ // Skip '%'

                if (position < format.length && format[position] == '%') {
                    // Handle escaped %
                    parts.add("%")
                    position++
                    textStart = position
                    continue
                }

                // Parse width
                var width = 0
                while (position < format.length && format[position].isDigit()) {
                    width = width * 10 + (format[position] - '0')
                    position++
                }

                // Parse precision
                var precision = -1
                if (position < format.length && format[position] == '.') {
                    position++ // Skip '.'
                    precision = 0
                    while (position < format.length && format[position].isDigit()) {
                        precision = precision * 10 + (format[position] - '0')
                        position++
                    }
                }

                // Parse type
                if (position >= format.length) {
                    throw IllegalArgumentException("Incomplete format specifier at end of string")
                }

                val type = format[position]
                position++

                // Validate type
                val validTypes = "sdfc"
                if (!validTypes.contains(type)) {
                    throw IllegalArgumentException(
                        "Unsupported format type '$type'. Supported types: s (string), d (integer), " +
                                "f (floating-point), c (character)"
                    )
                }

                parts.add(Specifier(width, precision, type))
                textStart = position
            } else {
                position++
            }
        }

        // Add any remaining text
        if (position > textStart) {
            parts.add(format.substring(textStart, position))
        }

        return parts
    }
}

// ============ Formatter ============

/**
 * Formats values according to specifiers.
 *
 * This object contains the formatting logic for converting values to strings
 * based on the provided specifiers. It handles type conversions, precision
 * adjustments, and width padding.
 *
 * ## Supported Value Types
 * - **Strings**: Any object (uses toString()), null becomes "null"
 * - **Integers**: Byte, Short, Int, Long, and unsigned variants
 * - **Floating-point**: Float, Double, or any Number
 * - **Characters**: Char or Number (converted via code point)
 *
 * @see Specifier for the format specifiers this formatter processes
 */
internal object Formatter {
    /** Default precision for floating-point numbers when not specified. */
    private const val DEFAULT_FLOAT_PRECISION = 6

    /** Small epsilon for floating-point stability in rounding. */
    private const val EPSILON = 1e-12

    /**
     * Formats a value according to the given specifier.
     *
     * This is the main formatting entry point that dispatches to type-specific
     * formatters based on the specifier type.
     *
     * @param value The value to format. May be null.
     * @param spec The format specifier controlling the output format.
     * @return Formatted string representation of the value.
     * @throws IllegalArgumentException if the value type is incompatible
     *   with the specifier type.
     * @throws AssertionError if an unsupported specifier type is encountered
     *   (should not happen with validated specifiers).
     */
    fun format(value: Any?, spec: Specifier): String {
        val base = when (spec.type) {
            's' -> formatString(value, spec.precision)
            'd' -> formatInt(value)
            'f' -> formatFloat(value, if (spec.precision >= 0) spec.precision else DEFAULT_FLOAT_PRECISION)
            'c' -> formatChar(value)
            else -> throw AssertionError("Unhandled type: ${spec.type}")
        }

        return applyWidth(base, spec.width)
    }

    /**
     * Formats a value as a string with optional truncation.
     *
     * @param value The value to convert to string.
     * @param precision Maximum characters to include. If -1 or longer than
     *   the string length, no truncation occurs.
     * @return The formatted string, or "null" if value is null.
     */
    private fun formatString(value: Any?, precision: Int): String {
        val str = value?.toString() ?: "null"
        return if (precision >= 0 && str.length > precision) {
            str.take(precision)
        } else {
            str
        }
    }

    /**
     * Formats a value as an integer.
     *
     * @param value The integer value to format.
     * @return String representation of the integer, or "null" if value is null.
     * @throws IllegalArgumentException if value is not an integer type.
     */
    private fun formatInt(value: Any?): String {
        return when (value) {
            null -> "null"
            is Byte, is Short, is Int, is Long,
            is UByte, is UShort, is UInt, is ULong
                -> value.toString()
            else -> throw IllegalArgumentException(
                "Expected integer type (Byte, Short, Int, Long, or unsigned variant) " +
                        "for %d, got ${value::class.simpleName}"
            )
        }
    }

    /**
     * Formats a value as a character.
     *
     * @param value The character value to format.
     * @return Single-character string, or "null" if value is null.
     * @throws IllegalArgumentException if value is not a Char or Number.
     */
    private fun formatChar(value: Any?): String {
        return when (value) {
            null -> "null"
            is Char -> value.toString()
            is Number -> value.toInt().toChar().toString()
            else -> throw IllegalArgumentException(
                "Expected Char or Number (for code point) for %c, got ${value::class.simpleName}"
            )
        }
    }

    /**
     * Formats a value as a floating-point number.
     *
     * @param value The floating-point value to format.
     * @param precision Number of decimal places to include.
     * @return Formatted floating-point string, or "null" if value is null.
     * @throws IllegalArgumentException if value is not a numeric type.
     */
    private fun formatFloat(value: Any?, precision: Int): String {
        if (precision < 0) {
            throw IllegalArgumentException("Precision cannot be negative: $precision")
        }

        return when (value) {
            null -> "null"
            is Float -> formatDouble(value.toDouble(), precision)
            is Double -> formatDouble(value, precision)
            is Number -> formatDouble(value.toDouble(), precision)
            else -> throw IllegalArgumentException(
                "Expected numeric type for %f, got ${value::class.simpleName}"
            )
        }
    }

    /**
     * Formats a Double value with specified precision.
     *
     * Handles special cases (NaN, Infinity) and formats finite numbers
     * with fixed-point notation.
     *
     * @param value The Double value to format.
     * @param precision Number of decimal places to include.
     * @return Formatted string representation.
     */
    private fun formatDouble(value: Double, precision: Int): String {
        // Handle special cases
        when {
            value.isNaN() -> return "NaN"
            value.isInfinite() -> return if (value > 0) "Infinity" else "-Infinity"
        }

        if (precision == 0) {
            // Round to nearest integer
            val rounded = round(value)
            return if (rounded == -0.0) "0" else rounded.toLong().toString()
        }

        // Simple fixed-point formatting
        val factor = 10.0.pow(precision)
        val scaled = round(value * factor + EPSILON)
        val integerPart = (scaled / factor).toLong()
        val fractionalPart = (scaled % factor).toLong()

        val sign = if (value < 0) "-" else ""
        val integerStr = integerPart.absoluteValue.toString()
        val fractionalStr = fractionalPart.absoluteValue.toString().padStart(precision, '0')

        return "$sign$integerStr.$fractionalStr"
    }

    /**
     * Applies minimum width padding to a string.
     *
     * If the string is shorter than the specified width, spaces are added
     * to the left to reach the minimum width (right-aligned formatting).
     *
     * @param text The string to pad.
     * @param width Minimum width requirement.
     * @return Padded string meeting minimum width, or original string if
     *   it already meets or exceeds the width.
     */
    private fun applyWidth(text: String, width: Int): String {
        if (width <= 0 || text.length >= width) {
            return text
        }

        val padding = " ".repeat(width - text.length)
        return padding + text
    }
}

// ============ Format Implementation ============

/**
 * Formats this string using the specified arguments.
 *
 * This function provides simplified printf-style formatting for common use
 * cases in Kotlin Multiplatform projects. It processes format strings
 * containing %-specifiers and replaces them with formatted argument values.
 *
 * ## Supported Format Specifiers
 *
 * | Specifier | Description                          | Example Input | Example Output |
 * |-----------|--------------------------------------|---------------|----------------|
 * | `%s`      | String                               | `"Hello %s"`  | `"Hello World"`|
 * | `%d`      | Integer (all integer types)          | `"Count: %d"` | `"Count: 42"`  |
 * | `%f`      | Floating-point (Float, Double)       | `"Value: %f"` | `"Value: 3.14"`|
 * | `%c`      | Character (Char or code point)       | `"Char: %c"`  | `"Char: A"`    |
 * | `%%`      | Literal percent sign                 | `"50%% done"` | `"50% done"`   |
 *
 * ## Width and Precision
 *
 * Format specifiers may include optional width and precision:
 *
 * ```kotlin
 * // Width: Minimum field width (right-aligned with spaces)
 * "%10s".format("test")      // "      test"
 * "%5d".format(123)          // "  123"
 *
 * // Precision: For strings (max length) or floats (decimal places)
 * "%.2f".format(3.14159)     // "3.14"
 * "%.5s".format("abcdefgh")  // "abcde"
 *
 * // Combined width and precision
 * "%8.2f".format(123.456)    // "  123.46"
 * ```
 *
 * ## Examples
 *
 * ```kotlin
 * // Basic formatting
 * "Hello %s".format("World")                     // "Hello World"
 * "Value: %d".format(42)                         // "Value: 42"
 * "Price: %.2f".format(12.3456)                  // "Price: 12.35"
 * "Name: %10s".format("John")                    // "Name:       John"
 * "Progress: %d%%".format(75)                    // "Progress: 75%"
 *
 * // Multiple arguments
 * "%s is %d years old".format("Alice", 30)       // "Alice is 30 years old"
 *
 * // Null values
 * "Value: %s".format(null)                       // "Value: null"
 * ```
 *
 * ## Error Handling
 *
 * This function throws [IllegalArgumentException] in the following cases:
 * - The format string contains malformed specifiers
 * - An argument type doesn't match its specifier
 * - Too few or too many arguments are provided
 *
 * @param args The arguments to substitute into format specifiers.
 * @return A formatted string with specifiers replaced by formatted arguments.
 * @throws IllegalArgumentException if the format is invalid, arguments don't
 *   match specifiers, or argument count is incorrect.
 */
internal fun String.formatImpl(args: Array<out Any?>): String {
    val parser = FormatParser(this)
    val parts = parser.parseParts()
    val output = StringBuilder()
    var argIndex = 0

    for (part in parts) {
        when (part) {
            is String -> output.append(part)
            is Specifier -> {
                if (argIndex >= args.size) {
                    throw IllegalArgumentException(
                        "Not enough arguments for format string. " +
                                "Expected at least ${argIndex + 1}, got ${args.size}."
                    )
                }
                val formatted = Formatter.format(args[argIndex], part)
                output.append(formatted)
                argIndex++
            }
        }
    }

    if (argIndex < args.size) {
        throw IllegalArgumentException(
            "Too many arguments for format string. " +
                    "Expected $argIndex, got ${args.size}."
        )
    }

    return output.toString()
}
