/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.analysis

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.name.FqName

internal const val STABILITY_WILDCARD_SINGLE = '*'
internal const val STABILITY_WILDCARD_MULTI = "**"
internal const val STABILITY_GENERIC_OPEN = '<'
internal const val STABILITY_GENERIC_CLOSE = '>'
internal const val STABILITY_GENERIC_INCLUDE = "*"
internal const val STABILITY_GENERIC_EXCLUDE = "_"
internal const val STABILITY_GENERIC_SEPARATOR = ","
internal const val STABILITY_PACKAGE_SEPARATOR = '.'
class FqNameMatcherCollection(private val matchers: Set<FqNameMatcher>) {
    // Cache of external types already matched
    private val externalTypesMatched: MutableMap<FqName, Boolean> = mutableMapOf()
    // Tree of matchers for fast lookup
    private val matcherTree = MutableMatcherTree()

    init {
        matcherTree.putAll(matchers)
    }

    fun maskForName(name: FqName?): Int? {
        if (name == null) return null
        return matcherTree.findFirstPositiveMatcher(name)?.mask
    }

    fun matches(name: FqName?, superTypes: List<IrType>): Boolean {
        if (matchers.isEmpty()) return false
        if (name == null) return false

        externalTypesMatched[name]?.let {
            return it
        }

        val superTypeNames = superTypes.mapNotNull { it.classFqName }
        return (matcherTree.findFirstPositiveMatcher(name) != null ||
            superTypeNames.any { superName ->
                matcherTree.findFirstPositiveMatcher(superName) != null
            })
            .also {
                externalTypesMatched[name] = it
            }
    }
}

/**
 * A tree of matchers for faster lookup.
 * The tree is structured around package segments. e.g. com.google.foo = com -> google -> foo
 * Because multiple matchers could match the same fqName due to wildcards, matchers are
 * stored in the tree at the point of their first wildcard. In the case of no wildcards,
 * matchers are stored at the end of the branch.
 */
private class MutableMatcherTree {
    private val root = Node()

    fun putAll(matchers: Iterable<FqNameMatcher>) {
        matchers.forEach { put(it) }
    }

    fun put(matcher: FqNameMatcher) {
        var node: Node = root

        for (c in matcher.key) {
            node = node.children.getOrPut(c) { Node() }
        }

        node.values.add(matcher)
    }

    fun findFirstPositiveMatcher(fqName: FqName): FqNameMatcher? {
        val segments = fqName.asString()
        var currSegmentIndex = 0
        var currNode: Node? = root

        while (currNode != null) {
            val segment = segments.getOrNull(currSegmentIndex)
            for (i in 0 until currNode.values.size) {
                val matcher = currNode.values[i]
                if (matcher.matches(fqName)) return matcher
            }
            if (segment != null) {
                currNode = currNode.children[segment]
                currSegmentIndex++
            } else {
                currNode = null
            }
        }

        return null
    }

    private class Node {
        val children = mutableMapOf<Char, Node>()
        val values = mutableListOf<FqNameMatcher>()
    }
}

class FqNameMatcher(val pattern: String) {
    /**
     * A key for storing this matcher.
     */
    val key: String
    /**
     * Mask for generic type inclusion in stability calculation
     */
    val mask: Int

    private val regex: Regex?

    init {
        val matchResult = validPatternMatcher.matchEntire(pattern)
            ?: error("$pattern is not a valid pattern")

        val regexPatternBuilder = StringBuilder()
        val keyBuilder = StringBuilder()
        var hasWildcard = false

        var index = 0
        var hitGenericOpener = false
        while (index < pattern.length && !hitGenericOpener) {
            when (val c = pattern[index]) {
                STABILITY_WILDCARD_SINGLE -> {
                    hasWildcard = true
                    if (pattern.getOrNull(index + 1) == STABILITY_WILDCARD_SINGLE) {
                        regexPatternBuilder.append(PATTERN_MULTI_WILD)
                        index ++ // Skip a char to take the multi
                    } else {
                        regexPatternBuilder.append(PATTERN_SINGLE_WILD)
                    }
                }
                STABILITY_PACKAGE_SEPARATOR -> {
                    if (hasWildcard) {
                        regexPatternBuilder.append(PATTERN_PACKAGE_SEGMENT)
                    } else {
                        keyBuilder.append(STABILITY_PACKAGE_SEPARATOR)
                    }
                }
                STABILITY_GENERIC_OPEN -> {
                    hitGenericOpener = true
                }
                else -> {
                    if (hasWildcard) {
                        regexPatternBuilder.append(c)
                    } else {
                        keyBuilder.append(c)
                    }
                }
            }

            index++
        }

        // Pre-alloc regex for pattern having a wildcard at the end of the string
        // because it should be common.
        regex = if (regexPatternBuilder.isNotEmpty()) {
            when (val regexPattern = regexPatternBuilder.toString()) {
                singleWildcardSuffix.pattern -> singleWildcardSuffix
                multiWildcardSuffix.pattern -> multiWildcardSuffix
                else -> Regex(regexPattern)
            }
        } else {
            null
        }

        val genericMask = matchResult.groups["genericmask"]
        if (genericMask == null) {
            key = keyBuilder.toString()
            mask = 0.inv()
        } else {
            mask = genericMask.value
                .split(STABILITY_GENERIC_SEPARATOR)
                .map { if (it == STABILITY_GENERIC_INCLUDE) 1 else 0 }
                .reduceIndexed { i, acc, flag ->
                    acc or (flag shl i)
                }

            key = keyBuilder.subSequence(0, genericMask.range.first - 1).toString()
        }
    }

    fun matches(name: FqName?): Boolean {
        if (pattern == STABILITY_WILDCARD_MULTI) return true

        val nameStr = name?.asString() ?: return false
        val suffix = nameStr.substring(key.length)
        return when {
            regex != null -> nameStr.startsWith(key) && regex.matches(suffix)
            else -> key == name.asString()
        }
    }

    override fun equals(other: Any?): Boolean {
        val otherMatcher = other as? FqNameMatcher ?: return false
        return this.pattern == otherMatcher.pattern
    }

    override fun hashCode(): Int {
        return pattern.hashCode()
    }

    companion object {
        private const val PATTERN_SINGLE_WILD = "\\w+"
        private const val PATTERN_MULTI_WILD = "[\\w\\.]+"
        private const val PATTERN_PACKAGE_SEGMENT = "\\."

        private val validPatternMatcher =
            Regex("((\\w+\\*{0,2}|\\*{1,2})\\.)*" +
                "((\\w+(<?(?<genericmask>([*|_],)*[*|_])>)+)|(\\w+\\*{0,2}|\\*{1,2}))")
        private val singleWildcardSuffix = Regex(PATTERN_SINGLE_WILD)
        private val multiWildcardSuffix = Regex(PATTERN_MULTI_WILD)
    }
}
