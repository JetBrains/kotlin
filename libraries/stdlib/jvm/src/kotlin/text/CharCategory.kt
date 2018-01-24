@file:JvmVersion
package kotlin.text

import kotlin.*

/**
 * Represents the character general category in the Unicode specification.
 */
public enum class CharCategory(public val value: Int, public val code: String) {
    /**
     * General category "Cn" in the Unicode specification.
     */
    UNASSIGNED(Character.UNASSIGNED.toInt(), "Cn"),

    /**
     * General category "Lu" in the Unicode specification.
     */
    UPPERCASE_LETTER(Character.UPPERCASE_LETTER.toInt(), "Lu"),

    /**
     * General category "Ll" in the Unicode specification.
     */
    LOWERCASE_LETTER(Character.LOWERCASE_LETTER.toInt(), "Ll"),

    /**
     * General category "Lt" in the Unicode specification.
     */
    TITLECASE_LETTER(Character.TITLECASE_LETTER.toInt(), "Lt"),

    /**
     * General category "Lm" in the Unicode specification.
     */
    MODIFIER_LETTER(Character.MODIFIER_LETTER.toInt(), "Lm"),

    /**
     * General category "Lo" in the Unicode specification.
     */
    OTHER_LETTER(Character.OTHER_LETTER.toInt(), "Lo"),

    /**
     * General category "Mn" in the Unicode specification.
     */
    NON_SPACING_MARK(Character.NON_SPACING_MARK.toInt(), "Mn"),

    /**
     * General category "Me" in the Unicode specification.
     */
    ENCLOSING_MARK(Character.ENCLOSING_MARK.toInt(), "Me"),

    /**
     * General category "Mc" in the Unicode specification.
     */
    COMBINING_SPACING_MARK(Character.COMBINING_SPACING_MARK.toInt(), "Mc"),

    /**
     * General category "Nd" in the Unicode specification.
     */
    DECIMAL_DIGIT_NUMBER(Character.DECIMAL_DIGIT_NUMBER.toInt(), "Nd"),

    /**
     * General category "Nl" in the Unicode specification.
     */
    LETTER_NUMBER(Character.LETTER_NUMBER.toInt(), "Nl"),

    /**
     * General category "No" in the Unicode specification.
     */
    OTHER_NUMBER(Character.OTHER_NUMBER.toInt(), "No"),

    /**
     * General category "Zs" in the Unicode specification.
     */
    SPACE_SEPARATOR(Character.SPACE_SEPARATOR.toInt(), "Zs"),

    /**
     * General category "Zl" in the Unicode specification.
     */
    LINE_SEPARATOR(Character.LINE_SEPARATOR.toInt(), "Zl"),

    /**
     * General category "Zp" in the Unicode specification.
     */
    PARAGRAPH_SEPARATOR(Character.PARAGRAPH_SEPARATOR.toInt(), "Zp"),

    /**
     * General category "Cc" in the Unicode specification.
     */
    CONTROL(Character.CONTROL.toInt(), "Cc"),

    /**
     * General category "Cf" in the Unicode specification.
     */
    FORMAT(Character.FORMAT.toInt(), "Cf"),

    /**
     * General category "Co" in the Unicode specification.
     */
    PRIVATE_USE(Character.PRIVATE_USE.toInt(), "Co"),

    /**
     * General category "Cs" in the Unicode specification.
     */
    SURROGATE(Character.SURROGATE.toInt(), "Cs"),

    /**
     * General category "Pd" in the Unicode specification.
     */
    DASH_PUNCTUATION(Character.DASH_PUNCTUATION.toInt(), "Pd"),

    /**
     * General category "Ps" in the Unicode specification.
     */
    START_PUNCTUATION(Character.START_PUNCTUATION.toInt(), "Ps"),

    /**
     * General category "Pe" in the Unicode specification.
     */
    END_PUNCTUATION(Character.END_PUNCTUATION.toInt(), "Pe"),

    /**
     * General category "Pc" in the Unicode specification.
     */
    CONNECTOR_PUNCTUATION(Character.CONNECTOR_PUNCTUATION.toInt(), "Pc"),

    /**
     * General category "Po" in the Unicode specification.
     */
    OTHER_PUNCTUATION(Character.OTHER_PUNCTUATION.toInt(), "Po"),

    /**
     * General category "Sm" in the Unicode specification.
     */
    MATH_SYMBOL(Character.MATH_SYMBOL.toInt(), "Sm"),

    /**
     * General category "Sc" in the Unicode specification.
     */
    CURRENCY_SYMBOL(Character.CURRENCY_SYMBOL.toInt(), "Sc"),

    /**
     * General category "Sk" in the Unicode specification.
     */
    MODIFIER_SYMBOL(Character.MODIFIER_SYMBOL.toInt(), "Sk"),

    /**
     * General category "So" in the Unicode specification.
     */
    OTHER_SYMBOL(Character.OTHER_SYMBOL.toInt(), "So"),

    /**
     * General category "Pi" in the Unicode specification.
     */
    INITIAL_QUOTE_PUNCTUATION(Character.INITIAL_QUOTE_PUNCTUATION.toInt(), "Pi"),

    /**
     * General category "Pf" in the Unicode specification.
     */
    FINAL_QUOTE_PUNCTUATION(Character.FINAL_QUOTE_PUNCTUATION.toInt(), "Pf");

    /**
     * Returns `true` if [char] character belongs to this category.
     */
    public operator fun contains(char: Char): Boolean = Character.getType(char) == this.value


    public companion object {
        private val categoryMap by lazy { CharCategory.values().associateBy { it.value } }

        public fun valueOf(category: Int): CharCategory = categoryMap[category] ?: throw IllegalArgumentException("Category #$category is not defined.")
    }
}