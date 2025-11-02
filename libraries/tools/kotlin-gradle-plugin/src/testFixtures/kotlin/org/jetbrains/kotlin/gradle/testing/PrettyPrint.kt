/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import org.gradle.api.file.FileCollection
import java.io.File
import kotlin.reflect.full.memberProperties

/** Pretty print diffable by text and copypastable collection-like hierarchies */
class PrettyPrint<T : Any>(
    val value: T,
    val indentation: Int,
) {
    override fun toString(): String {
        val twoSpaces = " ".repeat(2)
        val indentationSpace = " ".repeat(indentation)
        val nextIndentationDepth = indentation + 2
        val elements: Array<String> = when (value) {
            is FileCollection -> {
                val files = value.files.map { it.absolutePath }
                arrayOf(
                    PrettyPrintedFileCollection(files).prettyPrinted(indentation).toString()
                )
            }
            is PrettyPrintedFileCollection -> arrayOf(
                "prettyPrintedFileCollectionOf(",
                *value.map { twoSpaces + it.prettyPrinted(nextIndentationDepth) + "," }.toTypedArray(),
                ")"
            )

            is Map<*, *> -> arrayOf(
                "mapOf(",
                *value.map { it }.sortedBy { it.key.toString() }.map {
                    "${twoSpaces}${it.key?.prettyPrinted()} to ${it.value?.prettyPrinted(nextIndentationDepth)},"
                }.toTypedArray(),
                ")",
            )
            is Iterable<*> -> {
                val orderedValue = if (value is Set<*>) value.sortedBy { it.toString() }.toSet() else value
                val innerValue = orderedValue.map { "${twoSpaces}${it?.prettyPrinted(nextIndentationDepth)}," }.toTypedArray()
                when (orderedValue) {
                    is Set<*> -> arrayOf(
                        "setOf(",
                        *innerValue,
                        ")",
                    )
                    else -> arrayOf(
                        "listOf(",
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
                    } else if (value is File) {
                        arrayOf("File(\"${value}\")")
                    } else {
                        arrayOf(value.toString())
                    }
                } else {
                    val kClass = value::class
                    arrayOf(
                        "${kClass.simpleName}(",
                        *kClass.memberProperties.map { prop ->
                            "${twoSpaces}${prop.name} = ${prop.getter.call(value)?.prettyPrinted(nextIndentationDepth)},"
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
        if (other !is PrettyPrint<*>) return false
        return value.equals(other.value)
    }

    private fun Any.prettyPrinted(indentation: Int = 0): PrettyPrint<Any> = PrettyPrint(this, indentation)
}

val <T : Any> T.prettyPrinted: PrettyPrint<T> get() = PrettyPrint(this, 0)

private class PrettyPrintedFileCollection(files: List<String>) : List<String> by files
fun prettyPrintedFileCollectionOf(vararg files: String): List<String> = PrettyPrintedFileCollection(files.toList())