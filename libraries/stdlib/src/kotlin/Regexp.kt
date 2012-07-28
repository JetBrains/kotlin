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

public fun <S : CharSequence> S.createMatcher(p : Pattern) : Matcher = p.matcher(this)!!

public fun Matcher.iterator() : jet.Iterator<Matcher> {
    return MatcherIterator(this).toJetIterator()
}

public fun Matcher.forEachMatchResult(block : (MatchResult) -> Any?) : Unit {
    while (find()) {
        block(toMatchResult().sure())
    }
}

public fun Matcher.allMatchResults() : List<MatchResult> {
    val result = arrayList<MatchResult>()

    this.forEachMatchResult {result.add(it)}

    return result
}

public fun Matcher.forEachMatch(block : (String) -> Any?) : Unit {
    while (find()) {
        block(group().sure())
    }
}

public fun Matcher.allMatches() : List<String> {
    val result = arrayList<String>()

    this.forEachMatch {result.add(it)}

    return result
}

public fun Matcher.replace(block : (MatchResult) -> String) : String {
    val sb = StringBuffer()
    forEachMatchResult {
        appendReplacement(sb, block(it))
    }
    appendTail(sb)

    return sb.toString().sure()
}

