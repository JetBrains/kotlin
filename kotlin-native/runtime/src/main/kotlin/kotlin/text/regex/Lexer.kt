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
// TODO: Licenses.

@file:Suppress("DEPRECATION") // Char.toInt()
package kotlin.text.regex

// Access to the decomposition tables. =========================================================================
/** Gets canonical class for given codepoint from decomposition mappings table. */
@SymbolName("Kotlin_text_regex_getCanonicalClassInternal")
external private fun getCanonicalClassInternal(ch: Int): Int

/** Check if the given character is in table of single decompositions. */
@SymbolName("Kotlin_text_regex_hasSingleCodepointDecompositionInternal")
external private fun hasSingleCodepointDecompositionInternal(ch: Int): Boolean

/** Returns a decomposition for a given codepoint. */
@SymbolName("Kotlin_text_regex_getDecompositionInternal")
external private fun getDecompositionInternal(ch: Int): IntArray?

/**
 * Decomposes the given string represented as an array of codepoints. Saves the decomposition into [outputCodepoints] array.
 * Returns the length of the decomposition.
 */
@SymbolName("Kotlin_text_regex_decomposeString")
external private fun decomposeString(inputCodePoints: IntArray, inputLength: Int, outputCodePoints: IntArray): Int
// =============================================================================================================

/**
 * This is base class for special tokens like character classes and quantifiers.
 */
internal abstract class SpecialToken {

    /**
     * Returns the type of the token, may return following values:
     * TOK_CHARCLASS  - token representing character class;
     * TOK_QUANTIFIER - token representing quantifier;
     */
    abstract val type: Type

    enum class Type {
        CHARCLASS,
        QUANTIFIER
    }
}

internal class Lexer(val patternString: String, flags: Int) {

    // The property is set in the init block after some transformations over the pattern string.
    private val pattern: CharArray

    var flags = flags
        private set

    // Modes ===========================================================================================================
    enum class Mode {
        PATTERN,
        RANGE,
        ESCAPE
    }

    /**
     * Mode: whether the lexer processes character range ([Mode.RANGE]), escaped sequence ([Mode.RANGE])
     * or any other part of a regex ([PATTERN]).
     */
    var mode = Mode.PATTERN
        private set

    /** When in [Mode.ESCAPE] mode, this field will save the previous one */
    private var savedMode = Mode.PATTERN

    fun setModeWithReread(value: Mode) {
        if(value == Mode.PATTERN || value == Mode.RANGE) {
            mode = value
        }
        if (mode == Mode.PATTERN) {
            reread()
        }
    }

    // Tokens ==========================================================================================================
    internal var lookBack: Int = 0           // Previous char read.
        private set
    internal var currentChar: Int = 0        // Current character read. Returns 0 if there is no more characters.
        private set
    internal var lookAhead: Int = 0          // Next character.
        private set

    internal var curSpecialToken: SpecialToken? = null        // Current special token (e.g. quantifier)
        private set
    internal var lookAheadSpecialToken: SpecialToken? = null  // Next special token
        private set

    // Indices in the pattern.
    var index = 0                   // Current char being processed index.
        private set
    var prevNonWhitespaceIndex = 0  // Previous non-whitespace character index.
        private set
    var curTokenIndex = 0           // Current token start index.
        private set
    var lookAheadTokenIndex = 0     // Next token index.
        private set

    init {
        var processedPattern = patternString
        if (flags and Pattern.LITERAL > 0) {
            processedPattern = Pattern.quote(patternString)
        } else if (flags and Pattern.CANON_EQ > 0) {
            processedPattern = Lexer.normalize(patternString)
        }

        this.pattern = processedPattern.toCharArray().copyOf(processedPattern.length + 2)
        this.pattern[this.pattern.size - 1] = 0.toChar()
        this.pattern[this.pattern.size - 2] = 0.toChar()

        // Read first two tokens.
        movePointer()
        movePointer()
    }

    // Character checks ================================================================================================
    /** Returns true, if current token is special, i.e. quantifier, or other compound token. */
    val isSpecial: Boolean      get() = curSpecialToken != null
    val isQuantifier: Boolean   get() = isSpecial && curSpecialToken!!.type == SpecialToken.Type.QUANTIFIER
    val isNextSpecial: Boolean  get() = lookAheadSpecialToken != null

    private fun Int.isSurrogatePair() : Boolean {
        val high = (this ushr 16).toChar()
        val low = this.toChar()
        return high.isHighSurrogate() && low.isLowSurrogate()
    }

    private fun Char.isLineSeparator(): Boolean =
        this == '\n' || this == '\r' || this == '\u0085' || this.toInt() or 1 == '\u2029'.toInt()

    /** Checks if there are any characters in the pattern. */
    fun isEmpty(): Boolean =
        currentChar == 0 && lookAhead == 0 && index >= pattern.size && !isSpecial

    /** Return true if the current character is letter, false otherwise .*/
    fun isLetter(): Boolean =
        !isEmpty() && !isSpecial && isLetter(currentChar)

    /** Check if the current char is high/low surrogate. */
    fun isHighSurrogate(): Boolean = currentChar in 0xDBFF..0xD800
    fun isLowSurrogate(): Boolean = currentChar in 0xDFFF..0xDC00
    fun isSurrogate(): Boolean = isHighSurrogate() || isLowSurrogate()

    /**
     * Restores flags for Lexer
     * @param flags
     */
    fun restoreFlags(flags: Int) {
        this.flags = flags
        lookAhead = currentChar
        lookAheadSpecialToken = curSpecialToken

        // curTokenIndex is an index of closing bracket ')'
        index = curTokenIndex + 1
        lookAheadTokenIndex = curTokenIndex
        movePointer()
    }

    override fun toString(): String {
        return patternString
    }

    // Processing index moving =========================================================================================
    /** Returns current character and moves string index to the next one. */
    operator fun next(): Int {
        movePointer()
        return lookBack
    }

    /** Returns current special token and moves string index to the next one */
    fun nextSpecial(): SpecialToken? {
        val res = curSpecialToken
        movePointer()
        return res
    }

    /**
     * Reread current character. May be required if a previous token changes mode
     * to one with different character interpretation.
     */
    private fun reread() {
        lookAhead = currentChar
        lookAheadSpecialToken = curSpecialToken
        index = lookAheadTokenIndex
        lookAheadTokenIndex = curTokenIndex
        movePointer()
    }

    /**
     * Returns the next character index to read and moves pointer to the next one.
     * If comments flag is on this method will skip comments and whitespaces.
     *
     * The following actions are equivalent if comments flag is off:
     * currentChar = pattern[index++] == currentChar = pattern[nextIndex]
     */
    private fun nextIndex(): Int {
        prevNonWhitespaceIndex = index
        if (flags and Pattern.COMMENTS != 0) {
            skipComments()
        } else {
            index++
        }
        return prevNonWhitespaceIndex
    }

    /** Skips comments and whitespaces */
    private fun skipComments(): Int {
        val length = pattern.size - 2
        index++
        do {
            while (index < length && pattern[index].isWhitespace()) {
                index++
            }
            if (index < length && pattern[index] == '#') {
                index++
                while (index < length && !pattern[index].isLineSeparator()) {
                    index++
                }
            } else {
                return index
            }
        } while (true)
    }

    /**
     * Returns the next code point in the pattern string.
     */
    private fun nextCodePoint(): Int {
        val high = pattern[nextIndex()] // nextIndex skips comments and whitespaces if comments flag is on.
        if (high.isHighSurrogate()) {
            // Low and high chars may be delimited by spaces.
            val lowExpectedIndex = prevNonWhitespaceIndex + 1
            if (lowExpectedIndex < pattern.size) {
                val low = pattern[lowExpectedIndex]
                if (low.isLowSurrogate()) {
                    nextIndex()
                    return Char.toCodePoint(high, low)
                }
            }
        }
        return high.toInt()
    }

    /**
     * Moves pointer one position right. Saves the current character to [lookBack],
     * [lookAhead] to the current one and finally read one more to [lookAhead].
     */
    private fun movePointer() {
        // swap pointers
        lookBack = currentChar
        currentChar = lookAhead
        curSpecialToken = lookAheadSpecialToken
        curTokenIndex = lookAheadTokenIndex
        lookAheadTokenIndex = index
        var reread: Boolean
        do {
            // Read the next character, analyze it and construct a token.
            lookAhead = if (index < pattern.size) nextCodePoint() else 0
            lookAheadSpecialToken = null

            if (mode == Mode.ESCAPE) {
                processInEscapeMode()
            }

            reread = when (mode) {
                Mode.PATTERN -> processInPatternMode()
                Mode.RANGE -> processInRangeMode()
                else -> false
            }
        } while (reread)
    }

    // Special functions called from [movePointer] function to process chars in different modes ========================
    /**
     * Processing an escaped sequence like "\Q foo \E". Just skip a character if it is not \E.
     * Returns whether we need to reread the character or not
     */
    private fun processInEscapeMode(): Boolean {
        if (lookAhead == '\\'.toInt()) {
            // Need not care about supplementary code points here.
            val lookAheadChar: Char = if (index < pattern.size) pattern[nextIndex()] else '\u0000'
            lookAhead = lookAheadChar.toInt()

            if (lookAheadChar == 'E') {
                // If \E found - change the mode to the previous one and shift to the next char.
                mode = savedMode
                lookAhead = if (index <= pattern.size - 2) nextCodePoint() else 0
            } else {
                // If \ have no E - make a step back and return.
                lookAhead = '\\'.toInt()
                index = prevNonWhitespaceIndex
            }
        }
        return false
    }

    /** Processes a next character in [Mode.PATTERN] mode. Returns whether we need to reread the character or not */
    private fun processInPatternMode(): Boolean {
        if (lookAhead.isSurrogatePair()) {
            return false
        }
        val lookAheadChar = lookAhead.toChar()

        if (lookAheadChar == '\\') {
            return processEscapedChar()
        }

        // TODO: Look like we can create a quantifier here.
        when (lookAheadChar) {
            // Quantifier (*, +, ?).
            '+', '*', '?' -> {
                val mode = if (index < pattern.size) pattern[index] else '*'
                // look at the next character to determine if the mode is greedy, reluctant or possessive.
                when (mode) {
                    '+' -> { lookAhead = lookAhead or Lexer.QMOD_POSSESSIVE; nextIndex() }
                    '?' -> { lookAhead = lookAhead or Lexer.QMOD_RELUCTANT;  nextIndex() }
                    else ->  lookAhead = lookAhead or Lexer.QMOD_GREEDY
                }
            }

            // Quantifier ({x,y}).
            '{' -> lookAheadSpecialToken = processQuantifier()

            // $.
            '$' -> lookAhead = CHAR_DOLLAR

            // A group or a special construction.
            '(' -> {
                if (pattern[index] != '?') {
                    // Group
                    lookAhead = CHAR_LEFT_PARENTHESIS
                } else {
                    // Special constructs (non-capturing groups, look ahead/look behind etc).
                    nextIndex()
                    var char = pattern[index]
                    var isLookBehind = false
                    do {
                        if (!isLookBehind) {
                            when (char) {
                                // Look ahead or an atomic group.
                                '!' -> { lookAhead = CHAR_NEG_LOOKAHEAD; nextIndex() }
                                '=' -> { lookAhead = CHAR_POS_LOOKAHEAD; nextIndex() }
                                '>' -> { lookAhead = CHAR_ATOMIC_GROUP;  nextIndex() }
                                // Positive / negaitve look behind - need to check the next char.
                                '<' -> {
                                    nextIndex()
                                    char = pattern[index]
                                    isLookBehind = true
                                }
                                // Flags.
                                else -> {
                                    lookAhead = readFlags()

                                    // We return `res = res or 1 shl 8` from readFlags() if we read (?idmsux-idmsux)
                                    if (lookAhead >= 256) {
                                        // Just flags (no non-capturing group with them). Erase auxiliary bit.
                                        lookAhead = lookAhead and 0xff
                                        flags = lookAhead
                                        lookAhead = lookAhead shl 16
                                        lookAhead = CHAR_FLAGS or lookAhead
                                    } else {
                                        // A non-capturing group with flags: (?<flags>:Foo)
                                        flags = lookAhead
                                        lookAhead = lookAhead shl 16
                                        lookAhead = CHAR_NONCAP_GROUP or lookAhead
                                    }
                                }
                            }
                        } else {
                            // Process the second char for look behind construction.
                            isLookBehind = false
                            when (char) {
                                '!' -> {  lookAhead = CHAR_NEG_LOOKBEHIND; nextIndex() }
                                '=' -> {  lookAhead = CHAR_POS_LOOKBEHIND; nextIndex() }
                                else -> throw PatternSyntaxException("Unknown look behind", patternString, curTokenIndex)
                            }
                        }
                    } while (isLookBehind)
                }
            }

            ')' -> lookAhead = CHAR_RIGHT_PARENTHESIS
            '[' -> { lookAhead = CHAR_LEFT_SQUARE_BRACKET; mode = Mode.RANGE }
            '^' -> lookAhead = CHAR_CARET
            '|' -> lookAhead = CHAR_VERTICAL_BAR
            '.' -> lookAhead = CHAR_DOT
        }
        return false
    }

    /** Processes a character inside a range. Returns whether we need to reread the character or not */
    private fun processInRangeMode(): Boolean {
        if (lookAhead.isSurrogatePair()) {
            return false
        }
        val lookAheadChar = lookAhead.toChar()

        when (lookAheadChar) {
            '\\' -> return processEscapedChar()
            '['  -> lookAhead = CHAR_LEFT_SQUARE_BRACKET
            ']'  -> lookAhead = CHAR_RIGHT_SQUARE_BRACKET
            '^'  -> lookAhead = CHAR_CARET
            '&'  -> lookAhead = CHAR_AMPERSAND
            '-'  -> lookAhead = CHAR_HYPHEN
        }
        return false
    }

    /** Processes an escaped (\x) character in any mode. Returns whether we need to reread the character or not */
    private fun processEscapedChar() : Boolean {
        lookAhead = if (index < pattern.size - 2) {
            nextCodePoint()
        } else {
            throw PatternSyntaxException("Trailing \\", patternString, curTokenIndex)
        }

        // The current code point cannot be a surrogate pair because it is an escaped special one.
        // Cast it to char or just skip it as if we pass through the else branch of the when below.
        if (lookAhead.isSurrogatePair()) {
            return false
        }
        val lookAheadChar = lookAhead.toChar()

        when (lookAheadChar) {
            // Character class.
            'P', 'p' -> {
                val cs = parseCharClassName()
                val negative = lookAheadChar == 'P'

                lookAheadSpecialToken = AbstractCharClass.getPredefinedClass(cs, negative)
                lookAhead = 0
            }

            // Word/whitespace/digit.
            'w', 's', 'd', 'W', 'S', 'D' -> {
                lookAheadSpecialToken = AbstractCharClass.getPredefinedClass(
                        pattern.concatToString(prevNonWhitespaceIndex, prevNonWhitespaceIndex + 1),
                        false
                )
                lookAhead = 0
            }

            // Enter in ESCAPE mode. Skip this \Q symbol.
            'Q' -> {
                savedMode = mode
                mode = Mode.ESCAPE
                return true
            }

            // Special characters like tab, new line etc.
            't' -> lookAhead = '\t'.toInt()
            'n' -> lookAhead = '\n'.toInt()
            'r' -> lookAhead = '\r'.toInt()
            'f' -> lookAhead = '\u000C'.toInt()
            'a' -> lookAhead = '\u0007'.toInt()
            'e' -> lookAhead = '\u001B'.toInt()

            // Back references to capturing groups.
            '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                if (mode == Mode.PATTERN) {
                    lookAhead = 0x80000000.toInt() or lookAhead  // Captured group reference is 0x80...<group number>
                }
            }

            // A literal: octal, hex, or hex unicode.
            '0' -> lookAhead = readOctals()
            'x' -> lookAhead = readHex("hexadecimal", 2)
            'u' -> lookAhead = readHex("Unicode", 4)

            // Special characters like EOL, EOI etc
            'b' -> lookAhead = CHAR_WORD_BOUND
            'B' -> lookAhead = CHAR_NONWORD_BOUND
            'A' -> lookAhead = CHAR_START_OF_INPUT
            'G' -> lookAhead = CHAR_PREVIOUS_MATCH
            'Z' -> lookAhead = CHAR_END_OF_LINE
            'z' -> lookAhead = CHAR_END_OF_INPUT

            // \cx - A control character corresponding to x.
            'c' -> {
                if (index < pattern.size - 2) {
                    // Need not care about supplementary codepoints here.
                    lookAhead = pattern[nextIndex()].toInt() and 0x1f
                } else {
                    throw PatternSyntaxException("Illegal control sequence", patternString, curTokenIndex)
                }
            }

            'C', 'E', 'F', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'R', 'T', 'U', 'V', 'X', 'Y', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'o', 'q', 'y' ->
                throw PatternSyntaxException("Illegal escape sequence", patternString, curTokenIndex)
        }
        return false
    }

    /** Process [lookAhead] in assumption that it's quantifier. */
    private fun processQuantifier(): Quantifier {
        assert(lookAhead == '{'.toInt())
        val sb = StringBuilder(4)
        var min = -1
        var max = -1

        // Obtain a min value.
        var char: Char = if (index < pattern.size) {
            pattern[nextIndex()]
        } else {
            throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
        }
        while (char != '}') {

            if (char == ',' && min < 0) {
                try {
                    val minParsed = sb.toString().toInt()
                    min = if (minParsed >= 0) minParsed else throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
                    sb.setLength(0)
                } catch (nfe: NumberFormatException) {
                    throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
                }
            } else {
                sb.append(char)
            }
            char = if (index < pattern.size) pattern[nextIndex()] else break
        }

        if (char != '}') {
            throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
        }

        // Obtain a max value, if it exists
        if (sb.isNotEmpty()) {
            try {
                val maxParsed = sb.toString().toInt()
                max = if (maxParsed >= 0) maxParsed else throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
                if (min < 0) {
                    min = max
                }
            } catch (nfe: NumberFormatException) {
                throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
            }
        }

        if (min < 0 || max >=0 && max < min) {
            throw PatternSyntaxException("Incorrect Quantifier Syntax", patternString, curTokenIndex)
        }

        val mod = if (index < pattern.size) pattern[index] else '*'
        when (mod) {
            '+' -> { lookAhead = Lexer.QUANT_COMP_P; nextIndex() }
            '?' -> { lookAhead = Lexer.QUANT_COMP_R; nextIndex() }
            else ->  lookAhead = Lexer.QUANT_COMP
        }
        return Quantifier(min, max)
    }

    // Reading methods for specific tokens =============================================================================
    /** Process expression flags given with (?idmsux-idmsux). Returns the flags processed. */
    private fun readFlags(): Int {
        var positive = true
        var result = flags

        while (index < pattern.size) {
            val char = pattern[index]
            when (char) {
                '-' -> {
                    if (!positive) {
                        throw PatternSyntaxException("Illegal inline construct", patternString, curTokenIndex)
                    }
                    positive = false
                }

                'i' -> result = if (positive)
                                    result or Pattern.CASE_INSENSITIVE
                                else
                                    result xor Pattern.CASE_INSENSITIVE and result

                'd' -> result = if (positive)
                                    result or Pattern.UNIX_LINES
                                else
                                    result xor Pattern.UNIX_LINES and result

                'm' -> result = if (positive)
                                    result or Pattern.MULTILINE
                                else
                                    result xor Pattern.MULTILINE and result

                's' -> result = if (positive)
                                    result or Pattern.DOTALL
                                else
                                    result xor Pattern.DOTALL and result

                // We don't support UNICODE_CASE.
                /*'u' -> result = if (positive)
                                    result or Pattern.UNICODE_CASE
                                else
                                    result xor Pattern.UNICODE_CASE and result*/

                'x' -> result = if (positive)
                                    result or Pattern.COMMENTS
                                else
                                    result xor Pattern.COMMENTS and result

                ':' -> {
                    nextIndex()
                    return result
                }

                ')' -> {
                    nextIndex()
                    return result or (1 shl 8)
                }
            }
            nextIndex()
        }
        throw PatternSyntaxException("Illegal inline construct", patternString, curTokenIndex)
    }

    /** Parse character classes names and verifies correction of the syntax */
    private fun parseCharClassName(): String {
        val sb = StringBuilder(10)
        if (index < pattern.size - 2) {
            // one symbol family
            if (pattern[index] != '{') {
                return "Is${pattern[nextIndex()]}"
            }

            nextIndex() // Skip '{'
            var char = pattern[nextIndex()]
            while (index < pattern.size - 2 && char != '}') {
                sb.append(char)
                char = pattern[nextIndex()]
            }
            if (char != '}') throw PatternSyntaxException("Unclosed character family", patternString, curTokenIndex)
        }
        if (sb.isEmpty()) throw PatternSyntaxException("Empty character family", patternString, curTokenIndex)

        val res = sb.toString()
        return when {
            res.length == 1 -> "Is$res"
            res.length > 3 && (res.startsWith("Is") || res.startsWith("In")) -> res.substring(2)
            else -> res
        }
    }

    /** Process hexadecimal integer. */
    private fun readHex(radixName: String, max: Int): Int {
        val builder = StringBuilder(max)
        val length = pattern.size - 2
        var i = 0
        while (i < max && index < length) {
            builder.append(pattern[nextIndex()])
            i++
        }
        if (i == max) {
            try {
                return builder.toString().toInt(16)
            } catch (e: NumberFormatException) {}
        }
        throw PatternSyntaxException("Invalid $radixName escape sequence", patternString, curTokenIndex)
    }

    /** Process octal integer. */
    private fun readOctals(): Int {
        val length = pattern.size - 2
        var result = 0
        var digit = digitOf(pattern[index], 8)
        if (digit == -1) {
            throw PatternSyntaxException("Invalid octal escape sequence", patternString, curTokenIndex)
        }
        val max = if (digit > 3) 2 else 3
        var i = 0
        while (i < max && index < length && digit != -1) {
            result *= 8
            result += digit
            nextIndex()
            digit = digitOf(pattern[index], 8)
            i++
        }
        return result
    }

    companion object {
        // Special characters.
        val CHAR_DOLLAR               = 0xe0000000.toInt() or '$'.toInt()
        val CHAR_RIGHT_PARENTHESIS    = 0xe0000000.toInt() or ')'.toInt()
        val CHAR_LEFT_SQUARE_BRACKET  = 0xe0000000.toInt() or '['.toInt()
        val CHAR_RIGHT_SQUARE_BRACKET = 0xe0000000.toInt() or ']'.toInt()
        val CHAR_CARET                = 0xe0000000.toInt() or '^'.toInt()
        val CHAR_VERTICAL_BAR         = 0xe0000000.toInt() or '|'.toInt()
        val CHAR_AMPERSAND            = 0xe0000000.toInt() or '&'.toInt()
        val CHAR_HYPHEN               = 0xe0000000.toInt() or '-'.toInt()
        val CHAR_DOT                  = 0xe0000000.toInt() or '.'.toInt()
        val CHAR_LEFT_PARENTHESIS     = 0x80000000.toInt() or '('.toInt()
        val CHAR_NONCAP_GROUP         = 0xc0000000.toInt() or '('.toInt()
        val CHAR_POS_LOOKAHEAD        = 0xe0000000.toInt() or '('.toInt()
        val CHAR_NEG_LOOKAHEAD        = 0xf0000000.toInt() or '('.toInt()
        val CHAR_POS_LOOKBEHIND       = 0xf8000000.toInt() or '('.toInt()
        val CHAR_NEG_LOOKBEHIND       = 0xfc000000.toInt() or '('.toInt()
        val CHAR_ATOMIC_GROUP         = 0xfe000000.toInt() or '('.toInt()
        val CHAR_FLAGS                = 0xff000000.toInt() or '('.toInt()
        val CHAR_START_OF_INPUT       = 0x80000000.toInt() or 'A'.toInt()
        val CHAR_WORD_BOUND           = 0x80000000.toInt() or 'b'.toInt()
        val CHAR_NONWORD_BOUND        = 0x80000000.toInt() or 'B'.toInt()
        val CHAR_PREVIOUS_MATCH       = 0x80000000.toInt() or 'G'.toInt()
        val CHAR_END_OF_INPUT         = 0x80000000.toInt() or 'z'.toInt()
        val CHAR_END_OF_LINE          = 0x80000000.toInt() or 'Z'.toInt()

        // Quantifier modes.
        val QMOD_GREEDY     = 0xe0000000.toInt()
        val QMOD_RELUCTANT  = 0xc0000000.toInt()
        val QMOD_POSSESSIVE = 0x80000000.toInt()

        // Quantifiers.
        val QUANT_STAR   = QMOD_GREEDY or '*'.toInt()
        val QUANT_STAR_P = QMOD_POSSESSIVE or '*'.toInt()
        val QUANT_STAR_R = QMOD_RELUCTANT or '*'.toInt()
        val QUANT_PLUS   = QMOD_GREEDY or '+'.toInt()
        val QUANT_PLUS_P = QMOD_POSSESSIVE or '+'.toInt()
        val QUANT_PLUS_R = QMOD_RELUCTANT or '+'.toInt()
        val QUANT_ALT    = QMOD_GREEDY or '?'.toInt()
        val QUANT_ALT_P  = QMOD_POSSESSIVE or '?'.toInt()
        val QUANT_ALT_R  = QMOD_RELUCTANT or '?'.toInt()
        val QUANT_COMP   = QMOD_GREEDY or '{'.toInt()
        val QUANT_COMP_P = QMOD_POSSESSIVE or '{'.toInt()
        val QUANT_COMP_R = QMOD_RELUCTANT or '{'.toInt()

        /** Returns true if [ch] is a plain token. */
        fun isLetter(ch: Int): Boolean {
            // All supplementary codepoints have integer value that is >= 0.
            return ch >= 0
        }

        private fun String.codePointAt(index: Int): Int {
            val high = this[index]
            if (high.isHighSurrogate() && index + 1 < this.length) {
                val low = this[index + 1]
                if (low.isLowSurrogate()) {
                    return Char.toCodePoint(high, low)
                }
            }
            return high.toInt()
        }

        // Decomposition ===============================================================================================
        // Maximum length of decomposition.
        val MAX_DECOMPOSITION_LENGTH = 4
        // Maximum length of Hangul decomposition. Note that MAX_HANGUL_DECOMPOSITION_LENGTH <= MAX_DECOMPOSITION_LENGTH.
        val MAX_HANGUL_DECOMPOSITION_LENGTH = 3

        /*
         * Following constants are needed for Hangul canonical decomposition.
         * Hangul decomposition algorithm and constants are taken according
         * to description at http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        const val SBase = 0xAC00
        const val LBase = 0x1100
        const val VBase = 0x1161
        const val TBase = 0x11A7
        const val SCount = 11172
        const val LCount = 19
        const val VCount = 21
        const val TCount = 28
        const val NCount = 588

        // Access to the decomposition tables. =========================================================================
        /** Gets canonical class for given codepoint from decomposition mappings table. */
        fun getCanonicalClass(ch: Int): Int = getCanonicalClassInternal(ch)

        /** Tests Unicode codepoint if it is a boundary of decomposed Unicode codepoint. */
        fun isDecomposedCharBoundary(ch: Int): Boolean = getCanonicalClass(ch) == 0

        /** Tests if given codepoint is a canonical decomposition of another codepoint. */
        fun hasSingleCodepointDecomposition(ch: Int): Boolean = hasSingleCodepointDecompositionInternal(ch)

        /** Tests if given codepoint has canonical decomposition and given codepoint's canonical class is not 0. */
        fun hasDecompositionNonNullCanClass(ch: Int): Boolean =
            (ch == 0x0340) or (ch == 0x0341) or (ch == 0x0343) or (ch == 0x0344)

        /** Gets decomposition for given codepoint from decomposition mappings table. */
        fun getDecomposition(ch: Int): IntArray? = getDecompositionInternal(ch)

        // =============================================================================================================

        /**
         * Normalize given string.
         */
        fun normalize(input: String): String {
            val inputChars = input.toCharArray()
            val inputLength = inputChars.size
            var inputCodePointsIndex = 0
            var decompHangulIndex = 0

            //codePoints of input
            val inputCodePoints = IntArray(inputLength)

            //result of canonical decomposition of input
            var resCodePoints = IntArray(inputLength * MAX_DECOMPOSITION_LENGTH)

            //current symbol's codepoint
            var ch: Int

            //current symbol's decomposition
            var decomp: IntArray?

            //result of canonical and Hangul decomposition of input
            val decompHangul: IntArray

            //result of canonical decomposition of input in UTF-16 encoding
            val result = StringBuilder()

            var i = 0
            while (i < inputLength) {
                ch = input.codePointAt(i)
                inputCodePoints[inputCodePointsIndex++] = ch
                i += if (Char.isSupplementaryCodePoint(ch)) 2 else 1
            }

            // Canonical decomposition based on mappings in decomposition table.
            var resCodePointsIndex = decomposeString(inputCodePoints, inputCodePointsIndex, resCodePoints)

            // Canonical ordering.
            // See http://www.unicode.org/reports/tr15/#Decomposition for details
            resCodePoints = Lexer.getCanonicalOrder(resCodePoints, resCodePointsIndex)

            // Decomposition for Hangul syllables.
            // See http://www.unicode.org/reports/tr15/#Hangul for details
            decompHangul = IntArray(resCodePoints.size)
            @Suppress("NAME_SHADOWING")
            for (i in 0..resCodePointsIndex - 1) {
                val curSymb = resCodePoints[i]

                decomp = getHangulDecomposition(curSymb)
                if (decomp == null) {
                    decompHangul[decompHangulIndex++] = curSymb
                } else {
                    // Note that Hangul decompositions have length that is equal 2 or 3.
                    decompHangul[decompHangulIndex++] = decomp[0]
                    decompHangul[decompHangulIndex++] = decomp[1]
                    if (decomp.size == 3) {
                        decompHangul[decompHangulIndex++] = decomp[2]
                    }
                }
            }

            // Translating into UTF-16 encoding
            @Suppress("NAME_SHADOWING")
            for (i in 0..decompHangulIndex - 1) {
                result.append(Char.toChars(decompHangul[i]))
            }

            return result.toString()
        }

        /**
         * Rearrange codepoints in [inputInts] according to canonical order. Return an array with rearranged codepoints.
         */
        fun getCanonicalOrder(inputInts: IntArray, length: Int): IntArray {
            val inputLength = if (length < inputInts.size)
                length
            else
                inputInts.size

            /*
             * Simple bubble-sort algorithm. Note that many codepoints have 0 canonical class, so this algorithm works
             * almost lineary in overwhelming majority of cases. This is due to specific of Unicode combining
             * classes and codepoints.
             */
            for (i in 1..inputLength - 1) {
                var j = i - 1
                val iCanonicalClass = getCanonicalClass(inputInts[i])
                val ch: Int

                if (iCanonicalClass == 0) {
                    continue
                }

                while (j > -1) {
                    if (getCanonicalClass(inputInts[j]) > iCanonicalClass) {
                        j = j - 1
                    } else {
                        break
                    }
                }

                ch = inputInts[i]
                for (k in i downTo j + 1 + 1) {
                    inputInts[k] = inputInts[k - 1]
                }
                inputInts[j + 1] = ch
            }

            return inputInts
        }

        /**
         * Gets decomposition for given Hangul syllable.
         * This is an implementation of Hangul decomposition algorithm
         * according to http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf "3.12 Conjoining Jamo Behavior".
         */
        fun getHangulDecomposition(ch: Int): IntArray? {
            val SIndex = ch - SBase

            if (SIndex < 0 || SIndex >= SCount) {
                return null
            } else {
                val L = LBase + SIndex / NCount
                val V = VBase + SIndex % NCount / TCount
                var T = SIndex % TCount
                val decomp: IntArray

                if (T == 0) {
                    decomp = intArrayOf(L, V)
                } else {
                    T = TBase + T
                    decomp = intArrayOf(L, V, T)
                }
                return decomp
            }
        }
    }
}
