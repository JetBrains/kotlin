/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlin.text.regex

/**
 * Unicode category (i.e. Ll, Lu).
 */
internal open class UnicodeCategory(protected val category: Int) : AbstractCharClass() {
    override fun contains(ch: Int): Boolean = alt xor (ch.toChar().category.value == category)
}

/**
 * Unicode category scope (i.e IsL, IsM, ...)
 */
internal class UnicodeCategoryScope(category: Int) : UnicodeCategory(category) {
    override fun contains(ch: Int): Boolean = alt xor (category shr ch.toChar().category.value and 1 != 0)
}

/**
 * This class represents character classes, i.e. sets of character either predefined or user defined.
 * Note: this class represent a token, not node, so being constructed by lexer.
 */
internal abstract class AbstractCharClass : SpecialToken() {

    /**
     * Show if the class has alternative meaning:
     * if the class contains character 'a' and alt == true then the class will contains all characters except 'a'.
     */
    internal var alt: Boolean = false
    internal var altSurrogates: Boolean = false

    internal val lowHighSurrogates = BitSet(SURROGATE_CARDINALITY) // Bit set for surrogates?

    /*
     * Indicates if this class may contain supplementary Unicode codepoints.
     * If this flag is specified it doesn't mean that this class contains supplementary characters but may contain.
     */
    var mayContainSupplCodepoints = false
        protected set

    /** Returns true if this char class contains character specified. */
    abstract operator fun contains(ch: Int): Boolean
    open fun contains(ch: Char): Boolean = contains(ch.toInt())

    /**
     * Returns BitSet representing this character class or `null`
     * if this character class does not have character representation;
     */
    open internal val bits: BitSet?
        get() = null

    fun hasLowHighSurrogates(): Boolean {
        return if (altSurrogates)
            lowHighSurrogates.nextClearBit(0) != -1
        else
            lowHighSurrogates.nextSetBit(0) != -1
    }

    override val type: Type = Type.CHARCLASS

    open val instance: AbstractCharClass
        get() = this

    val surrogates: AbstractCharClass by lazy {
        val result = object : AbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                return if (index >= 0 && index < AbstractCharClass.SURROGATE_CARDINALITY)
                    this.altSurrogates xor this@AbstractCharClass.lowHighSurrogates.get(index)
                else
                    false
            }
        }
        result.setNegative(this.altSurrogates)
        return@lazy result
    }


    val withoutSurrogates: AbstractCharClass by lazy {
        val result = object : AbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                val containslHS = if (index >= 0 && index < AbstractCharClass.SURROGATE_CARDINALITY)
                    this.altSurrogates xor this@AbstractCharClass.lowHighSurrogates.get(index)
                else
                    false

                return this@AbstractCharClass.contains(ch) && !containslHS
            }
        }
        result.setNegative(isNegative())
        result.mayContainSupplCodepoints = mayContainSupplCodepoints
        return@lazy result
    }

    /**
     * Sets this CharClass to negative form, i.e. if they will add some characters and after that set this
     * class to negative it will accept all the characters except previously set ones.
     *
     * Although this method will not alternate all the already set characters,
     * just overall meaning of the class.
     */
    fun setNegative(value: Boolean): AbstractCharClass {
        if (alt xor value) {
            alt = !alt
            altSurrogates = !altSurrogates
        }
        if (!mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true
        }
        return this
    }

    fun isNegative(): Boolean {
        return alt
    }

    internal abstract class CachedCharClass {
        private val posValue: AbstractCharClass by lazy { computeValue() }
        private val negValue: AbstractCharClass by lazy { computeValue().setNegative(true) }

        fun getValue(negative: Boolean): AbstractCharClass = if (!negative) posValue else negValue
        protected abstract fun computeValue(): AbstractCharClass
    }

    internal class CachedDigit : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add('0', '9')
    }

    internal class CachedNonDigit : CachedCharClass() {
        override fun computeValue(): AbstractCharClass =
                CharClass().add('0', '9').setNegative(true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedSpace : CachedCharClass() {
        /* 9-13 - \t\n\x0B\f\r; 32 - ' ' */
        override fun computeValue(): AbstractCharClass = CharClass().add(9, 13).add(32)
    }

    internal class CachedNonSpace : CachedCharClass() {
        override fun computeValue(): AbstractCharClass =
                CachedSpace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedWord : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add('a', 'z').add('A', 'Z').add('0', '9').add('_')
    }

    internal class CachedNonWord : CachedCharClass() {
        override fun computeValue(): AbstractCharClass =
                CachedWord().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedLower : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add('a', 'z')
    }

    internal class CachedUpper : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add('A', 'Z')
    }

    internal class CachedASCII : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add(0x00, 0x7F)
    }

    internal class CachedAlpha : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add('a', 'z').add('A', 'Z')
    }

    internal class CachedAlnum : CachedCharClass() {
        override fun computeValue(): AbstractCharClass =
                (CachedAlpha().getValue(negative = false) as CharClass).add('0', '9')
    }

    internal class CachedPunct : CachedCharClass() {
        /* Punctuation !"#$%&'()*+,-./:;<=>?@ [\]^_` {|}~ */
        override fun computeValue(): AbstractCharClass = CharClass().add(0x21, 0x40).add(0x5B, 0x60).add(0x7B, 0x7E)
    }

    internal class CachedGraph : CachedCharClass() {
        /* plus punctuation */
        override fun computeValue(): AbstractCharClass =
                (CachedAlnum().getValue(negative = false) as CharClass)
                        .add(0x21, 0x40)
                        .add(0x5B, 0x60)
                        .add(0x7B, 0x7E)
    }

    internal class CachedPrint : CachedCharClass() {
        override fun computeValue(): AbstractCharClass =
                (CachedGraph().getValue(negative = true) as CharClass).add(0x20)
    }

    internal class CachedBlank : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add(' ').add('\t')
    }

    internal class CachedCntrl : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add(0x00, 0x1F).add(0x7F)
    }

    internal class CachedXDigit : CachedCharClass() {
        override fun computeValue(): AbstractCharClass = CharClass().add('0', '9').add('a', 'f').add('A', 'F')
    }

    internal class CachedRange(var start: Int, var end: Int) : CachedCharClass() {
        override fun computeValue(): AbstractCharClass =
            object: AbstractCharClass() {
                override fun contains(ch: Int): Boolean = alt xor (ch in start..end)
            }.apply {
                if (end >= Char.MIN_SUPPLEMENTARY_CODE_POINT) {
                    mayContainSupplCodepoints = true
                }
                val minSurrogate = Char.MIN_SURROGATE.toInt()
                val maxSurrogate = Char.MAX_SURROGATE.toInt()
                // There is an intersection with surrogate characters.
                if (end >= minSurrogate && start <= maxSurrogate && start <= end) {
                    val surrogatesStart = maxOf(start, minSurrogate) - minSurrogate
                    val surrogatesEnd = minOf(end, maxSurrogate) - minSurrogate
                    lowHighSurrogates.set(surrogatesStart..surrogatesEnd)
                }
            }
    }

    internal class CachedSpecialsBlock : CachedCharClass() {
        public override fun computeValue(): AbstractCharClass = CharClass().add(0xFEFF, 0xFEFF).add(0xFFF0, 0xFFFD)
    }

    internal class CachedCategoryScope(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {

        override fun computeValue(): AbstractCharClass {
            val result = UnicodeCategoryScope(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY)
            }

            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    internal class CachedCategory(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {

        override fun computeValue(): AbstractCharClass {
            val result = UnicodeCategory(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY)
            }
            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }


    companion object {
        //Char.MAX_SURROGATE - Char.MIN_SURROGATE + 1
        const val SURROGATE_CARDINALITY = 2048

        private var classCache: MutableMap<String, CachedCharClass>? = null

        /**
         * Character classes.
         * See http://www.unicode.org/reports/tr18/, http://www.unicode.org/Public/4.1.0/ucd/Blocks.txt
         */
        // TODO: Make a faster implementation.
        fun createClass(name: String) : CachedCharClass =
                when (name) {
                    "Lower" -> CachedLower()
                    "Upper" -> CachedUpper()
                    "ASCII" -> CachedASCII()
                    "Alpha" -> CachedAlpha()
                    "Digit" -> CachedDigit()
                    "Alnum" -> CachedAlnum()
                    "Punct" -> CachedPunct()
                    "Graph" -> CachedGraph()
                    "Print" -> CachedPrint()
                    "Blank" -> CachedBlank()
                    "Cntrl" -> CachedCntrl()
                    "XDigit" -> CachedXDigit()
                    "Space" -> CachedSpace()
                    "w" -> CachedWord()
                    "W" -> CachedNonWord()
                    "s" -> CachedSpace()
                    "S" -> CachedNonSpace()
                    "d" -> CachedDigit()
                    "D" -> CachedNonDigit()
                    "BasicLatin" -> CachedRange(0x0000, 0x007F)
                    "Latin-1Supplement" -> CachedRange(0x0080, 0x00FF)
                    "LatinExtended-A" -> CachedRange(0x0100, 0x017F)
                    "LatinExtended-B" -> CachedRange(0x0180, 0x024F)
                    "IPAExtensions" -> CachedRange(0x0250, 0x02AF)
                    "SpacingModifierLetters" -> CachedRange(0x02B0, 0x02FF)
                    "CombiningDiacriticalMarks" -> CachedRange(0x0300, 0x036F)
                    "Greek" -> CachedRange(0x0370, 0x03FF)
                    "Cyrillic" -> CachedRange(0x0400, 0x04FF)
                    "CyrillicSupplement" -> CachedRange(0x0500, 0x052F)
                    "Armenian" -> CachedRange(0x0530, 0x058F)
                    "Hebrew" -> CachedRange(0x0590, 0x05FF)
                    "Arabic" -> CachedRange(0x0600, 0x06FF)
                    "Syriac" -> CachedRange(0x0700, 0x074F)
                    "ArabicSupplement" -> CachedRange(0x0750, 0x077F)
                    "Thaana" -> CachedRange(0x0780, 0x07BF)
                    "Devanagari" -> CachedRange(0x0900, 0x097F)
                    "Bengali" -> CachedRange(0x0980, 0x09FF)
                    "Gurmukhi" -> CachedRange(0x0A00, 0x0A7F)
                    "Gujarati" -> CachedRange(0x0A80, 0x0AFF)
                    "Oriya" -> CachedRange(0x0B00, 0x0B7F)
                    "Tamil" -> CachedRange(0x0B80, 0x0BFF)
                    "Telugu" -> CachedRange(0x0C00, 0x0C7F)
                    "Kannada" -> CachedRange(0x0C80, 0x0CFF)
                    "Malayalam" -> CachedRange(0x0D00, 0x0D7F)
                    "Sinhala" -> CachedRange(0x0D80, 0x0DFF)
                    "Thai" -> CachedRange(0x0E00, 0x0E7F)
                    "Lao" -> CachedRange(0x0E80, 0x0EFF)
                    "Tibetan" -> CachedRange(0x0F00, 0x0FFF)
                    "Myanmar" -> CachedRange(0x1000, 0x109F)
                    "Georgian" -> CachedRange(0x10A0, 0x10FF)
                    "HangulJamo" -> CachedRange(0x1100, 0x11FF)
                    "Ethiopic" -> CachedRange(0x1200, 0x137F)
                    "EthiopicSupplement" -> CachedRange(0x1380, 0x139F)
                    "Cherokee" -> CachedRange(0x13A0, 0x13FF)
                    "UnifiedCanadianAboriginalSyllabics" -> CachedRange(0x1400, 0x167F)
                    "Ogham" -> CachedRange(0x1680, 0x169F)
                    "Runic" -> CachedRange(0x16A0, 0x16FF)
                    "Tagalog" -> CachedRange(0x1700, 0x171F)
                    "Hanunoo" -> CachedRange(0x1720, 0x173F)
                    "Buhid" -> CachedRange(0x1740, 0x175F)
                    "Tagbanwa" -> CachedRange(0x1760, 0x177F)
                    "Khmer" -> CachedRange(0x1780, 0x17FF)
                    "Mongolian" -> CachedRange(0x1800, 0x18AF)
                    "Limbu" -> CachedRange(0x1900, 0x194F)
                    "TaiLe" -> CachedRange(0x1950, 0x197F)
                    "NewTaiLue" -> CachedRange(0x1980, 0x19DF)
                    "KhmerSymbols" -> CachedRange(0x19E0, 0x19FF)
                    "Buginese" -> CachedRange(0x1A00, 0x1A1F)
                    "PhoneticExtensions" -> CachedRange(0x1D00, 0x1D7F)
                    "PhoneticExtensionsSupplement" -> CachedRange(0x1D80, 0x1DBF)
                    "CombiningDiacriticalMarksSupplement" -> CachedRange(0x1DC0, 0x1DFF)
                    "LatinExtendedAdditional" -> CachedRange(0x1E00, 0x1EFF)
                    "GreekExtended" -> CachedRange(0x1F00, 0x1FFF)
                    "GeneralPunctuation" -> CachedRange(0x2000, 0x206F)
                    "SuperscriptsandSubscripts" -> CachedRange(0x2070, 0x209F)
                    "CurrencySymbols" -> CachedRange(0x20A0, 0x20CF)
                    "CombiningMarksforSymbols" -> CachedRange(0x20D0, 0x20FF)
                    "LetterlikeSymbols" -> CachedRange(0x2100, 0x214F)
                    "NumberForms" -> CachedRange(0x2150, 0x218F)
                    "Arrows" -> CachedRange(0x2190, 0x21FF)
                    "MathematicalOperators" -> CachedRange(0x2200, 0x22FF)
                    "MiscellaneousTechnical" -> CachedRange(0x2300, 0x23FF)
                    "ControlPictures" -> CachedRange(0x2400, 0x243F)
                    "OpticalCharacterRecognition" -> CachedRange(0x2440, 0x245F)
                    "EnclosedAlphanumerics" -> CachedRange(0x2460, 0x24FF)
                    "BoxDrawing" -> CachedRange(0x2500, 0x257F)
                    "BlockElements" -> CachedRange(0x2580, 0x259F)
                    "GeometricShapes" -> CachedRange(0x25A0, 0x25FF)
                    "MiscellaneousSymbols" -> CachedRange(0x2600, 0x26FF)
                    "Dingbats" -> CachedRange(0x2700, 0x27BF)
                    "MiscellaneousMathematicalSymbols-A" -> CachedRange(0x27C0, 0x27EF)
                    "SupplementalArrows-A" -> CachedRange(0x27F0, 0x27FF)
                    "BraillePatterns" -> CachedRange(0x2800, 0x28FF)
                    "SupplementalArrows-B" -> CachedRange(0x2900, 0x297F)
                    "MiscellaneousMathematicalSymbols-B" -> CachedRange(0x2980, 0x29FF)
                    "SupplementalMathematicalOperators" -> CachedRange(0x2A00, 0x2AFF)
                    "MiscellaneousSymbolsandArrows" -> CachedRange(0x2B00, 0x2BFF)
                    "Glagolitic" -> CachedRange(0x2C00, 0x2C5F)
                    "Coptic" -> CachedRange(0x2C80, 0x2CFF)
                    "GeorgianSupplement" -> CachedRange(0x2D00, 0x2D2F)
                    "Tifinagh" -> CachedRange(0x2D30, 0x2D7F)
                    "EthiopicExtended" -> CachedRange(0x2D80, 0x2DDF)
                    "SupplementalPunctuation" -> CachedRange(0x2E00, 0x2E7F)
                    "CJKRadicalsSupplement" -> CachedRange(0x2E80, 0x2EFF)
                    "KangxiRadicals" -> CachedRange(0x2F00, 0x2FDF)
                    "IdeographicDescriptionCharacters" -> CachedRange(0x2FF0, 0x2FFF)
                    "CJKSymbolsandPunctuation" -> CachedRange(0x3000, 0x303F)
                    "Hiragana" -> CachedRange(0x3040, 0x309F)
                    "Katakana" -> CachedRange(0x30A0, 0x30FF)
                    "Bopomofo" -> CachedRange(0x3100, 0x312F)
                    "HangulCompatibilityJamo" -> CachedRange(0x3130, 0x318F)
                    "Kanbun" -> CachedRange(0x3190, 0x319F)
                    "BopomofoExtended" -> CachedRange(0x31A0, 0x31BF)
                    "CJKStrokes" -> CachedRange(0x31C0, 0x31EF)
                    "KatakanaPhoneticExtensions" -> CachedRange(0x31F0, 0x31FF)
                    "EnclosedCJKLettersandMonths" -> CachedRange(0x3200, 0x32FF)
                    "CJKCompatibility" -> CachedRange(0x3300, 0x33FF)
                    "CJKUnifiedIdeographsExtensionA" -> CachedRange(0x3400, 0x4DB5)
                    "YijingHexagramSymbols" -> CachedRange(0x4DC0, 0x4DFF)
                    "CJKUnifiedIdeographs" -> CachedRange(0x4E00, 0x9FFF)
                    "YiSyllables" -> CachedRange(0xA000, 0xA48F)
                    "YiRadicals" -> CachedRange(0xA490, 0xA4CF)
                    "ModifierToneLetters" -> CachedRange(0xA700, 0xA71F)
                    "SylotiNagri" -> CachedRange(0xA800, 0xA82F)
                    "HangulSyllables" -> CachedRange(0xAC00, 0xD7A3)
                    "HighSurrogates" -> CachedRange(0xD800, 0xDB7F)
                    "HighPrivateUseSurrogates" -> CachedRange(0xDB80, 0xDBFF)
                    "LowSurrogates" -> CachedRange(0xDC00, 0xDFFF)
                    "PrivateUseArea" -> CachedRange(0xE000, 0xF8FF)
                    "CJKCompatibilityIdeographs" -> CachedRange(0xF900, 0xFAFF)
                    "AlphabeticPresentationForms" -> CachedRange(0xFB00, 0xFB4F)
                    "ArabicPresentationForms-A" -> CachedRange(0xFB50, 0xFDFF)
                    "VariationSelectors" -> CachedRange(0xFE00, 0xFE0F)
                    "VerticalForms" -> CachedRange(0xFE10, 0xFE1F)
                    "CombiningHalfMarks" -> CachedRange(0xFE20, 0xFE2F)
                    "CJKCompatibilityForms" -> CachedRange(0xFE30, 0xFE4F)
                    "SmallFormVariants" -> CachedRange(0xFE50, 0xFE6F)
                    "ArabicPresentationForms-B" -> CachedRange(0xFE70, 0xFEFF)
                    "HalfwidthandFullwidthForms" -> CachedRange(0xFF00, 0xFFEF)
                    "all" -> CachedRange(0x00, 0x10FFFF)
                    "Specials" -> CachedSpecialsBlock()
                    "Cn" -> CachedCategory(CharCategory.UNASSIGNED.value, true)
                    "IsL" -> CachedCategoryScope(0x3E, true)
                    "Lu" -> CachedCategory(CharCategory.UPPERCASE_LETTER.value, true)
                    "Ll" -> CachedCategory(CharCategory.LOWERCASE_LETTER.value, true)
                    "Lt" -> CachedCategory(CharCategory.TITLECASE_LETTER.value, false)
                    "Lm" -> CachedCategory(CharCategory.MODIFIER_LETTER.value, false)
                    "Lo" -> CachedCategory(CharCategory.OTHER_LETTER.value, true)
                    "IsM" -> CachedCategoryScope(0x1C0, true)
                    "Mn" -> CachedCategory(CharCategory.NON_SPACING_MARK.value, true)
                    "Me" -> CachedCategory(CharCategory.ENCLOSING_MARK.value, false)
                    "Mc" -> CachedCategory(CharCategory.COMBINING_SPACING_MARK.value, true)
                    "N" -> CachedCategoryScope(0xE00, true)
                    "Nd" -> CachedCategory(CharCategory.DECIMAL_DIGIT_NUMBER.value, true)
                    "Nl" -> CachedCategory(CharCategory.LETTER_NUMBER.value, true)
                    "No" -> CachedCategory(CharCategory.OTHER_NUMBER.value, true)
                    "IsZ" -> CachedCategoryScope(0x7000, false)
                    "Zs" -> CachedCategory(CharCategory.SPACE_SEPARATOR.value, false)
                    "Zl" -> CachedCategory(CharCategory.LINE_SEPARATOR.value, false)
                    "Zp" -> CachedCategory(CharCategory.PARAGRAPH_SEPARATOR.value, false)
                    "IsC" -> CachedCategoryScope(0xF0000, true, true)
                    "Cc" -> CachedCategory(CharCategory.CONTROL.value, false)
                    "Cf" -> CachedCategory(CharCategory.FORMAT.value, true)
                    "Co" -> CachedCategory(CharCategory.PRIVATE_USE.value, true)
                    "Cs" -> CachedCategory(CharCategory.SURROGATE.value, false, true)
                    "IsP" -> CachedCategoryScope(1 shl CharCategory.DASH_PUNCTUATION.value or
                            (1 shl CharCategory.START_PUNCTUATION.value) or
                            (1 shl CharCategory.END_PUNCTUATION.value) or
                            (1 shl CharCategory.CONNECTOR_PUNCTUATION.value) or
                            (1 shl CharCategory.OTHER_PUNCTUATION.value) or
                            (1 shl CharCategory.INITIAL_QUOTE_PUNCTUATION.value) or
                            (1 shl CharCategory.FINAL_QUOTE_PUNCTUATION.value), true)
                    "Pd" -> CachedCategory(CharCategory.DASH_PUNCTUATION.value, false)
                    "Ps" -> CachedCategory(CharCategory.START_PUNCTUATION.value, false)
                    "Pe" -> CachedCategory(CharCategory.END_PUNCTUATION.value, false)
                    "Pc" -> CachedCategory(CharCategory.CONNECTOR_PUNCTUATION.value, false)
                    "Po" -> CachedCategory(CharCategory.OTHER_PUNCTUATION.value, true)
                    "IsS" -> CachedCategoryScope(0x7E000000, true)
                    "Sm" -> CachedCategory(CharCategory.MATH_SYMBOL.value, true)
                    "Sc" -> CachedCategory(CharCategory.CURRENCY_SYMBOL.value, false)
                    "Sk" -> CachedCategory(CharCategory.MODIFIER_SYMBOL.value, false)
                    "So" -> CachedCategory(CharCategory.OTHER_SYMBOL.value, true)
                    "Pi" -> CachedCategory(CharCategory.INITIAL_QUOTE_PUNCTUATION.value, false)
                    "Pf" -> CachedCategory(CharCategory.FINAL_QUOTE_PUNCTUATION.value, false)
                    else -> throw PatternSyntaxException("No such character class")
                }

        fun intersects(ch1: Int, ch2: Int): Boolean = ch1 == ch2
        fun intersects(cc: AbstractCharClass, ch: Int): Boolean = cc.contains(ch)

        fun intersects(cc1: AbstractCharClass, cc2: AbstractCharClass): Boolean {
            if (cc1.bits == null || cc2.bits == null) {
                return true
            }
            return cc1.bits!!.intersects(cc2.bits!!)
        }

        fun getPredefinedClass(name: String, negative: Boolean): AbstractCharClass {
            var cache = classCache
            if (cache == null) {
                cache = mutableMapOf()
                classCache = cache
            }
            var cachedClass = cache[name]
            if (cachedClass == null) {
                cachedClass = createClass(name)
                cache[name] = cachedClass
            }
            return cachedClass.getValue(negative)
        }
    }
}
