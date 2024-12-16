/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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

@file:Suppress("DEPRECATION") // Char.toInt()
@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.text.regex

import kotlin.experimental.ExperimentalNativeApi
import kotlin.collections.associate
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.AtomicReference
import kotlin.native.BitSet
import kotlin.native.ObsoleteNativeApi

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
    override fun contains(ch: Int): Boolean {
        return alt xor (((category shr ch.toChar().category.value) and 1) != 0)
    }
}

/**
 * This class represents character classes, i.e. sets of character either predefined or user defined.
 * Note: this class represent a token, not node, so being constructed by lexer.
 */
@OptIn(ObsoleteNativeApi::class)
internal abstract class AbstractCharClass : SpecialToken() {
    /**
     * Show if the class has alternative meaning:
     * if the class contains character 'a' and alt == true then the class will contains all characters except 'a'.
     */
    internal var alt: Boolean = false
    internal var altSurrogates: Boolean = false

    /**
     * For each unpaired surrogate char indicates whether it is contained in this char class.
     */
    internal val lowHighSurrogates = BitSet(SURROGATE_CARDINALITY) // Bit set for surrogates?

    /**
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


    private val surrogates_ = AtomicReference<AbstractCharClass?>(null)
    /**
     * Returns a char class that contains only unpaired surrogate chars from this char class.
     *
     * Consider the following char class: `[a\uD801\uDC00\uD800]`.
     * This function returns a char class that contains only `\uD800`: `[\uD800]`.
     * [classWithoutSurrogates] returns a char class that does not contain `\uD800`: `[a\uD801\uDC00]`.
     *
     * The returned char class is used to create [SurrogateRangeSet] node
     * that matches any unpaired surrogate from this char class. [SurrogateRangeSet]
     * doesn't match a surrogate that is paired with the char before or after it.
     * The result of [classWithoutSurrogates] is used to create [SupplementaryRangeSet]
     * or [RangeSet] depending on [mayContainSupplCodepoints].
     * The two nodes are then combined in [CompositeRangeSet] node to fully represent this char class.
     */
    fun classWithSurrogates(): AbstractCharClass {
        surrogates_.load()?.let {
            return it
        }
        val surrogates = lowHighSurrogates
        val result = object : AbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                return if (index >= 0 && index < AbstractCharClass.SURROGATE_CARDINALITY) {
                    this.altSurrogates xor surrogates[index]
                } else {
                    false
                }
            }
        }
        result.alt = this.alt
        result.altSurrogates = this.altSurrogates
        result.mayContainSupplCodepoints = this.mayContainSupplCodepoints
        surrogates_.compareAndSet(null, result)
        return surrogates_.load()!!
    }


    /**
     * Returns a char class that contains all chars from this char class excluding the unpaired surrogate chars.
     *
     * See [classWithSurrogates] for details.
     */
    // We cannot cache this class as we've done with surrogates above because
    // here is a circular reference between it and AbstractCharClass.
    fun classWithoutSurrogates(): AbstractCharClass {
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
        result.alt = this.alt
        result.altSurrogates = this.altSurrogates
        result.mayContainSupplCodepoints = this.mayContainSupplCodepoints
        return result
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

            if (!mayContainSupplCodepoints) {
                mayContainSupplCodepoints = true
            }
        }
        return this
    }

    fun isNegative(): Boolean {
        return alt
    }

    internal abstract class CachedCharClass {
        lateinit private var posValue: AbstractCharClass

        lateinit private var negValue: AbstractCharClass

        // Somewhat ugly init sequence, as computeValue() may depend on fields, initialized in subclass ctor.
        protected fun initValues() {
            posValue = computeValue()
            negValue = computeValue().setNegative(true)
        }

        fun getValue(negative: Boolean): AbstractCharClass = if (!negative) posValue else negValue
        protected abstract fun computeValue(): AbstractCharClass
    }

    internal class CachedDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add('0', '9')
    }

    internal class CachedNonDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
                CharClass().add('0', '9').setNegative(true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedSpace : CachedCharClass() {
        init {
            initValues()
        }
        /* 9-13 - \t\n\x0B\f\r; 32 - ' ' */
        override fun computeValue(): AbstractCharClass = CharClass().add(9, 13).add(32)
    }

    internal class CachedNonSpace : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
                CachedSpace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedWord : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add('a', 'z').add('A', 'Z').add('0', '9').add('_')
    }

    internal class CachedNonWord : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
                CachedWord().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedLower : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add('a', 'z')
    }

    internal class CachedUpper : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add('A', 'Z')
    }

    internal class CachedASCII : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add(0x00, 0x7F)
    }

    internal class CachedAlpha : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add('a', 'z').add('A', 'Z')
    }

    internal class CachedAlnum : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
                (CachedAlpha().getValue(negative = false) as CharClass).add('0', '9')
    }

    internal class CachedPunct : CachedCharClass() {
        init {
            initValues()
        }
        /* Punctuation !"#$%&'()*+,-./:;<=>?@ [\]^_` {|}~ */
        override fun computeValue(): AbstractCharClass = CharClass().add(0x21, 0x40).add(0x5B, 0x60).add(0x7B, 0x7E)
    }

    internal class CachedGraph : CachedCharClass() {
        init {
            initValues()
        }
        /* plus punctuation */
        override fun computeValue(): AbstractCharClass =
                (CachedAlnum().getValue(negative = false) as CharClass)
                        .add(0x21, 0x40)
                        .add(0x5B, 0x60)
                        .add(0x7B, 0x7E)
    }

    internal class CachedPrint : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
                (CachedGraph().getValue(negative = true) as CharClass).add(0x20)
    }

    internal class CachedBlank : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add(' ').add('\t')
    }

    internal class CachedCntrl : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add(0x00, 0x1F).add(0x7F)
    }

    internal class CachedXDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass = CharClass().add('0', '9').add('a', 'f').add('A', 'F')
    }

    // From Java 8+ `Pattern` doc: \v - A vertical whitespace character: [\n\x0B\f\r\x85\u2028\u2029]
    internal class CachedVerticalWhitespace : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
            CharClass().addAll(listOf('\n', '\u000B', '\u000C' /* aka \f */, '\r', '\u0085', '\u2028', '\u2029'))
    }

    // From Java 8+ `Pattern` doc: \V - A non-vertical whitespace character: [^\v]
    internal class CachedNonVerticalWhitespace: CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
            CachedVerticalWhitespace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
        // TODO: Does it match a pair of surrogates? Do we really need mayContainSupplCodepoints?
    }

    // From Java 8+ `Pattern` doc:
    // \h - A horizontal whitespace character: [ \t\xA0\u1680\u180e\u2000-\u200a\u202f\u205f\u3000]
    internal class CachedHorizontalWhitespace: CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
            CharClass().addAll(listOf(' ', '\t', '\u00A0', '\u1680', '\u180e', '\u202f', '\u205f', '\u3000'))
                .add('\u2000', '\u200a')
    }

    // From Java 8+ `Pattern` doc:
    // \H - A non-horizontal whitespace character: [^\h]
    internal class CachedNonHorizontalWhitespace: CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): AbstractCharClass =
            CachedHorizontalWhitespace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedRange(var start: Int, var end: Int) : CachedCharClass() {
        init {
            initValues()
        }

        @OptIn(ExperimentalNativeApi::class)
        override fun computeValue(): AbstractCharClass =
                object: AbstractCharClass() {
                    override fun contains(ch: Int): Boolean = alt xor (ch in start..end)
                }.apply {
                    if (end >= Char_MIN_SUPPLEMENTARY_CODE_POINT) {
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
        init {
            initValues()
        }
        public override fun computeValue(): AbstractCharClass = CharClass().add(0xFEFF, 0xFEFF).add(0xFFF0, 0xFFFD)
    }

    internal class CachedCategoryScope(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {
        init {
            initValues()
        }
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
        init {
            initValues()
        }
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

        /**
         * Character classes.
         * See http://www.unicode.org/reports/tr18/, http://www.unicode.org/Public/4.1.0/ucd/Blocks.txt
         */
        enum class CharClasses(val regexName : String) {
            LOWER("Lower"),
            UPPER("Upper"),
            ASCII("ASCII"),
            ALPHA("Alpha"),
            DIGIT("Digit"),
            ALNUM("Alnum"),
            PUNCT("Punct"),
            GRAPH("Graph"),
            PRINT("Print"),
            BLANK("Blank"),
            CNTRL("Cntrl"),
            XDIGIT("XDigit"),
            SPACE("Space"),
            WORD("w"),
            NON_WORD("W"),
            SPACE_SHORT("s"),
            NON_SPACE("S"),
            DIGIT_SHORT("d"),
            NON_DIGIT("D"),
            VERTICAL_WHITESPACE("v"),
            NON_VERTICAL_WHITESPACE("V"),
            HORIZONTAL_WHITESPACE("h"),
            NON_HORIZONTAL_WHITESPACE("H"),
            BASIC_LATIN("BasicLatin"),
            LATIN1_SUPPLEMENT("Latin-1Supplement"),
            LATIN_EXTENDED_A("LatinExtended-A"),
            LATIN_EXTENDED_B("LatinExtended-B"),
            IPA_EXTENSIONS("IPAExtensions"),
            SPACING_MODIFIER_LETTERS("SpacingModifierLetters"),
            COMBINING_DIACRITICAL_MARKS("CombiningDiacriticalMarks"),
            GREEK("Greek"),
            CYRILLIC("Cyrillic"),
            CYRILLIC_SUPPLEMENT("CyrillicSupplement"),
            ARMENIAN("Armenian"),
            HEBREW("Hebrew"),
            ARABIC("Arabic"),
            SYRIAC("Syriac"),
            ARABICSUPPLEMENT("ArabicSupplement"),
            THAANA("Thaana"),
            DEVANAGARI("Devanagari"),
            BENGALI("Bengali"),
            GURMUKHI("Gurmukhi"),
            GUJARATI("Gujarati"),
            ORIYA("Oriya"),
            TAMIL("Tamil"),
            TELUGU("Telugu"),
            KANNADA("Kannada"),
            MALAYALAM("Malayalam"),
            SINHALA("Sinhala"),
            THAI("Thai"),
            LAO("Lao"),
            TIBETAN("Tibetan"),
            MYANMAR("Myanmar"),
            GEORGIAN("Georgian"),
            HANGULJAMO("HangulJamo"),
            ETHIOPIC("Ethiopic"),
            ETHIOPICSUPPLEMENT("EthiopicSupplement"),
            CHEROKEE("Cherokee"),
            UNIFIEDCANADIANABORIGINALSYLLABICS("UnifiedCanadianAboriginalSyllabics"),
            OGHAM("Ogham"),
            RUNIC("Runic"),
            TAGALOG("Tagalog"),
            HANUNOO("Hanunoo"),
            BUHID("Buhid"),
            TAGBANWA("Tagbanwa"),
            KHMER("Khmer"),
            MONGOLIAN("Mongolian"),
            LIMBU("Limbu"),
            TAILE("TaiLe"),
            NEWTAILUE("NewTaiLue"),
            KHMERSYMBOLS("KhmerSymbols"),
            BUGINESE("Buginese"),
            PHONETICEXTENSIONS("PhoneticExtensions"),
            PHONETICEXTENSIONSSUPPLEMENT("PhoneticExtensionsSupplement"),
            COMBININGDIACRITICALMARKSSUPPLEMENT("CombiningDiacriticalMarksSupplement"),
            LATINEXTENDEDADDITIONAL("LatinExtendedAdditional"),
            GREEKEXTENDED("GreekExtended"),
            GENERALPUNCTUATION("GeneralPunctuation"),
            SUPERSCRIPTSANDSUBSCRIPTS("SuperscriptsandSubscripts"),
            CURRENCYSYMBOLS("CurrencySymbols"),
            COMBININGMARKSFORSYMBOLS("CombiningMarksforSymbols"),
            LETTERLIKESYMBOLS("LetterlikeSymbols"),
            NUMBERFORMS("NumberForms"),
            ARROWS("Arrows"),
            MATHEMATICALOPERATORS("MathematicalOperators"),
            MISCELLANEOUSTECHNICAL("MiscellaneousTechnical"),
            CONTROLPICTURES("ControlPictures"),
            OPTICALCHARACTERRECOGNITION("OpticalCharacterRecognition"),
            ENCLOSEDALPHANUMERICS("EnclosedAlphanumerics"),
            BOXDRAWING("BoxDrawing"),
            BLOCKELEMENTS("BlockElements"),
            GEOMETRICSHAPES("GeometricShapes"),
            MISCELLANEOUSSYMBOLS("MiscellaneousSymbols"),
            DINGBATS("Dingbats"),
            MISCELLANEOUSMATHEMATICALSYMBOLS_A("MiscellaneousMathematicalSymbols-A"),
            SUPPLEMENTALARROWS_A("SupplementalArrows-A"),
            BRAILLEPATTERNS("BraillePatterns"),
            SUPPLEMENTALARROWS_B("SupplementalArrows-B"),
            MISCELLANEOUSMATHEMATICALSYMBOLS_B("MiscellaneousMathematicalSymbols-B"),
            SUPPLEMENTALMATHEMATICALOPERATORS("SupplementalMathematicalOperators"),
            MISCELLANEOUSSYMBOLSANDARROWS("MiscellaneousSymbolsandArrows"),
            GLAGOLITIC("Glagolitic"),
            COPTIC("Coptic"),
            GEORGIANSUPPLEMENT("GeorgianSupplement"),
            TIFINAGH("Tifinagh"),
            ETHIOPICEXTENDED("EthiopicExtended"),
            SUPPLEMENTALPUNCTUATION("SupplementalPunctuation"),
            CJKRADICALSSUPPLEMENT("CJKRadicalsSupplement"),
            KANGXIRADICALS("KangxiRadicals"),
            IDEOGRAPHICDESCRIPTIONCHARACTERS("IdeographicDescriptionCharacters"),
            CJKSYMBOLSANDPUNCTUATION("CJKSymbolsandPunctuation"),
            HIRAGANA("Hiragana"),
            KATAKANA("Katakana"),
            BOPOMOFO("Bopomofo"),
            HANGULCOMPATIBILITYJAMO("HangulCompatibilityJamo"),
            KANBUN("Kanbun"),
            BOPOMOFOEXTENDED("BopomofoExtended"),
            CJKSTROKES("CJKStrokes"),
            KATAKANAPHONETICEXTENSIONS("KatakanaPhoneticExtensions"),
            ENCLOSEDCJKLETTERSANDMONTHS("EnclosedCJKLettersandMonths"),
            CJKCOMPATIBILITY("CJKCompatibility"),
            CJKUNIFIEDIDEOGRAPHSEXTENSIONA("CJKUnifiedIdeographsExtensionA"),
            YIJINGHEXAGRAMSYMBOLS("YijingHexagramSymbols"),
            CJKUNIFIEDIDEOGRAPHS("CJKUnifiedIdeographs"),
            YISYLLABLES("YiSyllables"),
            YIRADICALS("YiRadicals"),
            MODIFIERTONELETTERS("ModifierToneLetters"),
            SYLOTINAGRI("SylotiNagri"),
            HANGULSYLLABLES("HangulSyllables"),
            HIGHSURROGATES("HighSurrogates"),
            HIGHPRIVATEUSESURROGATES("HighPrivateUseSurrogates"),
            LOWSURROGATES("LowSurrogates"),
            PRIVATEUSEAREA("PrivateUseArea"),
            CJKCOMPATIBILITYIDEOGRAPHS("CJKCompatibilityIdeographs"),
            ALPHABETICPRESENTATIONFORMS("AlphabeticPresentationForms"),
            ARABICPRESENTATIONFORMS_A("ArabicPresentationForms-A"),
            VARIATIONSELECTORS("VariationSelectors"),
            VERTICALFORMS("VerticalForms"),
            COMBININGHALFMARKS("CombiningHalfMarks"),
            CJKCOMPATIBILITYFORMS("CJKCompatibilityForms"),
            SMALLFORMVARIANTS("SmallFormVariants"),
            ARABICPRESENTATIONFORMS_B("ArabicPresentationForms-B"),
            HALFWIDTHANDFULLWIDTHFORMS("HalfwidthandFullwidthForms"),
            ALL("all"),
            SPECIALS("Specials"),
            CN("Cn"),
            ISL("IsL"),
            LU("Lu"),
            LL("Ll"),
            LT("Lt"),
            LM("Lm"),
            LO("Lo"),
            ISM("IsM"),
            MN("Mn"),
            ME("Me"),
            MC("Mc"),
            N("N"),
            ND("Nd"),
            NL("Nl"),
            NO("No"),
            ISZ("IsZ"),
            ZS("Zs"),
            ZL("Zl"),
            ZP("Zp"),
            ISC("IsC"),
            CC("Cc"),
            CF("Cf"),
            CO("Co"),
            CS("Cs"),
            ISP("IsP"),
            PD("Pd"),
            PS("Ps"),
            PE("Pe"),
            PC("Pc"),
            PO("Po"),
            ISS("IsS"),
            SM("Sm"),
            SC("Sc"),
            SK("Sk"),
            SO("So"),
            PI("Pi"),
            PF("Pf")
        }

        private fun CharClasses.factory() : CachedCharClass {
            return when (this) {
                CharClasses.LOWER -> CachedLower()
                CharClasses.UPPER -> CachedUpper()
                CharClasses.ASCII -> CachedASCII()
                CharClasses.ALPHA -> CachedAlpha()
                CharClasses.DIGIT -> CachedDigit()
                CharClasses.ALNUM ->  CachedAlnum()
                CharClasses.PUNCT -> CachedPunct()
                CharClasses.GRAPH -> CachedGraph()
                CharClasses.PRINT -> CachedPrint()
                CharClasses.BLANK -> CachedBlank()
                CharClasses.CNTRL -> CachedCntrl()
                CharClasses.XDIGIT -> CachedXDigit()
                CharClasses.SPACE -> CachedSpace()
                CharClasses.WORD -> CachedWord()
                CharClasses.NON_WORD -> CachedNonWord()
                CharClasses.SPACE_SHORT -> CachedSpace()
                CharClasses.NON_SPACE -> CachedNonSpace()
                CharClasses.DIGIT_SHORT -> CachedDigit()
                CharClasses.NON_DIGIT -> CachedNonDigit()
                CharClasses.VERTICAL_WHITESPACE -> CachedVerticalWhitespace()
                CharClasses.NON_VERTICAL_WHITESPACE -> CachedNonVerticalWhitespace()
                CharClasses.HORIZONTAL_WHITESPACE -> CachedHorizontalWhitespace()
                CharClasses.NON_HORIZONTAL_WHITESPACE -> CachedNonHorizontalWhitespace()
                CharClasses.BASIC_LATIN -> CachedRange(0x0000, 0x007F)
                CharClasses.LATIN1_SUPPLEMENT -> CachedRange(0x0080, 0x00FF)
                CharClasses.LATIN_EXTENDED_A -> CachedRange(0x0100, 0x017F)
                CharClasses.LATIN_EXTENDED_B -> CachedRange(0x0180, 0x024F)
                CharClasses.IPA_EXTENSIONS -> CachedRange(0x0250, 0x02AF)
                CharClasses.SPACING_MODIFIER_LETTERS -> CachedRange(0x02B0, 0x02FF)
                CharClasses.COMBINING_DIACRITICAL_MARKS -> CachedRange(0x0300, 0x036F)
                CharClasses.GREEK -> CachedRange(0x0370, 0x03FF)
                CharClasses.CYRILLIC -> CachedRange(0x0400, 0x04FF)
                CharClasses.CYRILLIC_SUPPLEMENT -> CachedRange(0x0500, 0x052F)
                CharClasses.ARMENIAN -> CachedRange(0x0530, 0x058F)
                CharClasses.HEBREW -> CachedRange(0x0590, 0x05FF)
                CharClasses.ARABIC -> CachedRange(0x0600, 0x06FF)
                CharClasses.SYRIAC -> CachedRange(0x0700, 0x074F)
                CharClasses.ARABICSUPPLEMENT -> CachedRange(0x0750, 0x077F)
                CharClasses.THAANA -> CachedRange(0x0780, 0x07BF)
                CharClasses.DEVANAGARI -> CachedRange(0x0900, 0x097F)
                CharClasses.BENGALI -> CachedRange(0x0980, 0x09FF)
                CharClasses.GURMUKHI -> CachedRange(0x0A00, 0x0A7F)
                CharClasses.GUJARATI -> CachedRange(0x0A80, 0x0AFF)
                CharClasses.ORIYA -> CachedRange(0x0B00, 0x0B7F)
                CharClasses.TAMIL -> CachedRange(0x0B80, 0x0BFF)
                CharClasses.TELUGU -> CachedRange(0x0C00, 0x0C7F)
                CharClasses.KANNADA -> CachedRange(0x0C80, 0x0CFF)
                CharClasses.MALAYALAM -> CachedRange(0x0D00, 0x0D7F)
                CharClasses.SINHALA -> CachedRange(0x0D80, 0x0DFF)
                CharClasses.THAI -> CachedRange(0x0E00, 0x0E7F)
                CharClasses.LAO -> CachedRange(0x0E80, 0x0EFF)
                CharClasses.TIBETAN -> CachedRange(0x0F00, 0x0FFF)
                CharClasses.MYANMAR -> CachedRange(0x1000, 0x109F)
                CharClasses.GEORGIAN -> CachedRange(0x10A0, 0x10FF)
                CharClasses.HANGULJAMO -> CachedRange(0x1100, 0x11FF)
                CharClasses.ETHIOPIC -> CachedRange(0x1200, 0x137F)
                CharClasses.ETHIOPICSUPPLEMENT -> CachedRange(0x1380, 0x139F)
                CharClasses.CHEROKEE -> CachedRange(0x13A0, 0x13FF)
                CharClasses.UNIFIEDCANADIANABORIGINALSYLLABICS -> CachedRange(0x1400, 0x167F)
                CharClasses.OGHAM -> CachedRange(0x1680, 0x169F)
                CharClasses.RUNIC -> CachedRange(0x16A0, 0x16FF)
                CharClasses.TAGALOG -> CachedRange(0x1700, 0x171F)
                CharClasses.HANUNOO -> CachedRange(0x1720, 0x173F)
                CharClasses.BUHID -> CachedRange(0x1740, 0x175F)
                CharClasses.TAGBANWA -> CachedRange(0x1760, 0x177F)
                CharClasses.KHMER -> CachedRange(0x1780, 0x17FF)
                CharClasses.MONGOLIAN -> CachedRange(0x1800, 0x18AF)
                CharClasses.LIMBU -> CachedRange(0x1900, 0x194F)
                CharClasses.TAILE -> CachedRange(0x1950, 0x197F)
                CharClasses.NEWTAILUE -> CachedRange(0x1980, 0x19DF)
                CharClasses.KHMERSYMBOLS -> CachedRange(0x19E0, 0x19FF)
                CharClasses.BUGINESE -> CachedRange(0x1A00, 0x1A1F)
                CharClasses.PHONETICEXTENSIONS -> CachedRange(0x1D00, 0x1D7F)
                CharClasses.PHONETICEXTENSIONSSUPPLEMENT -> CachedRange(0x1D80, 0x1DBF)
                CharClasses.COMBININGDIACRITICALMARKSSUPPLEMENT -> CachedRange(0x1DC0, 0x1DFF)
                CharClasses.LATINEXTENDEDADDITIONAL -> CachedRange(0x1E00, 0x1EFF)
                CharClasses.GREEKEXTENDED -> CachedRange(0x1F00, 0x1FFF)
                CharClasses.GENERALPUNCTUATION -> CachedRange(0x2000, 0x206F)
                CharClasses.SUPERSCRIPTSANDSUBSCRIPTS -> CachedRange(0x2070, 0x209F)
                CharClasses.CURRENCYSYMBOLS -> CachedRange(0x20A0, 0x20CF)
                CharClasses.COMBININGMARKSFORSYMBOLS -> CachedRange(0x20D0, 0x20FF)
                CharClasses.LETTERLIKESYMBOLS -> CachedRange(0x2100, 0x214F)
                CharClasses.NUMBERFORMS -> CachedRange(0x2150, 0x218F)
                CharClasses.ARROWS -> CachedRange(0x2190, 0x21FF)
                CharClasses.MATHEMATICALOPERATORS -> CachedRange(0x2200, 0x22FF)
                CharClasses.MISCELLANEOUSTECHNICAL -> CachedRange(0x2300, 0x23FF)
                CharClasses.CONTROLPICTURES -> CachedRange(0x2400, 0x243F)
                CharClasses.OPTICALCHARACTERRECOGNITION -> CachedRange(0x2440, 0x245F)
                CharClasses.ENCLOSEDALPHANUMERICS -> CachedRange(0x2460, 0x24FF)
                CharClasses.BOXDRAWING -> CachedRange(0x2500, 0x257F)
                CharClasses.BLOCKELEMENTS -> CachedRange(0x2580, 0x259F)
                CharClasses.GEOMETRICSHAPES -> CachedRange(0x25A0, 0x25FF)
                CharClasses.MISCELLANEOUSSYMBOLS -> CachedRange(0x2600, 0x26FF)
                CharClasses.DINGBATS -> CachedRange(0x2700, 0x27BF)
                CharClasses.MISCELLANEOUSMATHEMATICALSYMBOLS_A -> CachedRange(0x27C0, 0x27EF)
                CharClasses.SUPPLEMENTALARROWS_A -> CachedRange(0x27F0, 0x27FF)
                CharClasses.BRAILLEPATTERNS -> CachedRange(0x2800, 0x28FF)
                CharClasses.SUPPLEMENTALARROWS_B -> CachedRange(0x2900, 0x297F)
                CharClasses.MISCELLANEOUSMATHEMATICALSYMBOLS_B -> CachedRange(0x2980, 0x29FF)
                CharClasses.SUPPLEMENTALMATHEMATICALOPERATORS -> CachedRange(0x2A00, 0x2AFF)
                CharClasses.MISCELLANEOUSSYMBOLSANDARROWS -> CachedRange(0x2B00, 0x2BFF)
                CharClasses.GLAGOLITIC -> CachedRange(0x2C00, 0x2C5F)
                CharClasses.COPTIC -> CachedRange(0x2C80, 0x2CFF)
                CharClasses.GEORGIANSUPPLEMENT -> CachedRange(0x2D00, 0x2D2F)
                CharClasses.TIFINAGH -> CachedRange(0x2D30, 0x2D7F)
                CharClasses.ETHIOPICEXTENDED -> CachedRange(0x2D80, 0x2DDF)
                CharClasses.SUPPLEMENTALPUNCTUATION -> CachedRange(0x2E00, 0x2E7F)
                CharClasses.CJKRADICALSSUPPLEMENT -> CachedRange(0x2E80, 0x2EFF)
                CharClasses.KANGXIRADICALS -> CachedRange(0x2F00, 0x2FDF)
                CharClasses.IDEOGRAPHICDESCRIPTIONCHARACTERS -> CachedRange(0x2FF0, 0x2FFF)
                CharClasses.CJKSYMBOLSANDPUNCTUATION -> CachedRange(0x3000, 0x303F)
                CharClasses.HIRAGANA -> CachedRange(0x3040, 0x309F)
                CharClasses.KATAKANA -> CachedRange(0x30A0, 0x30FF)
                CharClasses.BOPOMOFO -> CachedRange(0x3100, 0x312F)
                CharClasses.HANGULCOMPATIBILITYJAMO -> CachedRange(0x3130, 0x318F)
                CharClasses.KANBUN -> CachedRange(0x3190, 0x319F)
                CharClasses.BOPOMOFOEXTENDED -> CachedRange(0x31A0, 0x31BF)
                CharClasses.CJKSTROKES -> CachedRange(0x31C0, 0x31EF)
                CharClasses.KATAKANAPHONETICEXTENSIONS -> CachedRange(0x31F0, 0x31FF)
                CharClasses.ENCLOSEDCJKLETTERSANDMONTHS -> CachedRange(0x3200, 0x32FF)
                CharClasses.CJKCOMPATIBILITY -> CachedRange(0x3300, 0x33FF)
                CharClasses.CJKUNIFIEDIDEOGRAPHSEXTENSIONA -> CachedRange(0x3400, 0x4DB5)
                CharClasses.YIJINGHEXAGRAMSYMBOLS -> CachedRange(0x4DC0, 0x4DFF)
                CharClasses.CJKUNIFIEDIDEOGRAPHS -> CachedRange(0x4E00, 0x9FFF)
                CharClasses.YISYLLABLES -> CachedRange(0xA000, 0xA48F)
                CharClasses.YIRADICALS -> CachedRange(0xA490, 0xA4CF)
                CharClasses.MODIFIERTONELETTERS -> CachedRange(0xA700, 0xA71F)
                CharClasses.SYLOTINAGRI -> CachedRange(0xA800, 0xA82F)
                CharClasses.HANGULSYLLABLES -> CachedRange(0xAC00, 0xD7A3)
                CharClasses.HIGHSURROGATES -> CachedRange(0xD800, 0xDB7F)
                CharClasses.HIGHPRIVATEUSESURROGATES -> CachedRange(0xDB80, 0xDBFF)
                CharClasses.LOWSURROGATES -> CachedRange(0xDC00, 0xDFFF)
                CharClasses.PRIVATEUSEAREA -> CachedRange(0xE000, 0xF8FF)
                CharClasses.CJKCOMPATIBILITYIDEOGRAPHS -> CachedRange(0xF900, 0xFAFF)
                CharClasses.ALPHABETICPRESENTATIONFORMS -> CachedRange(0xFB00, 0xFB4F)
                CharClasses.ARABICPRESENTATIONFORMS_A -> CachedRange(0xFB50, 0xFDFF)
                CharClasses.VARIATIONSELECTORS -> CachedRange(0xFE00, 0xFE0F)
                CharClasses.VERTICALFORMS -> CachedRange(0xFE10, 0xFE1F)
                CharClasses.COMBININGHALFMARKS -> CachedRange(0xFE20, 0xFE2F)
                CharClasses.CJKCOMPATIBILITYFORMS -> CachedRange(0xFE30, 0xFE4F)
                CharClasses.SMALLFORMVARIANTS -> CachedRange(0xFE50, 0xFE6F)
                CharClasses.ARABICPRESENTATIONFORMS_B -> CachedRange(0xFE70, 0xFEFF)
                CharClasses.HALFWIDTHANDFULLWIDTHFORMS -> CachedRange(0xFF00, 0xFFEF)
                CharClasses.ALL -> CachedRange(0x00, 0x10FFFF)
                CharClasses.SPECIALS -> CachedSpecialsBlock()
                CharClasses.CN -> CachedCategory(CharCategory.UNASSIGNED.value, true)
                CharClasses.ISL -> CachedCategoryScope(0x3E, true)
                CharClasses.LU -> CachedCategory(CharCategory.UPPERCASE_LETTER.value, true)
                CharClasses.LL -> CachedCategory(CharCategory.LOWERCASE_LETTER.value, true)
                CharClasses.LT -> CachedCategory(CharCategory.TITLECASE_LETTER.value, false)
                CharClasses.LM -> CachedCategory(CharCategory.MODIFIER_LETTER.value, false)
                CharClasses.LO -> CachedCategory(CharCategory.OTHER_LETTER.value, true)
                CharClasses.ISM -> CachedCategoryScope(0x1C0, true)
                CharClasses.MN -> CachedCategory(CharCategory.NON_SPACING_MARK.value, true)
                CharClasses.ME -> CachedCategory(CharCategory.ENCLOSING_MARK.value, false)
                CharClasses.MC -> CachedCategory(CharCategory.COMBINING_SPACING_MARK.value, true)
                CharClasses.N -> CachedCategoryScope(0xE00, true)
                CharClasses.ND -> CachedCategory(CharCategory.DECIMAL_DIGIT_NUMBER.value, true)
                CharClasses.NL -> CachedCategory(CharCategory.LETTER_NUMBER.value, true)
                CharClasses.NO -> CachedCategory(CharCategory.OTHER_NUMBER.value, true)
                CharClasses.ISZ -> CachedCategoryScope(0x7000, false)
                CharClasses.ZS -> CachedCategory(CharCategory.SPACE_SEPARATOR.value, false)
                CharClasses.ZL -> CachedCategory(CharCategory.LINE_SEPARATOR.value, false)
                CharClasses.ZP -> CachedCategory(CharCategory.PARAGRAPH_SEPARATOR.value, false)
                CharClasses.ISC -> CachedCategoryScope(0xF0000, true, true)
                CharClasses.CC -> CachedCategory(CharCategory.CONTROL.value, false)
                CharClasses.CF -> CachedCategory(CharCategory.FORMAT.value, true)
                CharClasses.CO -> CachedCategory(CharCategory.PRIVATE_USE.value, true)
                CharClasses.CS -> CachedCategory(CharCategory.SURROGATE.value, false, true)
                CharClasses.ISP -> CachedCategoryScope((1 shl CharCategory.DASH_PUNCTUATION.value) or (1 shl CharCategory.START_PUNCTUATION.value) or (1 shl CharCategory.END_PUNCTUATION.value) or (1 shl CharCategory.CONNECTOR_PUNCTUATION.value) or (1 shl CharCategory.OTHER_PUNCTUATION.value) or (1 shl CharCategory.INITIAL_QUOTE_PUNCTUATION.value) or (1 shl CharCategory.FINAL_QUOTE_PUNCTUATION.value), true)
                CharClasses.PD -> CachedCategory(CharCategory.DASH_PUNCTUATION.value, false)
                CharClasses.PS -> CachedCategory(CharCategory.START_PUNCTUATION.value, false)
                CharClasses.PE -> CachedCategory(CharCategory.END_PUNCTUATION.value, false)
                CharClasses.PC -> CachedCategory(CharCategory.CONNECTOR_PUNCTUATION.value, false)
                CharClasses.PO -> CachedCategory(CharCategory.OTHER_PUNCTUATION.value, true)
                CharClasses.ISS -> CachedCategoryScope(0x7E000000, true)
                CharClasses.SM -> CachedCategory(CharCategory.MATH_SYMBOL.value, true)
                CharClasses.SC -> CachedCategory(CharCategory.CURRENCY_SYMBOL.value, false)
                CharClasses.SK -> CachedCategory(CharCategory.MODIFIER_SYMBOL.value, false)
                CharClasses.SO -> CachedCategory(CharCategory.OTHER_SYMBOL.value, true)
                CharClasses.PI -> CachedCategory(CharCategory.INITIAL_QUOTE_PUNCTUATION.value, false)
                CharClasses.PF -> CachedCategory(CharCategory.FINAL_QUOTE_PUNCTUATION.value, false)
            }
        }

        private val classCache = Array<AtomicReference<CachedCharClass?>>(CharClasses.entries.size, {
            AtomicReference<CachedCharClass?>(null)
        })
        private val classCacheMap = CharClasses.entries.associate { it -> it.regexName to it }

        fun intersects(ch1: Int, ch2: Int): Boolean = ch1 == ch2
        fun intersects(cc: AbstractCharClass, ch: Int): Boolean = cc.contains(ch)

        fun intersects(cc1: AbstractCharClass, cc2: AbstractCharClass): Boolean {
            if (cc1.bits == null || cc2.bits == null) {
                return true
            }
            return cc1.bits!!.intersects(cc2.bits!!)
        }

        fun getPredefinedClass(name: String, negative: Boolean): AbstractCharClass {
            val charClass = classCacheMap[name] ?: throw PatternSyntaxException("No such character class")
            val cachedClass = classCache[charClass.ordinal].load() ?: run {
                classCache[charClass.ordinal].compareAndSet(null, charClass.factory())
                classCache[charClass.ordinal].load()!!
            }
            return cachedClass.getValue(negative)
        }
    }
}
