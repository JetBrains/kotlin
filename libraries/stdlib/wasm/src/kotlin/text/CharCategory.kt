/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Represents the character general category in the Unicode specification.
 */
public actual enum class CharCategory {
    /**
     * General category "Cn" in the Unicode specification.
     */
    UNASSIGNED,

    /**
     * General category "Lu" in the Unicode specification.
     */
    UPPERCASE_LETTER,

    /**
     * General category "Ll" in the Unicode specification.
     */
    LOWERCASE_LETTER,

    /**
     * General category "Lt" in the Unicode specification.
     */
    TITLECASE_LETTER,

    /**
     * General category "Lm" in the Unicode specification.
     */
    MODIFIER_LETTER,

    /**
     * General category "Lo" in the Unicode specification.
     */
    OTHER_LETTER,

    /**
     * General category "Mn" in the Unicode specification.
     */
    NON_SPACING_MARK,

    /**
     * General category "Me" in the Unicode specification.
     */
    ENCLOSING_MARK,

    /**
     * General category "Mc" in the Unicode specification.
     */
    COMBINING_SPACING_MARK,

    /**
     * General category "Nd" in the Unicode specification.
     */
    DECIMAL_DIGIT_NUMBER,

    /**
     * General category "Nl" in the Unicode specification.
     */
    LETTER_NUMBER,

    /**
     * General category "No" in the Unicode specification.
     */
    OTHER_NUMBER,

    /**
     * General category "Zs" in the Unicode specification.
     */
    SPACE_SEPARATOR,

    /**
     * General category "Zl" in the Unicode specification.
     */
    LINE_SEPARATOR,

    /**
     * General category "Zp" in the Unicode specification.
     */
    PARAGRAPH_SEPARATOR,

    /**
     * General category "Cc" in the Unicode specification.
     */
    CONTROL,

    /**
     * General category "Cf" in the Unicode specification.
     */
    FORMAT,

    /**
     * General category "Co" in the Unicode specification.
     */
    PRIVATE_USE,

    /**
     * General category "Cs" in the Unicode specification.
     */
    SURROGATE,

    /**
     * General category "Pd" in the Unicode specification.
     */
    DASH_PUNCTUATION,

    /**
     * General category "Ps" in the Unicode specification.
     */
    START_PUNCTUATION,

    /**
     * General category "Pe" in the Unicode specification.
     */
    END_PUNCTUATION,

    /**
     * General category "Pc" in the Unicode specification.
     */
    CONNECTOR_PUNCTUATION,

    /**
     * General category "Po" in the Unicode specification.
     */
    OTHER_PUNCTUATION,

    /**
     * General category "Sm" in the Unicode specification.
     */
    MATH_SYMBOL,

    /**
     * General category "Sc" in the Unicode specification.
     */
    CURRENCY_SYMBOL,

    /**
     * General category "Sk" in the Unicode specification.
     */
    MODIFIER_SYMBOL,

    /**
     * General category "So" in the Unicode specification.
     */
    OTHER_SYMBOL,

    /**
     * General category "Pi" in the Unicode specification.
     */
    INITIAL_QUOTE_PUNCTUATION,

    /**
     * General category "Pf" in the Unicode specification.
     */
    FINAL_QUOTE_PUNCTUATION;

    /**
     * Two-letter code of this general category in the Unicode specification.
     */
    public actual val code: String get() = TODO("Wasm stdlib: Text")

    /**
     * Returns `true` if [char] character belongs to this category.
     */
    public actual operator fun contains(char: Char): Boolean = TODO("Wasm stdlib: Text")
}
