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
package kotlin.text.regex

import kotlin.experimental.ExperimentalNativeApi

/** Represents a compiled pattern used by [Regex] for matching, searching, or replacing strings. */
internal class Pattern(val pattern: String, flags: Int = 0) {

    var flags = flags
        private set

    /** A lexer instance used to get tokens from the pattern. */
    private val lexemes = Lexer(pattern, flags)

    /** List of all capturing groups in the pattern. Primarily used for handling back references. */
    val capturingGroups = mutableListOf<FSet>()

    /** Mapping from group name to its index */
    val groupNameToIndex = hashMapOf<String, Int>()

    /** Is true if back referenced sets replacement by second compilation pass is needed.*/
    private var needsBackRefReplacement = false

    /** A number of group quantifiers in the pattern */
    var groupQuantifierCount = 0
        private set

    /**
     * A number of consumers found in the pattern.
     * Consumer is any expression ending with an FSet except capturing groups (they are counted by [capturingGroups])
     */
    var consumersCount = 0
        private set

    /** A node to start a matching/searching process by call startNode.matches/startNode.find. */
    internal val startNode: AbstractSet

    /** Compiles the given pattern */
    init {
        if (flags != 0 && flags or flagsBitMask != flagsBitMask) {
            throw IllegalArgumentException("Invalid match flags value")
        }
        startNode = processExpression(-1, this.flags, null)

        if (!lexemes.isEmpty()) {
            throw PatternSyntaxException("Trailing characters", pattern, lexemes.curTokenIndex)
        }

        // Finalize compilation
        if (needsBackRefReplacement) {
            startNode.processSecondPass()
        }
    }

    override fun toString(): String = pattern

    /** Return true if the pattern has the specified flag */
    private fun hasFlag(flag: Int): Boolean = flags and flag == flag

    // Compilation methods. ============================================================================================
    /** A->(a|)+ */
    private fun processAlternations(last: AbstractSet): AbstractSet {
        val auxRange = CharClass(hasFlag(Pattern.CASE_INSENSITIVE))
        while (!lexemes.isEmpty() && lexemes.isLetter()
                && (lexemes.lookAhead == 0
                    || lexemes.lookAhead == Lexer.CHAR_VERTICAL_BAR
                    || lexemes.lookAhead == Lexer.CHAR_RIGHT_PARENTHESIS)) {
            auxRange.add(lexemes.next())
            if (lexemes.currentChar == Lexer.CHAR_VERTICAL_BAR) {
                lexemes.next()
            }
        }
        val rangeSet = processRangeSet(auxRange)
        rangeSet.next = last

        return rangeSet
    }

    /** E->AE; E->S|E; E->S; A->(a|)+ E->S(|S)* */
    private fun processExpression(ch: Int, newFlags: Int, last: AbstractSet?): AbstractSet {
        val children = ArrayList<AbstractSet>()
        val savedFlags = flags
        var saveChangedFlags = false

        if (newFlags != flags) {
            flags = newFlags
        }

        // Create a right finalizing set.
        val fSet: FSet
        when (ch) {
            // Special groups: non-capturing, look ahead/behind etc.
            Lexer.CHAR_NONCAP_GROUP -> fSet = NonCapFSet(consumersCount++)
            Lexer.CHAR_POS_LOOKAHEAD,
            Lexer.CHAR_NEG_LOOKAHEAD -> fSet = AheadFSet()
            Lexer.CHAR_POS_LOOKBEHIND,
            Lexer.CHAR_NEG_LOOKBEHIND -> fSet = BehindFSet(consumersCount++)
            Lexer.CHAR_ATOMIC_GROUP -> fSet = AtomicFSet(consumersCount++)
            // A Capturing group.
            else -> {
                if (last == null) {
                    // Whole pattern - group #0.
                    fSet = FinalSet()
                    saveChangedFlags = true
                } else {
                    fSet = FSet(capturingGroups.size)
                }

                capturingGroups.add(fSet)

                if (ch == Lexer.CHAR_NAMED_GROUP) {
                    val name = (lexemes.curSpecialToken as NamedGroup).name
                    if (groupNameToIndex.containsKey(name)) {
                        throw PatternSyntaxException("Named capturing group <$name> is already defined", pattern, lexemes.curTokenIndex)
                    }
                    groupNameToIndex[name] = fSet.groupIndex
                }
            }
        }

        if (last != null) {
            lexemes.next()
        }

        //Process to EOF or ')'
        do {
            val child: AbstractSet
            when {
                // a|...
                lexemes.isLetter() && lexemes.lookAhead == Lexer.CHAR_VERTICAL_BAR -> child = processAlternations(fSet)
                // ..|.., e.g. in "a||||b"
                lexemes.currentChar == Lexer.CHAR_VERTICAL_BAR -> {
                    child = EmptySet(fSet)
                    lexemes.next()
                }
                else -> {
                    child = processSubExpression(fSet)
                    if (lexemes.currentChar == Lexer.CHAR_VERTICAL_BAR) {
                        lexemes.next()
                    }
                }
            }
            children.add(child)
        } while (!(lexemes.isEmpty() || lexemes.currentChar == Lexer.CHAR_RIGHT_PARENTHESIS))

        // |) or |<EOF> - add an empty node.
        if (lexemes.lookBack == Lexer.CHAR_VERTICAL_BAR) {
            children.add(EmptySet(fSet))
        }

        // Restore flags.
        if (flags != savedFlags && !saveChangedFlags) {
            flags = savedFlags
            lexemes.restoreFlags(flags)
        }

        when (ch) {
            Lexer.CHAR_NONCAP_GROUP -> return NonCapturingJointSet(children, fSet)
            Lexer.CHAR_POS_LOOKAHEAD -> return PositiveLookAheadSet(children, fSet)
            Lexer.CHAR_NEG_LOOKAHEAD -> return NegativeLookAheadSet(children, fSet)
            Lexer.CHAR_POS_LOOKBEHIND -> return PositiveLookBehindSet(children, fSet)
            Lexer.CHAR_NEG_LOOKBEHIND -> return NegativeLookBehindSet(children, fSet)
            Lexer.CHAR_ATOMIC_GROUP -> return AtomicJointSet(children, fSet)

            else -> when (children.size) {
                0 -> return EmptySet(fSet)
                1 -> return SingleSet(children[0], fSet)
                else -> return JointSet(children, fSet)
            }
        }
    }


    /**
     * T->aaa
     */
    @OptIn(ExperimentalNativeApi::class)
    private fun processSequence(): AbstractSet {
        val substring = StringBuilder()
        while (!lexemes.isEmpty()
                && lexemes.isLetter()
                && !lexemes.isSurrogate()
                && (!lexemes.isNextSpecial && lexemes.lookAhead == 0 // End of a pattern.
                    || !lexemes.isNextSpecial && Lexer.isLetter(lexemes.lookAhead)
                    || lexemes.lookAhead == Lexer.CHAR_RIGHT_PARENTHESIS
                    || lexemes.lookAhead and 0x8000ffff.toInt() == Lexer.CHAR_LEFT_PARENTHESIS
                    || lexemes.lookAhead == Lexer.CHAR_VERTICAL_BAR
                    || lexemes.lookAhead == Lexer.CHAR_DOLLAR)) {
            val ch = lexemes.next()

            if (Char.isSupplementaryCodePoint(ch)) {
                substring.append(Char.toChars(ch))
            } else {
                substring.append(ch.toChar())
            }
        }
        return SequenceSet(substring, hasFlag(CASE_INSENSITIVE))
    }

    /**
     * D->a
     */
    private fun processDecomposedChar(): AbstractSet {
        val codePoints = IntArray(Lexer.MAX_DECOMPOSITION_LENGTH)
        val codePointsHangul: CharArray
        var readCodePoints = 0
        var curSymb = -1
        var curSymbIndex = -1

        if (!lexemes.isEmpty() && lexemes.isLetter()) {
            curSymb = lexemes.next()
            codePoints[readCodePoints] = curSymb
            curSymbIndex = curSymb - Lexer.LBase
        }

        /*
         * We process decomposed Hangul syllable LV or LVT or process jamo L.
         * See http://www.unicode.org/versions/Unicode4.0.0/ch03.pdf
         * "3.12 Conjoining Jamo Behavior"
         */
        if (curSymbIndex >= 0 && curSymbIndex < Lexer.LCount) {
            codePointsHangul = CharArray(Lexer.MAX_HANGUL_DECOMPOSITION_LENGTH)
            codePointsHangul[readCodePoints++] = curSymb.toChar()

            curSymb = lexemes.currentChar
            curSymbIndex = curSymb - Lexer.VBase
            if (curSymbIndex >= 0 && curSymbIndex < Lexer.VCount) {
                codePointsHangul[readCodePoints++] = curSymb.toChar()
                lexemes.next()
                curSymb = lexemes.currentChar
                curSymbIndex = curSymb - Lexer.TBase
                if (curSymbIndex >= 0 && curSymbIndex < Lexer.TCount) {
                    codePointsHangul[@Suppress("UNUSED_CHANGED_VALUE")readCodePoints++] = curSymb.toChar()
                    lexemes.next()

                    //LVT syllable
                    return HangulDecomposedCharSet(codePointsHangul, 3)
                } else {

                    //LV syllable
                    return HangulDecomposedCharSet(codePointsHangul, 2)
                }
            } else {

                //L jamo
                return CharSet(codePointsHangul[0], hasFlag(CASE_INSENSITIVE))
            }

        /*
         * We process single codepoint or decomposed codepoint.
         * We collect decomposed codepoint and obtain
         * one DecomposedCharSet.
         */
        } else {
            readCodePoints++

            while (readCodePoints < Lexer.MAX_DECOMPOSITION_LENGTH
                    && !lexemes.isEmpty() && lexemes.isLetter()
                    && !Lexer.isDecomposedCharBoundary(lexemes.currentChar)) {
                codePoints[readCodePoints++] = lexemes.next()
            }

            /*
             * We have read an ordinary symbol.
             */
            if (readCodePoints == 1 && !Lexer.hasSingleCodepointDecomposition(codePoints[0])) {
                return processCharSet(codePoints[0])
            } else {
                return DecomposedCharSet(codePoints, readCodePoints)
            }
        }
    }

    /**
     * S->BS; S->QS; S->Q; B->a+
     */
    private fun processSubExpression(last: AbstractSet): AbstractSet {
        var cur: AbstractSet
        when {
            lexemes.isLetter() && !lexemes.isNextSpecial && Lexer.isLetter(lexemes.lookAhead) -> {
                when {
                    hasFlag(Pattern.CANON_EQ) -> {
                        cur = processDecomposedChar()
                        if (!lexemes.isEmpty()
                            && (lexemes.currentChar != Lexer.CHAR_RIGHT_PARENTHESIS || last is FinalSet)
                            && lexemes.currentChar != Lexer.CHAR_VERTICAL_BAR
                            && !lexemes.isLetter()) {

                            cur = processQuantifier(last, cur)
                        }
                    }
                    lexemes.isHighSurrogate() || lexemes.isLowSurrogate() -> {
                        val term = processTerminal(last)
                        cur = processQuantifier(last, term)
                    }
                    else -> {
                        cur = processSequence()
                    }
                }
            }
            lexemes.currentChar == Lexer.CHAR_RIGHT_PARENTHESIS -> {
                if (last is FinalSet) {
                    throw PatternSyntaxException("unmatched )", pattern, lexemes.curTokenIndex)
                }
                cur = EmptySet(last)
            }
            else -> {
                val term = processTerminal(last)
                cur = processQuantifier(last, term)
            }
        }

        if (!lexemes.isEmpty()
            && (lexemes.currentChar != Lexer.CHAR_RIGHT_PARENTHESIS || last is FinalSet)
            && lexemes.currentChar != Lexer.CHAR_VERTICAL_BAR) {

            val next = processSubExpression(last)
            if (cur is LeafQuantifierSet
                // '*' or '{0,}' quantifier
                && cur.max == Quantifier.INF
                && cur.min == 0
                && !next.first(cur.innerSet)) {
                // An Optimizer node for the case where there is no intersection with the next node
                cur = UnifiedQuantifierSet(cur)
            }
            cur.next = next
        } else  {
            cur.next = last
        }
        return cur
    }

    private fun quantifierFromLexerToken(quant: Int): Quantifier {
        return when (quant) {
            Lexer.QUANT_COMP, Lexer.QUANT_COMP_R, Lexer.QUANT_COMP_P -> {
                lexemes.nextSpecial() as Quantifier
            }
            else -> {
                lexemes.next()
                Quantifier.fromLexerToken(quant)
            }
        }
    }

    /**
     * Q->T(*|+|?...) also do some optimizations.
     */
    private fun processQuantifier(last: AbstractSet, term: AbstractSet): AbstractSet {
        val quant = lexemes.currentChar

        if (term.type == AbstractSet.TYPE_DOTSET && (quant == Lexer.QUANT_STAR || quant == Lexer.QUANT_PLUS)) {
            lexemes.next()
            return DotQuantifierSet(term, last, quant, AbstractLineTerminator.getInstance(flags), hasFlag(Pattern.DOTALL))
        }

        return when (quant) {

            Lexer.QUANT_STAR, Lexer.QUANT_PLUS, Lexer.QUANT_ALT, Lexer.QUANT_COMP -> {
                val quantifier = quantifierFromLexerToken(quant)
                when {
                    term is LeafSet ->
                        LeafQuantifierSet(quantifier, term, last, quant)
                    term.consumesFixedLength ->
                        FixedLengthQuantifierSet(quantifier, term, last, quant)
                    else ->
                        GroupQuantifierSet(quantifier, term, last, quant, groupQuantifierCount++)
                }
            }

            Lexer.QUANT_STAR_R, Lexer.QUANT_PLUS_R, Lexer.QUANT_ALT_R, Lexer.QUANT_COMP_R -> {
                val quantifier = quantifierFromLexerToken(quant)
                when {
                    term is LeafSet ->
                        ReluctantLeafQuantifierSet(quantifier, term, last, quant)
                    term.consumesFixedLength ->
                        ReluctantFixedLengthQuantifierSet(quantifier, term, last, quant)
                    else ->
                        ReluctantGroupQuantifierSet(quantifier, term, last, quant, groupQuantifierCount++)
                }
            }

            Lexer.QUANT_PLUS_P, Lexer.QUANT_STAR_P, Lexer.QUANT_ALT_P, Lexer.QUANT_COMP_P -> {
                val quantifier = quantifierFromLexerToken(quant)
                when {
                    term is LeafSet ->
                        PossessiveLeafQuantifierSet(quantifier, term, last, quant)
                    term.consumesFixedLength ->
                        PossessiveFixedLengthQuantifierSet(quantifier, term, last, quant)
                    else ->
                        PossessiveGroupQuantifierSet(quantifier, term, last, quant, groupQuantifierCount++)
                }
            }

            else -> term
        }
    }

    /**
     * T-> letter|[range]|{char-class}|(E)
     */
    private fun processTerminal(last: AbstractSet): AbstractSet {
        val term: AbstractSet
        var char = lexemes.currentChar
        // Process flags: (?...)(?...)...
        while (char and 0xff00ffff.toInt() == Lexer.CHAR_FLAGS) {
            lexemes.next()
            flags = (char shr 16) and flagsBitMask
            char = lexemes.currentChar
        }
        // The terminal is some kind of group: (E). Call processExpression for it.
        if (char and 0x8000ffff.toInt() == Lexer.CHAR_LEFT_PARENTHESIS) {
            var newFlags = flags
            if (char and 0xff00ffff.toInt() == Lexer.CHAR_NONCAP_GROUP) {
                newFlags = (char shr 16) and flagsBitMask
            }
            term = processExpression(char and 0xff00ffff.toInt(), newFlags, last) // Remove flags from the token.
            if (lexemes.currentChar != Lexer.CHAR_RIGHT_PARENTHESIS) {
                throw PatternSyntaxException("unmatched (", pattern, lexemes.curTokenIndex)
            }
            lexemes.next()
        } else {
            // Other terminals.
            when (char) {
                Lexer.CHAR_LEFT_SQUARE_BRACKET -> { // Range: [...]
                    lexemes.next()
                    var negative = false
                    if (lexemes.currentChar == Lexer.CHAR_CARET) {
                        negative = true
                        lexemes.next()
                    }

                    term = processRange(negative, last)
                    if (lexemes.currentChar != Lexer.CHAR_RIGHT_SQUARE_BRACKET) {
                        throw PatternSyntaxException("unmatched [", pattern, lexemes.curTokenIndex)
                    }
                    lexemes.setModeWithReread(Lexer.Mode.PATTERN)
                    lexemes.next()
                }

                Lexer.CHAR_DOT -> {  // Dot: .
                    lexemes.next()
                    term = DotSet(AbstractLineTerminator.getInstance(flags), hasFlag(DOTALL))
                }

                Lexer.CHAR_CARET -> { // Beginning of the string: ^
                    lexemes.next()
                    term = SOLSet(AbstractLineTerminator.getInstance(flags), hasFlag(MULTILINE))
                    consumersCount++
                }

                Lexer.CHAR_DOLLAR -> { // End of the string: $
                    lexemes.next()
                    term = EOLSet(consumersCount++, AbstractLineTerminator.getInstance(flags), hasFlag(MULTILINE))

                }

                // Word / non-word boundary.
                Lexer.CHAR_WORD_BOUND -> {
                    lexemes.next()
                    term = WordBoundarySet(true)
                }

                Lexer.CHAR_NONWORD_BOUND -> {
                    lexemes.next()
                    term = WordBoundarySet(false)
                }

                Lexer.CHAR_END_OF_INPUT -> { // End of an input: \z
                    lexemes.next()
                    term = EOISet()
                }

                Lexer.CHAR_END_OF_LINE -> { // End of a line: \Z
                    lexemes.next()
                    term = EOLSet(consumersCount++, AbstractLineTerminator.getInstance(flags))
                }

                Lexer.CHAR_START_OF_INPUT -> {  // Start of an input: \A
                    lexemes.next()
                    term = SOLSet(AbstractLineTerminator.getInstance(flags))
                }

                Lexer.CHAR_LINEBREAK -> {
                    // Any unicode linebreak sequence:
                    // \u000D\u000A|[\u000A\u000B\u000C\u000D\u0085\u2028\u2029]
                    lexemes.next()
                    val fSet = NonCapFSet(consumersCount++)
                    val lineBreakSequence = SequenceSet("\u000D\u000A").apply {
                        next = fSet
                    }
                    val lineBreakChars = RangeSet(
                        CharClass().addAll(listOf('\u000A', '\u000B', '\u000C', '\u000D', '\u0085', '\u2028', '\u2029'))
                    ).apply {
                        next = fSet
                    }
                    term = NonCapturingJointSet(listOf(lineBreakSequence, lineBreakChars), fSet)
                }

                Lexer.CHAR_PREVIOUS_MATCH -> {  // A previous match: \G
                    lexemes.next()
                    term = PreviousMatchSet()
                }

                // Back references: \1, \2 etc.
                0x80000000.toInt() or '1'.toInt(),
                0x80000000.toInt() or '2'.toInt(),
                0x80000000.toInt() or '3'.toInt(),
                0x80000000.toInt() or '4'.toInt(),
                0x80000000.toInt() or '5'.toInt(),
                0x80000000.toInt() or '6'.toInt(),
                0x80000000.toInt() or '7'.toInt(),
                0x80000000.toInt() or '8'.toInt(),
                0x80000000.toInt() or '9'.toInt() -> {
                    var groupIndex = (char and 0x7FFFFFFF) - '0'.code
                    while (lexemes.lookAhead in '0'.code..'9'.code) {
                        val newGroupIndex = (groupIndex * 10) + (lexemes.lookAhead - '0'.code)
                        if (newGroupIndex in 0 until capturingGroups.size) {
                            groupIndex = newGroupIndex
                            lexemes.next()
                        } else {
                            break
                        }
                    }
                    term = createBackReference(groupIndex)
                    lexemes.next()
                }

                Lexer.CHAR_NAMED_GROUP_REF -> {
                    val name = (lexemes.curSpecialToken as NamedGroup).name
                    val groupIndex = groupNameToIndex[name] ?: -1
                    term = createBackReference(groupIndex)
                    lexemes.next()
                }

                // A special token (\D, \w etc), 'u0000' or the end of the pattern.
                0 -> {
                    val cc: AbstractCharClass? = lexemes.curSpecialToken as AbstractCharClass?
                    when {
                        cc != null -> {
                            term = processRangeSet(cc)
                            lexemes.next()
                        }
                        !lexemes.isEmpty() -> {
                            term = CharSet(char.toChar())
                            lexemes.next()
                        }
                        else -> term = EmptySet(last)
                    }
                }

                else -> {
                    when {
                        // A regular character.
                        char >= 0 && !lexemes.isSpecial -> {
                            term = processCharSet(char)
                            lexemes.next()
                        }
                        char == Lexer.CHAR_VERTICAL_BAR -> {
                            term = EmptySet(last)
                        }
                        char == Lexer.CHAR_RIGHT_PARENTHESIS -> {
                            if (last is FinalSet) {
                                throw PatternSyntaxException("unmatched )", pattern, lexemes.curTokenIndex)
                            }
                            term = EmptySet(last)
                        }
                        else -> {
                            val current = if (lexemes.isSpecial) lexemes.curSpecialToken.toString() else char.toString()
                            throw PatternSyntaxException("Dangling meta construction: $current", pattern, lexemes.curTokenIndex)
                        }
                    }
                }
            }
        }
        return term
    }

    /** Creates a back reference to the group with specified [groupIndex], or throws if the group doesn't exist yet. */
    private fun createBackReference(groupIndex: Int): BackReferenceSet {
        if (groupIndex >= 0 && groupIndex < capturingGroups.size) {
            capturingGroups[groupIndex].isBackReferenced = true
            needsBackRefReplacement = true // And process back references in the second pass.
            return BackReferenceSet(groupIndex, consumersCount++, hasFlag(CASE_INSENSITIVE))
        } else {
            throw PatternSyntaxException("No such group yet exists at this point in the pattern", pattern, lexemes.curTokenIndex)
        }
    }

    /**
     * Process [...] ranges
     */
    private fun processRange(negative: Boolean, last: AbstractSet): AbstractSet {
        val res = processRangeExpression(negative)
        val rangeSet = processRangeSet(res)
        rangeSet.next = last

        return rangeSet
    }

    private fun processRangeExpression(alt: Boolean): CharClass {
        var result = CharClass(hasFlag(Pattern.CASE_INSENSITIVE), alt)
        var buffer = -1
        var intersection = false
        var firstInClass = true

        var notClosed = lexemes.currentChar != Lexer.CHAR_RIGHT_SQUARE_BRACKET
        while (!lexemes.isEmpty() && (notClosed || firstInClass)) {
            when (lexemes.currentChar) {

                Lexer.CHAR_RIGHT_SQUARE_BRACKET -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = ']'.toInt()
                    lexemes.next()
                }

                Lexer.CHAR_LEFT_SQUARE_BRACKET -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                        buffer = -1
                    }
                    lexemes.next()
                    var negative = false
                    if (lexemes.currentChar == Lexer.CHAR_CARET) {
                        lexemes.next()
                        negative = true
                    }

                    if (intersection)
                        result.intersection(processRangeExpression(negative))
                    else
                        result.union(processRangeExpression(negative))
                    intersection = false
                    lexemes.next()
                }

                Lexer.CHAR_AMPERSAND -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = lexemes.next()  // buffer == Lexer.CHAR_AMPERSAND since next() returns currentChar.

                    /*
                     * If there is a start for subrange we will do an intersection
                     * otherwise treat '&' as a normal character
                     */
                    if (lexemes.currentChar == Lexer.CHAR_AMPERSAND) {
                        if (lexemes.lookAhead == Lexer.CHAR_LEFT_SQUARE_BRACKET) {
                            lexemes.next()
                            intersection = true
                            buffer = -1
                        } else {
                            lexemes.next()
                            if (firstInClass) {
                                // Skip "&&" at "[&&...]" or "[^&&...]"
                                result = processRangeExpression(false)
                            } else {
                                // Ignore "&&" at "[X&&]" ending where X != empty string
                                if (lexemes.currentChar != Lexer.CHAR_RIGHT_SQUARE_BRACKET) {
                                    result.intersection(processRangeExpression(false))
                                }
                            }
                        }
                    } else {
                        //treat '&' as a normal character
                        buffer = '&'.toInt()
                    }
                }

                Lexer.CHAR_HYPHEN -> {
                    if (firstInClass
                        || lexemes.lookAhead == Lexer.CHAR_RIGHT_SQUARE_BRACKET
                        || lexemes.lookAhead == Lexer.CHAR_LEFT_SQUARE_BRACKET
                        || buffer < 0) {
                        // Treat the hypen as a normal character.
                        if (buffer >= 0) {
                            result.add(buffer)
                        }
                        buffer = '-'.toInt()
                        lexemes.next()
                    } else {
                        // A range.
                        lexemes.next()
                        var cur = lexemes.currentChar

                        if (!lexemes.isSpecial
                            && (cur >= 0
                                || lexemes.lookAhead == Lexer.CHAR_RIGHT_SQUARE_BRACKET
                                || lexemes.lookAhead == Lexer.CHAR_LEFT_SQUARE_BRACKET
                                || buffer < 0)) {

                            try {
                                if (!Lexer.isLetter(cur)) {
                                    cur = cur and 0xFFFF
                                }
                                result.add(buffer, cur)
                            } catch (e: Exception) {
                                throw PatternSyntaxException("Illegal character range", pattern, lexemes.curTokenIndex)
                            }

                            lexemes.next()
                            buffer = -1
                        } else {
                            throw PatternSyntaxException("Illegal character range", pattern, lexemes.curTokenIndex)
                        }
                    }
                }

                Lexer.CHAR_CARET -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = '^'.toInt()
                    lexemes.next()
                }

                0 -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    val cs = lexemes.curSpecialToken as AbstractCharClass?
                    if (cs != null) {
                        result.add(cs)
                        buffer = -1
                    } else {
                        buffer = 0
                    }

                    lexemes.next()
                }

                else -> {
                    if (buffer >= 0) {
                        result.add(buffer)
                    }
                    buffer = lexemes.next()
                }
            }

            firstInClass = false
            notClosed = lexemes.currentChar != Lexer.CHAR_RIGHT_SQUARE_BRACKET
        }
        if (notClosed) {
            throw PatternSyntaxException("Missing ']'", pattern, lexemes.curTokenIndex)
        }
        if (buffer >= 0) {
            result.add(buffer)
        }
        return result
    }

    private fun processRangeSet(charClass: AbstractCharClass): AbstractSet {
        if (charClass.hasLowHighSurrogates()) {
            val lowHighSurrRangeSet = SurrogateRangeSet(charClass.classWithSurrogates())

            if (charClass.mayContainSupplCodepoints) {
                return CompositeRangeSet(SupplementaryRangeSet(charClass.classWithoutSurrogates(), hasFlag(CASE_INSENSITIVE)), lowHighSurrRangeSet)
            }

            return CompositeRangeSet(RangeSet(charClass.classWithoutSurrogates(), hasFlag(CASE_INSENSITIVE)), lowHighSurrRangeSet)
        }

        if (charClass.mayContainSupplCodepoints) {
            return SupplementaryRangeSet(charClass, hasFlag(CASE_INSENSITIVE))
        }

        return RangeSet(charClass, hasFlag(CASE_INSENSITIVE))
    }

    @OptIn(ExperimentalNativeApi::class)
    private fun processCharSet(ch: Int): AbstractSet {
        val isSupplCodePoint = Char.isSupplementaryCodePoint(ch)

        return when {
            isSupplCodePoint -> SequenceSet(Char.toChars(ch).concatToString(0, 2), hasFlag(CASE_INSENSITIVE))
            ch.toChar().isLowSurrogate() ->  LowSurrogateCharSet(ch.toChar())
            ch.toChar().isHighSurrogate() -> HighSurrogateCharSet(ch.toChar())
            else -> CharSet(ch.toChar(), hasFlag(CASE_INSENSITIVE))
        }
    }

    companion object {
        //TODO: Use RegexOption enum here.
        // Flags.
        /**
         * This constant specifies that a pattern matches Unix line endings ('\n')
         * only against the '.', '^', and '$' meta characters.
         */
        val UNIX_LINES = 1 shl 0

        /**
         * This constant specifies that a `Pattern` is matched
         * case-insensitively. That is, the patterns "a+" and "A+" would both match
         * the string "aAaAaA".
         */
        val CASE_INSENSITIVE = 1 shl 1

        /**
         * This constant specifies that a `Pattern` may contain whitespace or
         * comments. Otherwise comments and whitespace are taken as literal
         * characters.
         */
        val COMMENTS = 1 shl 2

        /**
         * This constant specifies that the meta characters '^' and '$' match only
         * the beginning and end end of an input line, respectively. Normally, they
         * match the beginning and the end of the complete input.
         */
        val MULTILINE = 1 shl 3

        /**
         * This constant specifies that the whole `Pattern` is to be taken
         * literally, that is, all meta characters lose their meanings.
         */
        val LITERAL = 1 shl 4

        /**
         * This constant specifies that the '.' meta character matches arbitrary
         * characters, including line endings, which is normally not the case.
         */
        val DOTALL = 1 shl 5

        /**
         * This constant specifies that a character in a `Pattern` and a
         * character in the input string only match if they are canonically
         * equivalent.
         */
        val CANON_EQ = 1 shl 6

        /** A bit mask that includes all defined match flags */
        internal val flagsBitMask = Pattern.UNIX_LINES or
                Pattern.CASE_INSENSITIVE or
                Pattern.COMMENTS or
                Pattern.MULTILINE or
                Pattern.LITERAL or
                Pattern.DOTALL or
                Pattern.CANON_EQ


        /**
         * Quotes a given string using "\Q" and "\E", so that all other meta-characters lose their special meaning.
         * If the string is used for a `Pattern` afterwards, it can only be matched literally.
         */
        fun quote(s: String): String {
            return StringBuilder()
                    .append("\\Q")
                    .append(s.replace("\\E", "\\E\\\\E\\Q"))
                    .append("\\E").toString()
        }
    }
}

