package kotlin.regexp

import java.util.regex.Matcher
import java.util.regex.MatchResult
import java.util.List
import java.util.regex.Pattern
import kotlin.support.AbstractIterator

/**
 * @author Sergey Mashkov aka cy6erGn0m
 * @since 28.07.12
 */

private class MatcherIterator (val m : Matcher) : AbstractIterator<Matcher>() {

    public override fun computeNext() {
        if (m.find()) {
            setNext(m)
        } else {
            done()
        }
    }
}

/**
 * Creates java.util.regex.Matcher for the specified char sequence and pre-compiled pattern
 */
public fun <S : CharSequence> S.createMatcher(p : Pattern) : Matcher = p.matcher(this)!!

/**
 * Provides iterator to iterate through matches. Iterator returns the same Matcher every time
 */
public fun Matcher.iterator() : jet.Iterator<Matcher> {
    return MatcherIterator(this).toJetIterator()
}

/**
 * Executes specified block on every match and supply MatchResult to this block
 */
public fun Matcher.forEachMatchResult(block : (MatchResult) -> Any?) : Unit {
    while (find()) {
        block(toMatchResult().sure())
    }
}

/**
 * Returns list of all remaining match results for this matcher.
 */
public fun Matcher.allMatchResults() : List<MatchResult> {
    val result = arrayList<MatchResult>()

    this.forEachMatchResult {result.add(it)}

    return result
}

/**
 * Executes specified block on every remaining match and supply matched text to it
 */
public fun Matcher.forEachMatch(block : (String) -> Any?) : Unit {
    while (find()) {
        block(group().sure())
    }
}

/**
 * Returns list of all remaining matched texts
 */
public fun Matcher.allMatches() : List<String> {
    val result = arrayList<String>()

    this.forEachMatch {result.add(it)}

    return result
}

/**
 * Executes specified block on each match and uses it return value as replacement to found match.
 * Function returns result of replacement as return value.
 */
public fun Matcher.replace(block : (MatchResult) -> String) : String {
    val sb = StringBuffer()
    forEachMatchResult {
        appendReplacement(sb, block(it))
    }
    appendTail(sb)

    return sb.toString().sure()
}

