/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.text

import java.util.ArrayList
import java.util.regex.Pattern as NativePattern
import java.util.regex.Matcher
import kotlin.properties.Delegates

private val TODO: Nothing get() = throw UnsupportedOperationException()

public trait FlagEnum {
    public val value: Int
    public val mask: Int
}
public fun Iterable<FlagEnum>.toInt(): Int =
        this.fold(0, { value, option -> value or option.value })
public fun <T: FlagEnum> fromInt(value: Int, allValues: Array<T>): Set<T> =
        allValues.filter({ value and it.mask == it.value }).toSet()


public enum class PatternOptions(override val value: Int, override val mask: Int = value) : FlagEnum {
    // common
    IGNORE_CASE : PatternOptions(NativePattern.CASE_INSENSITIVE)
    MULTILINE : PatternOptions(NativePattern.MULTILINE)

    //jvm-specific
    LITERAL : PatternOptions(NativePattern.LITERAL)
    UNICODE_CASE: PatternOptions(NativePattern.UNICODE_CASE)
    UNIX_LINES: PatternOptions(NativePattern.UNIX_LINES)
    COMMENTS: PatternOptions(NativePattern.COMMENTS)
    DOTALL: PatternOptions(NativePattern.DOTALL)
    CANON_EQ: PatternOptions(NativePattern.CANON_EQ)
}

/* in JS
public enum class PatternOptions(val value: String) {
    IGNORE_CASE : PatternOptions("i")
    MULTILINE : PatternOptions("m")
}
*/

public trait MatchGroup {
    public val range: IntRange
    public val value: String
}

public trait MatchResult : MatchGroup {
    public val groups: List<MatchGroup?>
    public fun next(): MatchResult?
}


public class Pattern ( /* visibility? */ val nativePattern: NativePattern) {

    public constructor(pattern: String, options: Set<PatternOptions>): this(NativePattern.compile(pattern, options.toInt()))
    public constructor(pattern: String, vararg options: PatternOptions) : this(pattern, options.toSet())

    public val pattern: String
        get() = nativePattern.pattern()

    public val options: Set<PatternOptions> = fromInt(nativePattern.flags(), PatternOptions.values())

    public fun matches(input: CharSequence): Boolean = nativePattern.matcher(input).matches()

    public fun match(input: CharSequence): MatchResult? = nativePattern.matcher(input).findNext(0)

    public fun matchAll(input: CharSequence): Sequence<MatchResult> = sequence({ match(input) }, { match -> match.next() })

    public fun replace(input: CharSequence, replacement: String): String = nativePattern.matcher(input).replaceAll(replacement)
    public fun replace(input: CharSequence, evaluator: (MatchResult) -> String): String = TODO

    public fun split(input: CharSequence, limit: Int = 0): List<String> = nativePattern.split(input, limit).asList() // TODO: require(limit>=0)

    companion object {
        public fun fromLiteral(literal: String): Pattern = Pattern(literal, PatternOptions.LITERAL)
        public fun escape(literal: String): String = NativePattern.quote(literal)
        public fun escapeReplacement(literal: String): String = Matcher.quoteReplacement(literal)
    }

}

public fun String.toPattern(vararg options: PatternOptions): Pattern = Pattern(this, *options)
public fun String.toPattern(options: Set<PatternOptions>): Pattern = Pattern(this, options)



private fun Matcher.findNext(from: Int): MatchResult? {
    if (!find(from))
        return null

    var matchResult = this as java.util.regex.MatchResult

    return object: MatchResult {
        override val range: IntRange
            get() = matchResult.start()..matchResult.end()-1
        override val value: String
            get() = matchResult.group()

        override val groups: List<MatchGroup?> by Delegates.lazy {
            // TODO:  wrap
            val groups = ArrayList<MatchGroup?>(matchResult.groupCount())
            for (groupIndex in 1..groupCount()) {
                val range = matchResult.start(groupIndex)..matchResult.end(groupIndex)-1
                if (range.start >= 0)
                    groups.add(object: MatchGroup {
                        override val range: IntRange = range
                        override val value: String
                            get() = matchResult.group(groupIndex)
                    })
                else
                    groups.add(null)
            }
            groups
        }

        override fun next(): MatchResult? {
            matchResult = this@findNext.toMatchResult()
            return this@findNext.findNext(matchResult.end()) // TODO: advance next
        }
    }
}
