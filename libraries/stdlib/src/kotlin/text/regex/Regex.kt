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

/** Represents a collection of captured groups in a single match. */
public trait MatchGroupCollection : Collection<MatchGroup?> {

    /** Returns a group with the specified [index]
     *
     * @return An instance of [MatchGroup] if the group with the specified [index] was matched or `null` otherwise.
     *
     * The groups are indexed from 1 to the count of groups in regular expression. The group with zero index
     * represents the entire match.
     */
    public fun get(index: Int): MatchGroup?

    // TODO: Provide get(name: String) on JVM 7+
}

/**
 * Represents the results from a single regular expression match.
 */
public trait MatchResult {
    /** The range of indices in the original string where match was captured. */
    public val range: IntRange
    /** The substring from the input string captured by this match. */
    public val value: String
    /** A collection of groups matched by the regular expression. */
    public val groups: MatchGroupCollection
    // TODO: Should we have groupCount (equals groups.size()-1)?

    /** Returns a new [MatchResult] with the results for the next match, starting at the position
     *  at which the last match ended (at the character after the last matched character).
     */
    public fun next(): MatchResult?
}

