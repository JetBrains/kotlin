package kotlin.internal

import java.util.regex.MatchResult
import java.util.regex.Matcher

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
internal open class JRE8PlatformImplementations : JRE7PlatformImplementations() {

    override fun getMatchResultNamedGroup(matchResult: MatchResult, name: String): MatchGroup? {
        val matcher = matchResult as? Matcher ?: throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

        val range = matcher.start(name)..matcher.end(name)-1
        return if (range.start >= 0)
            MatchGroup(matcher.group(name), range)
        else
            null
    }

}
