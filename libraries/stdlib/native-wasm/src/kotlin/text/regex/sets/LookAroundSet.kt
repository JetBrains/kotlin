/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


package kotlin.text.regex

/**
 * Abstract class for lookahead and lookbehind nodes.
 */
internal abstract class LookAroundSet(children: List<AbstractSet>, fSet: FSet) : AtomicJointSet(children, fSet) {
    protected abstract fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int

    /** Returns startIndex+shift, the next position to match */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.saveState()
        return tryToMatch(startIndex, testString, matchResult).also { if (it < 0) matchResult.rollbackState() }
    }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean = true
}