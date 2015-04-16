package kotlin

import kotlin.properties.Delegates

/**
 * Represents the character general category in the Unicode specification.
 */
public enum class CharCategory(public val value: Int, public val code: String) {
    /**
     * General category "Cn" in the Unicode specification.
     */
    UNASSIGNED: CharCategory(Character.UNASSIGNED.toInt(), "Cn")

    /**
     * General category "Lu" in the Unicode specification.
     */
    UPPERCASE_LETTER: CharCategory(Character.UPPERCASE_LETTER.toInt(), "Lu")

    /**
     * General category "Ll" in the Unicode specification.
     */
    LOWERCASE_LETTER: CharCategory(Character.LOWERCASE_LETTER.toInt(), "Ll")

    /**
     * General category "Lt" in the Unicode specification.
     */
    TITLECASE_LETTER: CharCategory(Character.TITLECASE_LETTER.toInt(), "Lt")

    /**
     * General category "Lm" in the Unicode specification.
     */
    MODIFIER_LETTER: CharCategory(Character.MODIFIER_LETTER.toInt(), "Lm")

    /**
     * General category "Lo" in the Unicode specification.
     */
    OTHER_LETTER: CharCategory(Character.OTHER_LETTER.toInt(), "Lo")

    /**
     * General category "Mn" in the Unicode specification.
     */
    NON_SPACING_MARK: CharCategory(Character.NON_SPACING_MARK.toInt(), "Mn")

    /**
     * General category "Me" in the Unicode specification.
     */
    ENCLOSING_MARK: CharCategory(Character.ENCLOSING_MARK.toInt(), "Me")

    /**
     * General category "Mc" in the Unicode specification.
     */
    COMBINING_SPACING_MARK: CharCategory(Character.COMBINING_SPACING_MARK.toInt(), "Mc")

    /**
     * General category "Nd" in the Unicode specification.
     */
    DECIMAL_DIGIT_NUMBER: CharCategory(Character.DECIMAL_DIGIT_NUMBER.toInt(), "Nd")

    /**
     * General category "Nl" in the Unicode specification.
     */
    LETTER_NUMBER: CharCategory(Character.LETTER_NUMBER.toInt(), "Nl")

    /**
     * General category "No" in the Unicode specification.
     */
    OTHER_NUMBER: CharCategory(Character.OTHER_NUMBER.toInt(), "No")

    /**
     * General category "Zs" in the Unicode specification.
     */
    SPACE_SEPARATOR: CharCategory(Character.SPACE_SEPARATOR.toInt(), "Zs")

    /**
     * General category "Zl" in the Unicode specification.
     */
    LINE_SEPARATOR: CharCategory(Character.LINE_SEPARATOR.toInt(), "Zl")

    /**
     * General category "Zp" in the Unicode specification.
     */
    PARAGRAPH_SEPARATOR: CharCategory(Character.PARAGRAPH_SEPARATOR.toInt(), "Zp")

    /**
     * General category "Cc" in the Unicode specification.
     */
    CONTROL: CharCategory(Character.CONTROL.toInt(), "Cc")

    /**
     * General category "Cf" in the Unicode specification.
     */
    FORMAT: CharCategory(Character.FORMAT.toInt(), "Cf")

    /**
     * General category "Co" in the Unicode specification.
     */
    PRIVATE_USE: CharCategory(Character.PRIVATE_USE.toInt(), "Co")

    /**
     * General category "Cs" in the Unicode specification.
     */
    SURROGATE: CharCategory(Character.SURROGATE.toInt(), "Cs")

    /**
     * General category "Pd" in the Unicode specification.
     */
    DASH_PUNCTUATION: CharCategory(Character.DASH_PUNCTUATION.toInt(), "Pd")

    /**
     * General category "Ps" in the Unicode specification.
     */
    START_PUNCTUATION: CharCategory(Character.START_PUNCTUATION.toInt(), "Ps")

    /**
     * General category "Pe" in the Unicode specification.
     */
    END_PUNCTUATION: CharCategory(Character.END_PUNCTUATION.toInt(), "Pe")

    /**
     * General category "Pc" in the Unicode specification.
     */
    CONNECTOR_PUNCTUATION: CharCategory(Character.CONNECTOR_PUNCTUATION.toInt(), "Pc")

    /**
     * General category "Po" in the Unicode specification.
     */
    OTHER_PUNCTUATION: CharCategory(Character.OTHER_PUNCTUATION.toInt(), "Po")

    /**
     * General category "Sm" in the Unicode specification.
     */
    MATH_SYMBOL: CharCategory(Character.MATH_SYMBOL.toInt(), "Sm")

    /**
     * General category "Sc" in the Unicode specification.
     */
    CURRENCY_SYMBOL: CharCategory(Character.CURRENCY_SYMBOL.toInt(), "Sc")

    /**
     * General category "Sk" in the Unicode specification.
     */
    MODIFIER_SYMBOL: CharCategory(Character.MODIFIER_SYMBOL.toInt(), "Sk")

    /**
     * General category "So" in the Unicode specification.
     */
    OTHER_SYMBOL: CharCategory(Character.OTHER_SYMBOL.toInt(), "So")

    /**
     * General category "Pi" in the Unicode specification.
     */
    INITIAL_QUOTE_PUNCTUATION: CharCategory(Character.INITIAL_QUOTE_PUNCTUATION.toInt(), "Pi")

    /**
     * General category "Pf" in the Unicode specification.
     */
    FINAL_QUOTE_PUNCTUATION: CharCategory(Character.FINAL_QUOTE_PUNCTUATION.toInt(), "Pf")


    public companion object {
        private val categoryMap by Delegates.lazy { CharCategory.values().toMap { it.value } }

        public fun valueOf(category: Int): CharCategory = categoryMap[category] ?: throw IllegalArgumentException("Category #$category is not defined.")
    }
}