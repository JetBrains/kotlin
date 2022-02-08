/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.text

/**
 * Represents the character general category in the Unicode specification.
 */
public actual enum class CharCategory(public val value: Int, public actual val code: String) {
    /**
     * General category "Cn" in the Unicode specification.
     */
    UNASSIGNED(0, "Cn"),

    /**
     * General category "Lu" in the Unicode specification.
     */
    UPPERCASE_LETTER(1, "Lu"),

    /**
     * General category "Ll" in the Unicode specification.
     */
    LOWERCASE_LETTER(2, "Ll"),

    /**
     * General category "Lt" in the Unicode specification.
     */
    TITLECASE_LETTER(3, "Lt"),

    /**
     * General category "Lm" in the Unicode specification.
     */
    MODIFIER_LETTER(4, "Lm"),

    /**
     * General category "Lo" in the Unicode specification.
     */
    OTHER_LETTER(5, "Lo"),

    /**
     * General category "Mn" in the Unicode specification.
     */
    NON_SPACING_MARK(6, "Mn"),

    /**
     * General category "Me" in the Unicode specification.
     */
    ENCLOSING_MARK(7, "Me"),

    /**
     * General category "Mc" in the Unicode specification.
     */
    COMBINING_SPACING_MARK(8, "Mc"),

    /**
     * General category "Nd" in the Unicode specification.
     */
    DECIMAL_DIGIT_NUMBER(9, "Nd"),

    /**
     * General category "Nl" in the Unicode specification.
     */
    LETTER_NUMBER(10, "Nl"),

    /**
     * General category "No" in the Unicode specification.
     */
    OTHER_NUMBER(11, "No"),

    /**
     * General category "Zs" in the Unicode specification.
     */
    SPACE_SEPARATOR(12, "Zs"),

    /**
     * General category "Zl" in the Unicode specification.
     */
    LINE_SEPARATOR(13, "Zl"),

    /**
     * General category "Zp" in the Unicode specification.
     */
    PARAGRAPH_SEPARATOR(14, "Zp"),

    /**
     * General category "Cc" in the Unicode specification.
     */
    CONTROL(15, "Cc"),

    /**
     * General category "Cf" in the Unicode specification.
     */
    FORMAT(16, "Cf"),

    /**
     * General category "Co" in the Unicode specification.
     */
    PRIVATE_USE(18, "Co"),

    /**
     * General category "Cs" in the Unicode specification.
     */
    SURROGATE(19, "Cs"),

    /**
     * General category "Pd" in the Unicode specification.
     */
    DASH_PUNCTUATION(20, "Pd"),

    /**
     * General category "Ps" in the Unicode specification.
     */
    START_PUNCTUATION(21, "Ps"),

    /**
     * General category "Pe" in the Unicode specification.
     */
    END_PUNCTUATION(22, "Pe"),

    /**
     * General category "Pc" in the Unicode specification.
     */
    CONNECTOR_PUNCTUATION(23, "Pc"),

    /**
     * General category "Po" in the Unicode specification.
     */
    OTHER_PUNCTUATION(24, "Po"),

    /**
     * General category "Sm" in the Unicode specification.
     */
    MATH_SYMBOL(25, "Sm"),

    /**
     * General category "Sc" in the Unicode specification.
     */
    CURRENCY_SYMBOL(26, "Sc"),

    /**
     * General category "Sk" in the Unicode specification.
     */
    MODIFIER_SYMBOL(27, "Sk"),

    /**
     * General category "So" in the Unicode specification.
     */
    OTHER_SYMBOL(28, "So"),

    /**
     * General category "Pi" in the Unicode specification.
     */
    INITIAL_QUOTE_PUNCTUATION(29, "Pi"),

    /**
     * General category "Pf" in the Unicode specification.
     */
    FINAL_QUOTE_PUNCTUATION(30, "Pf");

    /**
     * Returns `true` if [char] character belongs to this category.
     */
    public actual operator fun contains(char: Char): Boolean = char.getCategoryValue() == this.value

    public companion object {
        public fun valueOf(category: Int): CharCategory =
                when (category) {
                    in 0..16 -> values()[category]
                    in 18..30 -> values()[category - 1]
                    else -> throw IllegalArgumentException("Category #$category is not defined.")
                }
    }
}