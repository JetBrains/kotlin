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

package kotlin.text

/**
 * Unicode category (i.e. Ll, Lu).
 * @author Nikolay A. Kuznetsov
 */
internal open class UnicodeCategory(protected val category: Int) : AbstractCharClass() {
    override fun contains(ch: Int): Boolean = alt xor (ch.toChar().category.value == category)
}

/**
 * Unicode category scope (i.e IsL, IsM, ...)
 * @author Nikolay A. Kuznetsov
 */
internal class UnicodeCategoryScope(category: Int) : UnicodeCategory(category) {
    override fun contains(ch: Int): Boolean = alt xor (category shr ch.toChar().category.value and 1 != 0)
}

/**
 * This class represents character classes, i.e. sets of character either predefined or user defined.
 * Note: this class represent a token, not node, so being constructed by lexer.
 *
 * @author Nikolay A. Kuznetsov
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
    // TODO: @C++?. Or implement a BitSet
    open internal val bits: BitSet?
        get() = null

    fun hasLowHighSurrogates(): Boolean {
        return if (altSurrogates)
            lowHighSurrogates.nextClearBit(0) != -1 // TODO: What if the bitset is empty?
        else
            lowHighSurrogates.nextSetBit(0) != -1
    }

    override val type: Type = Type.CHARCLASS

    open val instance: AbstractCharClass
        get() = this

    // TODO: refactor
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


    // TODO: refactor
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
    // TODO: replace with a property
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

    // TODO: replace getValue with property access?
    internal abstract class LazyCharClass {
        private val posValue: AbstractCharClass by lazy { computeValue() }
        private val negValue: AbstractCharClass by lazy { computeValue().setNegative(true) }

        fun getValue(negative: Boolean): AbstractCharClass = if (!negative) posValue else negValue
        protected abstract fun computeValue(): AbstractCharClass
    }

    internal class LazyDigit : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add('0', '9')
        }
    }

    // TODO: DO we need it? But we have LazyCharCLass.negValue. May be it's connected with mayContainSupplCodepoints?
    // TODO: Don't use bitmaps in preset classes?
    internal class LazyNonDigit : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            val result = CharClass().add('0', '9').setNegative(true)

            result.mayContainSupplCodepoints = true
            return result
        }
    }

    internal class LazySpace : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            /* 9-13 - \t\n\x0B\f\r; 32 - ' ' */
            return CharClass().add(9, 13).add(32)
        }
    }

    internal class LazyNonSpace : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            val result = LazySpace().getValue(negative = true)

            result.mayContainSupplCodepoints = true
            return result
        }
    }

    internal class LazyWord : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add('a', 'z').add('A', 'Z').add('0', '9')
                    .add('_')
        }
    }

    internal class LazyNonWord : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            val result = LazyWord().getValue(negative = true)

            result.mayContainSupplCodepoints = true
            return result
        }
    }

    // TODO: It is only for latin in Harmony.
    internal class LazyLower : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add('a', 'z')
        }
    }

    internal class LazyUpper : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add('A', 'Z')
        }
    }

    internal class LazyASCII : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add(0x00, 0x7F)
        }
    }

    internal class LazyAlpha : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add('a', 'z').add('A', 'Z')
        }
    }

    internal class LazyAlnum : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            // TODO: Get rid of the cast?
            return (LazyAlpha().getValue(negative = false) as CharClass).add('0', '9')
        }
    }

    internal class LazyPunct : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            /* Punctuation !"#$%&'()*+,-./:;<=>?@ [\]^_` {|}~ */
            return CharClass().add(0x21, 0x40).add(0x5B, 0x60).add(0x7B,
                    0x7E)
        }
    }

    internal class LazyGraph : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            /* plus punctuation */
            return (LazyAlnum().getValue(negative = false) as CharClass)
                    .add(0x21, 0x40)
                    .add(0x5B, 0x60)
                    .add(0x7B, 0x7E)
        }
    }

    internal class LazyPrint : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return (LazyGraph().getValue(negative = true) as CharClass).add(0x20)
        }
    }

    internal class LazyBlank : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add(' ').add('\t')
        }
    }

    internal class LazyCntrl : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add(0x00, 0x1F).add(0x7F)
        }
    }

    internal class LazyXDigit : LazyCharClass() {
        override fun computeValue(): AbstractCharClass {
            return CharClass().add('0', '9').add('a', 'f').add('A', 'F')
        }
    }

    internal class LazyRange(var start: Int, var end: Int) : LazyCharClass() {

        public override fun computeValue(): AbstractCharClass {
            val chCl = CharClass().add(start, end)
            return chCl
        }
    }

    internal class LazySpecialsBlock : LazyCharClass() {
        public override fun computeValue(): AbstractCharClass {
            return CharClass().add(0xFEFF, 0xFEFF).add(0xFFF0, 0xFFFD)
        }
    }

    internal class LazyCategoryScope(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : LazyCharClass() {
        
        override fun computeValue(): AbstractCharClass {
            val result = UnicodeCategoryScope(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY)
            }

            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    internal class LazyCategory(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : LazyCharClass() {

        override fun computeValue(): AbstractCharClass {
            val result = UnicodeCategory(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY)
            }
            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    // -----------------------------------------------------------------
    // Static methods and predefined classes
    // -----------------------------------------------------------------
    companion object {

        //Char.MAX_SURROGATE - Char.MIN_SURROGATE + 1
        var SURROGATE_CARDINALITY = 2048

        var space: LazyCharClass = LazySpace()
        var digit: LazyCharClass = LazyDigit()

        /**
         * character classes generated from
         * http://www.unicode.org/reports/tr18/
         * http://www.unicode.org/Public/4.1.0/ucd/Blocks.txt
         */
        // TODO: @C++ Harmony uses ListResourceBundle class here.  TODO: perfrom a effector lookup on the C++ side.
        // Temporary solution: when
        // TODO: @C++. Or make it in code. Or do something else.
        // We can lazily create objects of char classes in C++ hashmap or Array.
        // The main point of it is laziness
        fun getClass(name: String) : LazyCharClass {

            return when (name) {

                "Lower" -> LazyLower()
                "Upper" -> LazyUpper()
                "ASCII" -> LazyASCII()
                "Alpha" -> LazyAlpha()
                "Digit" -> digit
                "Alnum" -> LazyAlnum()
                "Punct" -> LazyPunct()
                "Graph" -> LazyGraph()
                "Print" -> LazyPrint()
                "Blank" -> LazyBlank()
                "Cntrl" -> LazyCntrl()
                "XDigit" -> LazyXDigit()
                "Space" -> space
                "w" -> LazyWord()
                "W" -> LazyNonWord()
                "s" -> space
                "S" -> LazyNonSpace()
                "d" -> digit
                "D" -> LazyNonDigit()
                "BasicLatin" -> LazyRange(0x0000, 0x007F)
                "Latin-1Supplement" -> LazyRange(0x0080, 0x00FF)
                "LatinExtended-A" -> LazyRange(0x0100, 0x017F)
                "LatinExtended-B" -> LazyRange(0x0180, 0x024F)
                "IPAExtensions" -> LazyRange(0x0250, 0x02AF)
                "SpacingModifierLetters" -> LazyRange(0x02B0, 0x02FF)
                "CombiningDiacriticalMarks" -> LazyRange(0x0300, 0x036F)
                "Greek" -> LazyRange(0x0370, 0x03FF)
                "Cyrillic" -> LazyRange(0x0400, 0x04FF)
                "CyrillicSupplement" -> LazyRange(0x0500, 0x052F)
                "Armenian" -> LazyRange(0x0530, 0x058F)
                "Hebrew" -> LazyRange(0x0590, 0x05FF)
                "Arabic" -> LazyRange(0x0600, 0x06FF)
                "Syriac" -> LazyRange(0x0700, 0x074F)
                "ArabicSupplement" -> LazyRange(0x0750, 0x077F)
                "Thaana" -> LazyRange(0x0780, 0x07BF)
                "Devanagari" -> LazyRange(0x0900, 0x097F)
                "Bengali" -> LazyRange(0x0980, 0x09FF)
                "Gurmukhi" -> LazyRange(0x0A00, 0x0A7F)
                "Gujarati" -> LazyRange(0x0A80, 0x0AFF)
                "Oriya" -> LazyRange(0x0B00, 0x0B7F)
                "Tamil" -> LazyRange(0x0B80, 0x0BFF)
                "Telugu" -> LazyRange(0x0C00, 0x0C7F)
                "Kannada" -> LazyRange(0x0C80, 0x0CFF)
                "Malayalam" -> LazyRange(0x0D00, 0x0D7F)
                "Sinhala" -> LazyRange(0x0D80, 0x0DFF)
                "Thai" -> LazyRange(0x0E00, 0x0E7F)
                "Lao" -> LazyRange(0x0E80, 0x0EFF)
                "Tibetan" -> LazyRange(0x0F00, 0x0FFF)
                "Myanmar" -> LazyRange(0x1000, 0x109F)
                "Georgian" -> LazyRange(0x10A0, 0x10FF)
                "HangulJamo" -> LazyRange(0x1100, 0x11FF)
                "Ethiopic" -> LazyRange(0x1200, 0x137F)
                "EthiopicSupplement" -> LazyRange(0x1380, 0x139F)
                "Cherokee" -> LazyRange(0x13A0, 0x13FF)
                "UnifiedCanadianAboriginalSyllabics" -> LazyRange(0x1400, 0x167F)
                "Ogham" -> LazyRange(0x1680, 0x169F)
                "Runic" -> LazyRange(0x16A0, 0x16FF)
                "Tagalog" -> LazyRange(0x1700, 0x171F)
                "Hanunoo" -> LazyRange(0x1720, 0x173F)
                "Buhid" -> LazyRange(0x1740, 0x175F)
                "Tagbanwa" -> LazyRange(0x1760, 0x177F)
                "Khmer" -> LazyRange(0x1780, 0x17FF)
                "Mongolian" -> LazyRange(0x1800, 0x18AF)
                "Limbu" -> LazyRange(0x1900, 0x194F)
                "TaiLe" -> LazyRange(0x1950, 0x197F)
                "NewTaiLue" -> LazyRange(0x1980, 0x19DF)
                "KhmerSymbols" -> LazyRange(0x19E0, 0x19FF)
                "Buginese" -> LazyRange(0x1A00, 0x1A1F)
                "PhoneticExtensions" -> LazyRange(0x1D00, 0x1D7F)
                "PhoneticExtensionsSupplement" -> LazyRange(0x1D80, 0x1DBF)
                "CombiningDiacriticalMarksSupplement" -> LazyRange(0x1DC0, 0x1DFF)
                "LatinExtendedAdditional" -> LazyRange(0x1E00, 0x1EFF)
                "GreekExtended" -> LazyRange(0x1F00, 0x1FFF)
                "GeneralPunctuation" -> LazyRange(0x2000, 0x206F)
                "SuperscriptsandSubscripts" -> LazyRange(0x2070, 0x209F)
                "CurrencySymbols" -> LazyRange(0x20A0, 0x20CF)
                "CombiningMarksforSymbols" -> LazyRange(0x20D0, 0x20FF)
                "LetterlikeSymbols" -> LazyRange(0x2100, 0x214F)
                "NumberForms" -> LazyRange(0x2150, 0x218F)
                "Arrows" -> LazyRange(0x2190, 0x21FF)
                "MathematicalOperators" -> LazyRange(0x2200, 0x22FF)
                "MiscellaneousTechnical" -> LazyRange(0x2300, 0x23FF)
                "ControlPictures" -> LazyRange(0x2400, 0x243F)
                "OpticalCharacterRecognition" -> LazyRange(0x2440, 0x245F)
                "EnclosedAlphanumerics" -> LazyRange(0x2460, 0x24FF)
                "BoxDrawing" -> LazyRange(0x2500, 0x257F)
                "BlockElements" -> LazyRange(0x2580, 0x259F)
                "GeometricShapes" -> LazyRange(0x25A0, 0x25FF)
                "MiscellaneousSymbols" -> LazyRange(0x2600, 0x26FF)
                "Dingbats" -> LazyRange(0x2700, 0x27BF)
                "MiscellaneousMathematicalSymbols-A" -> LazyRange(0x27C0, 0x27EF)
                "SupplementalArrows-A" -> LazyRange(0x27F0, 0x27FF)
                "BraillePatterns" -> LazyRange(0x2800, 0x28FF)
                "SupplementalArrows-B" -> LazyRange(0x2900, 0x297F)
                "MiscellaneousMathematicalSymbols-B" -> LazyRange(0x2980, 0x29FF)
                "SupplementalMathematicalOperators" -> LazyRange(0x2A00, 0x2AFF)
                "MiscellaneousSymbolsandArrows" -> LazyRange(0x2B00, 0x2BFF)
                "Glagolitic" -> LazyRange(0x2C00, 0x2C5F)
                "Coptic" -> LazyRange(0x2C80, 0x2CFF)
                "GeorgianSupplement" -> LazyRange(0x2D00, 0x2D2F)
                "Tifinagh" -> LazyRange(0x2D30, 0x2D7F)
                "EthiopicExtended" -> LazyRange(0x2D80, 0x2DDF)
                "SupplementalPunctuation" -> LazyRange(0x2E00, 0x2E7F)
                "CJKRadicalsSupplement" -> LazyRange(0x2E80, 0x2EFF)
                "KangxiRadicals" -> LazyRange(0x2F00, 0x2FDF)
                "IdeographicDescriptionCharacters" -> LazyRange(0x2FF0, 0x2FFF)
                "CJKSymbolsandPunctuation" -> LazyRange(0x3000, 0x303F)
                "Hiragana" -> LazyRange(0x3040, 0x309F)
                "Katakana" -> LazyRange(0x30A0, 0x30FF)
                "Bopomofo" -> LazyRange(0x3100, 0x312F)
                "HangulCompatibilityJamo" -> LazyRange(0x3130, 0x318F)
                "Kanbun" -> LazyRange(0x3190, 0x319F)
                "BopomofoExtended" -> LazyRange(0x31A0, 0x31BF)
                "CJKStrokes" -> LazyRange(0x31C0, 0x31EF)
                "KatakanaPhoneticExtensions" -> LazyRange(0x31F0, 0x31FF)
                "EnclosedCJKLettersandMonths" -> LazyRange(0x3200, 0x32FF)
                "CJKCompatibility" -> LazyRange(0x3300, 0x33FF)
                "CJKUnifiedIdeographsExtensionA" -> LazyRange(0x3400, 0x4DB5)
                "YijingHexagramSymbols" -> LazyRange(0x4DC0, 0x4DFF)
                "CJKUnifiedIdeographs" -> LazyRange(0x4E00, 0x9FFF)
                "YiSyllables" -> LazyRange(0xA000, 0xA48F)
                "YiRadicals" -> LazyRange(0xA490, 0xA4CF)
                "ModifierToneLetters" -> LazyRange(0xA700, 0xA71F)
                "SylotiNagri" -> LazyRange(0xA800, 0xA82F)
                "HangulSyllables" -> LazyRange(0xAC00, 0xD7A3)
                "HighSurrogates" -> LazyRange(0xD800, 0xDB7F)
                "HighPrivateUseSurrogates" -> LazyRange(0xDB80, 0xDBFF)
                "LowSurrogates" -> LazyRange(0xDC00, 0xDFFF)
                "PrivateUseArea" -> LazyRange(0xE000, 0xF8FF)
                "CJKCompatibilityIdeographs" -> LazyRange(0xF900, 0xFAFF)
                "AlphabeticPresentationForms" -> LazyRange(0xFB00, 0xFB4F)
                "ArabicPresentationForms-A" -> LazyRange(0xFB50, 0xFDFF)
                "VariationSelectors" -> LazyRange(0xFE00, 0xFE0F)
                "VerticalForms" -> LazyRange(0xFE10, 0xFE1F)
                "CombiningHalfMarks" -> LazyRange(0xFE20, 0xFE2F)
                "CJKCompatibilityForms" -> LazyRange(0xFE30, 0xFE4F)
                "SmallFormVariants" -> LazyRange(0xFE50, 0xFE6F)
                "ArabicPresentationForms-B" -> LazyRange(0xFE70, 0xFEFF)
                "HalfwidthandFullwidthForms" -> LazyRange(0xFF00, 0xFFEF)
                "all" -> LazyRange(0x00, 0x10FFFF)
                "Specials" -> LazySpecialsBlock()
                "Cn" -> LazyCategory(CharCategory.UNASSIGNED.value, true)
                "IsL" -> LazyCategoryScope(0x3E, true)
                "Lu" -> LazyCategory(CharCategory.UPPERCASE_LETTER.value, true)
                "Ll" -> LazyCategory(CharCategory.LOWERCASE_LETTER.value, true)
                "Lt" -> LazyCategory(CharCategory.TITLECASE_LETTER.value, false)
                "Lm" -> LazyCategory(CharCategory.MODIFIER_LETTER.value, false)
                "Lo" -> LazyCategory(CharCategory.OTHER_LETTER.value, true)
                "IsM" -> LazyCategoryScope(0x1C0, true)
                "Mn" -> LazyCategory(CharCategory.NON_SPACING_MARK.value, true)
                "Me" -> LazyCategory(CharCategory.ENCLOSING_MARK.value, false)
                "Mc" -> LazyCategory(CharCategory.COMBINING_SPACING_MARK.value, true)
                "N" -> LazyCategoryScope(0xE00, true)
                "Nd" -> LazyCategory(CharCategory.DECIMAL_DIGIT_NUMBER.value, true)
                "Nl" -> LazyCategory(CharCategory.LETTER_NUMBER.value, true)
                "No" -> LazyCategory(CharCategory.OTHER_NUMBER.value, true)
                "IsZ" -> LazyCategoryScope(0x7000, false)
                "Zs" -> LazyCategory(CharCategory.SPACE_SEPARATOR.value, false)
                "Zl" -> LazyCategory(CharCategory.LINE_SEPARATOR.value, false)
                "Zp" -> LazyCategory(CharCategory.PARAGRAPH_SEPARATOR.value, false)
                "IsC" -> LazyCategoryScope(0xF0000, true, true)
                "Cc" -> LazyCategory(CharCategory.CONTROL.value, false)
                "Cf" -> LazyCategory(CharCategory.FORMAT.value, true)
                "Co" -> LazyCategory(CharCategory.PRIVATE_USE.value, true)
                "Cs" -> LazyCategory(CharCategory.SURROGATE.value, false, true)
                "IsP" -> LazyCategoryScope(1 shl CharCategory.DASH_PUNCTUATION.value or
                        (1 shl CharCategory.START_PUNCTUATION.value) or
                        (1 shl CharCategory.END_PUNCTUATION.value) or
                        (1 shl CharCategory.CONNECTOR_PUNCTUATION.value) or
                        (1 shl CharCategory.OTHER_PUNCTUATION.value) or
                        (1 shl CharCategory.INITIAL_QUOTE_PUNCTUATION.value) or
                        (1 shl CharCategory.FINAL_QUOTE_PUNCTUATION.value), true)
                "Pd" -> LazyCategory(CharCategory.DASH_PUNCTUATION.value, false)
                "Ps" -> LazyCategory(CharCategory.START_PUNCTUATION.value, false)
                "Pe" -> LazyCategory(CharCategory.END_PUNCTUATION.value, false)
                "Pc" -> LazyCategory(CharCategory.CONNECTOR_PUNCTUATION.value, false)
                "Po" -> LazyCategory(CharCategory.OTHER_PUNCTUATION.value, true)
                "IsS" -> LazyCategoryScope(0x7E000000, true)
                "Sm" -> LazyCategory(CharCategory.MATH_SYMBOL.value, true)
                "Sc" -> LazyCategory(CharCategory.CURRENCY_SYMBOL.value, false)
                "Sk" -> LazyCategory(CharCategory.MODIFIER_SYMBOL.value, false)
                "So" -> LazyCategory(CharCategory.OTHER_SYMBOL.value, true)
                "Pi" -> LazyCategory(CharCategory.INITIAL_QUOTE_PUNCTUATION.value, false)
                "Pf" -> LazyCategory(CharCategory.FINAL_QUOTE_PUNCTUATION.value, false)
                else -> throw PatternSyntaxException("No such character class")
            }
        }

        fun intersects(ch1: Int, ch2: Int): Boolean {
            return ch1 == ch2
        }

        fun intersects(cc: AbstractCharClass, ch: Int): Boolean {
            return cc.contains(ch)
        }

        fun intersects(cc1: AbstractCharClass,
                       cc2: AbstractCharClass): Boolean {
            if (cc1.bits == null || cc2.bits == null)
                return true
            return cc1.bits!!.intersects(cc2.bits!!)
        }

        fun getPredefinedClass(name: String, negative: Boolean): AbstractCharClass {
            return (getClass(name)).getValue(negative)
        }
    }
}
