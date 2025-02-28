/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

// Pretty print diffable by text and copypastable collection-like hierarchies
class PP(
    val value: Any,
    val indentation: Int,
) {
    override fun toString(): String {
        val twoSpaces = " ".repeat(2)
        val indentationSpace = " ".repeat(indentation)
        val nextIndentationDepth = indentation + 2
        val elements: Array<String> = when (value) {
            is Map<*, *> -> arrayOf(
                "mutableMapOf(",
                *value.map { it }.sortedBy { it.key.toString() }.map {
                    "${twoSpaces}${it.key?.pp()} to ${it.value?.pp(nextIndentationDepth)},"
                }.toTypedArray(),
                ")",
            )
            is Iterable<*> -> {
                val innerValue = value.map { "${twoSpaces}${it?.pp(nextIndentationDepth)}," }.toTypedArray()
                when (value) {
                    is Set<*> -> arrayOf(
                        "mutableSetOf(",
                        *innerValue,
                        ")",
                    )
                    is List<*> -> arrayOf(
                        "mutableListOf(",
                        *innerValue,
                        ")",
                    )
                    else -> arrayOf(
                        "mutableListOf(",
                        *innerValue,
                        ")",
                    )
                }
            }
            else -> {
                val packageName = value::class.java.`package`.name
                if (packageName.startsWith("kotlin.") || packageName.startsWith("java.")) {
                    if (value is String) {
                        arrayOf("\"${value}\"")
                    } else {
                        arrayOf(value.toString())
                    }
                } else {
                    val kClass = value::class
                    arrayOf(
                        "${kClass.simpleName}(",
                        *kClass.memberProperties.map { prop ->
                            "${twoSpaces}${prop.name} = ${prop.getter.call(value)?.pp(nextIndentationDepth)},"
                        }.toTypedArray(),
                        ")",
                    )
                }
            }
        }

        if (elements.size == 1) return elements[0]

        return (listOf(elements[0]) + elements.toList().subList(1, elements.size).map { "${indentationSpace}${it}" }).joinToString("\n")
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        var otherUnwrapped = other
        if (other is PP) otherUnwrapped = other.value
        return value.equals(otherUnwrapped)
    }

    private fun Any.pp(indentation: Int = 0): PP = PP(this, indentation)
}

object AssertionTypeCollapsedToAny : AssertionError("Assertion type parameter collapsed to Any, please specify type explicitly")

// FIXME: Hack @OnlyInputTypes?
inline fun <reified T> assertEqualsPP(expected: T, actual: T) {
    if (T::class.java == Any::class.java) {
        throw AssertionTypeCollapsedToAny
    }
    assertEquals(PP(expected as Any, 0), PP(actual as Any, 0))
}