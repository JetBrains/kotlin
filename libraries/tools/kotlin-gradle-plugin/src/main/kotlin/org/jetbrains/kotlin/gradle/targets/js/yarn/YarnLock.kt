/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import com.google.gson.stream.JsonReader
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import java.io.File
import java.io.StringReader

data class YarnLock(val entries: List<Entry>) {
    data class Entry(
        val key: String,
        val version: String?,
        val resolved: String?,
        val integrity: String?,
        val dependencies: List<Dependency>
    )

    data class Dependency(val key: String, val version: String?) {
        val scopedName: PackageJson.ScopedName
            get() = PackageJson.scopedName(key)
    }

    companion object {
        fun dependencyKey(packageKey: String, version: String) = "$packageKey@$version"

        private class Node(val parent: Node?, val value: String? = null) {
            var indent: String? = null
            val children = mutableMapOf<String, Node>()
        }

        fun parse(yarnLock: File): YarnLock {
            val root = parseTree(yarnLock)

            return YarnLock(root.children.map {
                val values = it.value.children
                Entry(
                    it.key,
                    values["version"]?.value,
                    values["resolved"]?.value,
                    values["integrity"]?.value,
                    values["dependencies"]?.children?.map { dep ->
                        Dependency(dep.key, dep.value.value)
                    } ?: listOf()
                )
            })
        }

        private fun parseTree(yarnLock: File): Node {
            val root = Node(null)
            var parent = root
            var onNewLevel = true
            yarnLock.useLines {
                it.forEach { line ->
                    if (!line.startsWith("#")) {
                        var i = 0
                        while (i < line.length && line[i].isWhitespace()) i++
                        val indentPos = i
                        val indent = line.substring(0, indentPos)

                        val key = line.endsWith(":")

                        if (line.isNotEmpty()) {
                            val values = parseLine(line, indentPos)

                            if (onNewLevel) {
                                parent.indent = indent
                                onNewLevel = false
                            } else {
                                while (indent != parent.indent!!) {
                                    parent = parent.parent!!
                                }
                            }

                            val child = Node(parent, if (values.size > 1) values[1] else null)
                            if (key) {
                                values.forEach {
                                    parent.children[it] = child
                                }
                                parent = child
                                onNewLevel = true
                            } else {
                                parent.children[values[0]] = child
                            }
                        }
                    }
                }
            }
            return root
        }

        private fun parseLine(line: String, indentPos: Int): List<String> {
            val quotedLineResult = replaceQuoted(line.removeSuffix(":"))

            return quotedLineResult
                .parse(indentPos)
        }

        private fun replaceQuoted(line: String): QuotedLineResult {
            var result: String = line
            val quotedValues = QUOTED_VALUE.findAll(line)
                .flatMap { it.groupValues.drop(1).asSequence() }
                .toList()

            quotedValues.forEachIndexed { index, item ->
                result = result.replace(
                    """
                        "$item"
                    """.trimIndent(),
                    """
                        "$$index"
                    """.trimIndent()
                )
            }

            return QuotedLineResult(
                result,
                quotedValues
            )
        }
    }
}

private data class QuotedLineResult(
    val value: String,
    val quotedValues: List<String>
)

private fun QuotedLineResult.parse(indentPos: Int): List<String> {
    return value
        .substring(indentPos)
        .split(" ")
        .map { value ->
            val withoutComma = value.removeSuffix(",")
            if (withoutComma.startsWith("\"")) {
                val (index) = QUOTED_PLACEHOLDER.find(withoutComma)!!.destructured
                val restoredValue = """
                    "${quotedValues[index.toInt()]}"
                """.trimIndent()

                try {
                    JsonReader(StringReader(restoredValue)).nextString()
                } catch (e: Throwable) {
                    restoredValue.removePrefix("\"").removeSuffix("\"")
                }
            } else withoutComma
        }
}

private val QUOTED_VALUE = """"(.+?)"""".toRegex()
private val QUOTED_PLACEHOLDER = """\$(\d+)""".toRegex()