/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import kotlin.reflect.full.memberProperties

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
                "{",
                *value.map { it }.sortedBy { it.key.toString() }.map { "${twoSpaces}${it.key}=${it.value?.pp(nextIndentationDepth)}," }.toTypedArray(),
                "}",
            )
            is Iterable<*> -> arrayOf(
                "[",
                *value.map { "${twoSpaces}${it?.pp(nextIndentationDepth)}," }.toTypedArray(),
                "]",
            )
            else -> {
                val packageName = value::class.java.packageName
                if (packageName.startsWith("kotlin.") || packageName.startsWith("java.")) {
                    arrayOf(value.toString())
                } else {
                    val kClass = value::class
                    arrayOf(
                        "${kClass.simpleName}(",
                        *kClass.memberProperties.map { prop ->
                            "${twoSpaces}${prop.name}=${prop.getter.call(value)?.pp(nextIndentationDepth)},"
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
}

fun Any.pp(indentation: Int = 0): PP = PP(this, indentation)