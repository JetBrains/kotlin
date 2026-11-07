/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.KdType
import java.lang.reflect.Modifier

abstract class AbstractKdmTypesGoldenTest : AbstractKdmGoldenTest() {
    fun runTest(testDataFilePath: String) {
        val file = testDataFile(testDataFilePath)
        val bundle = generateDocumentationBundleFromTestData(file)
        assertMatchesGoldenTextFile(
            buildString {
                fun appendTypes(value: Any?, path: String) {
                    when (value) {
                        is KdType -> {
                            append(path).append(":\n")
                            append(value.dumpAllFields())
                        }
                        is Map<*, *> -> value.entries.sortedBy { it.key.toString() }.forEach { entry ->
                            appendTypes(entry.value, "$path[${entry.key}]")
                        }
                        is List<*> -> value.forEachIndexed { index, item -> appendTypes(item, "$path[$index]") }
                        else -> {
                            if (value == null || value.javaClass.`package`.name != "org.jetbrains.kotlin.documentation") return
                            for (field in value.javaClass.declaredFields
                                .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
                                .sortedBy { it.name }
                            ) {
                                field.isAccessible = true
                                appendTypes(field.get(value), "$path.${field.name}")
                            }
                        }
                    }
                }
                appendTypes(bundle.canonicalized(), "bundle")
            },
            file,
            ".types.txt",
        )
    }
}

private fun KdType.dumpAllFields(): String = buildString {
    fun appendValue(value: Any?, indent: String) {
        when (value) {
            null -> append("null")
            is String -> {
                append('"')
                append(
                    value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                )
                append('"')
            }
            is KdElementId -> appendValue(value.value, indent)
            is Enum<*> -> append(value.name)
            is List<*> -> {
                if (value.isEmpty()) {
                    append("[]")
                } else {
                    append("[\n")
                    for (item in value) {
                        append(indent).append("  ")
                        appendValue(item, "$indent  ")
                        append('\n')
                    }
                    append(indent).append(']')
                }
            }
            else -> {
                if (value.javaClass.`package`?.name != "org.jetbrains.kotlin.documentation") {
                    append(value)
                } else {
                    append(value.javaClass.simpleName).append(" {\n")
                    for (field in value.javaClass.declaredFields
                        .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
                        .sortedBy { it.name }
                    ) {
                        field.isAccessible = true
                        append(indent).append("  ").append(field.name).append(" = ")
                        appendValue(field.get(value), "$indent  ")
                        append('\n')
                    }
                    append(indent).append('}')
                }
            }
        }
    }
    appendValue(this@dumpAllFields, "")
    append('\n')
}
