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


public enum class PatternOption(val value: String) {
    IGNORE_CASE : PatternOption("i")
    MULTILINE : PatternOption("m")
}


public data class MatchGroup(val value: String)

public trait MatchGroupCollection : Collection<MatchGroup?> {
    public fun get(index: Int): MatchGroup?
}

public trait MatchResult {
    public val range: IntRange
    public val value: String
    public val groups: MatchGroupCollection

    public fun next(): MatchResult?
}


public class Pattern(public val pattern: String, options_: Set<PatternOption>) {

    public constructor(pattern: String, vararg options: PatternOption) : this(pattern, options.toSet())

    public val options: Set<PatternOption> = options_.toSet()
    private val nativePattern: RegExp = RegExp(pattern, options.map { it.value }.joinToString() + "g")


    public fun matches(input: CharSequence): Boolean {
        nativePattern.reset()
        return nativePattern.test(input.toString())
    }

    public fun match(input: CharSequence): MatchResult? = nativePattern.findNext(input.toString(), 0)

    public fun matchAll(input: CharSequence): Sequence<MatchResult> = sequence({ match(input) }, { match -> match.next() })

    public fun replace(input: CharSequence, replacement: String): String = input.toString().nativeReplace(nativePattern, replacement)
    public fun replace(input: CharSequence, evaluator: (MatchResult) -> String): String = TODO

    public fun split(input: CharSequence, limit: Int = 0): List<String> = TODO

    public fun toString(): String = nativePattern.toString()

    companion object {
        public fun fromLiteral(literal: String): Pattern = Pattern(escape(literal))
        public fun escape(literal: String): String = TODO
        public fun escapeReplacement(literal: String): String = literal.nativeReplace(RegExp("\\$", "g"), "$$$$")
    }
}


private fun RegExp.findNext(input: String, from: Int): MatchResult? {
    this.lastIndex = from
    val match = exec(input)
    if (match == null) return null
    val reMatch = match as RegExpMatch
    val range = reMatch.index..lastIndex-1

    return object : MatchResult {
        override val range: IntRange = range
        override val value: String
            get() = match[0]!!

        override val groups: MatchGroupCollection = object : MatchGroupCollection {
            override fun size(): Int = match.size()
            override fun isEmpty(): Boolean = size() == 0

            override fun contains(o: Any?): Boolean = this.any { it == o }
            override fun containsAll(c: Collection<Any?>): Boolean = c.all({contains(it)})

            override fun iterator(): Iterator<MatchGroup?> = indices.sequence().map { this[it] }.iterator()

            override fun get(index: Int): MatchGroup? = match[index]?.let { MatchGroup(it) }
        }

        override fun next(): MatchResult? = this@findNext.findNext(input, range.end + 1)
    }
}