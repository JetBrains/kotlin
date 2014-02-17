/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import java.util.ArrayList

fun List<Node>.toKotlin(separator: String, prefix: String = "", suffix: String = ""): String {
    val result = StringBuilder()
    if (size() > 0) {
        result.append(prefix)
        var first = true
        for (x in this) {
            if (!first) result.append(separator)
            first = false
            result.append(x.toKotlin())
        }
        result.append(suffix)
    }
    return result.toString()
}

fun Collection<Modifier>.toKotlin(separator: String = " "): String {
    val result = StringBuilder()
    for (x in this) {
        result.append(x.name)
        result.append(separator)
    }
    return result.toString()
}

fun String.withSuffix(suffix: String): String = if (isEmpty()) "" else this + suffix
fun String.withPrefix(prefix: String): String = if (isEmpty()) "" else prefix + this
fun Expression.withPrefix(prefix: String): String = if (isEmpty()) "" else prefix + toKotlin()

open class WhiteSpaceSeparatedElementList(
        val elements: List<Element>,
        val minimalWhiteSpace: WhiteSpace,
        val ensureSurroundedByWhiteSpace: Boolean = true
) {
    val nonEmptyElements = elements.filterNot { it.isEmpty() }

    fun isEmpty() = nonEmptyElements.all { it is WhiteSpace }

    fun toKotlin(): String {
        if (isEmpty()) {
            return ""
        }
        return nonEmptyElements.surroundWithWhiteSpaces().insertAndMergeWhiteSpaces().map { it.toKotlin() }.makeString("")
    }

    private fun List<Element>.surroundWithWhiteSpaces(): List<Element> {
        if (!ensureSurroundedByWhiteSpace) {
            return this
        }
        val result = ArrayList<Element>()
        result.add(minimalWhiteSpace)
        result.addAll(this)
        result.add(minimalWhiteSpace)
        return result
    }


    // ensure that there is whitespace between non-whitespace elements
    // choose maximum among subsequent whitespaces
    // all resulting whitespaces are at least minimal whitespace
    private fun List<Element>.insertAndMergeWhiteSpaces(): List<Element> {
        var currentWhiteSpace: WhiteSpace? = null
        val result = ArrayList<Element>()
        var isFirst = true
        for (element in this) {
            if (element is WhiteSpace) {
                if (currentWhiteSpace == null || element > currentWhiteSpace!!) {
                    currentWhiteSpace = if (element > minimalWhiteSpace) element else minimalWhiteSpace
                }
            }
            else {
                if (!isFirst) {
                    //do not insert whitespace before first element
                    result.add(currentWhiteSpace ?: minimalWhiteSpace)
                }
                result.add(element)
                currentWhiteSpace = null
            }
            isFirst = false
        }
        if (currentWhiteSpace != null) {
            result.add(currentWhiteSpace!!)
        }
        return result
    }
}