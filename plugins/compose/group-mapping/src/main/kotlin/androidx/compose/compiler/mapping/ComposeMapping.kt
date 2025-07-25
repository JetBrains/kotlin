/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping

import androidx.compose.compiler.mapping.bytecode.JvmDescriptor
import androidx.compose.compiler.mapping.group.GroupInfo
import androidx.compose.compiler.mapping.group.GroupType
import androidx.compose.compiler.mapping.group.LambdaKeyCache

class ComposeMapping(
    private val reporter: ErrorReporter
) {
    private val entries = mutableListOf<Entry>()
    private val keyCache = LambdaKeyCache()

    private class Entry(
        val cls: ClassInfo,
        val method: MethodInfo,
        val group: GroupInfo,
        val functionRoot: Boolean
    )

    fun append(bytecode: ByteArray) {
        val cls = context(reporter, keyCache) { ClassInfo(bytecode) }
        val entries = buildList {
            cls.methods.forEach { method ->
                method.groups.forEachIndexed { i, group ->
                    // last closed group is always root
                    val isRoot = i == method.groups.lastIndex
                    add(Entry(cls, method, group, isRoot))
                }
            }
        }
        this.entries.addAll(entries)
    }

    fun writeProguardMapping(writer: Appendable) {
        writer.appendLine("ComposeStackTrace -> ${"$$"}compose:")
        entries.patchGroupKeys().forEach { entry ->
            writer.appendEntry(entry.cls, entry.method, entry.group)
        }
    }

    private fun Appendable.appendEntry(
        cls: ClassInfo,
        method: MethodInfo,
        group: GroupInfo
    ) {
        if (group.key == null) return
        if (group.line == -1) return

        append("  ")
        append("1:1:")
        append(descriptorToProguardString("${cls.classId.fqName}.${method.id.methodName}", method.id.methodDescriptor))
        append(":")
        append(group.line.toString())
        append(":")
        append(group.line.toString())
        append(" -> ")
        append("m$")
        append(group.key.toString())
        appendLine()
    }

    private fun descriptorToProguardString(name: String, descriptor: String): String {
        if (descriptor.isEmpty()) return "$name()"

        fun descriptorToJavaType(d: String): String =
            when (d) {
                "V" -> "void"
                "Z" -> "boolean"
                "B" -> "byte"
                "I" -> "int"
                "J" -> "long"
                "S" -> "short"
                "F" -> "float"
                "D" -> "double"
                "C" -> "char"
                else -> {
                    if (d.startsWith('L')) {
                        d.substring(1, d.length - 1).replace('/', '.')
                    } else if (d.startsWith('[')) {
                        descriptorToJavaType(d.drop(1)) + "[]"
                    } else {
                        error("Unknown descriptor $d")
                    }
                }
            }

        val desc = JvmDescriptor.fromString(descriptor)

        return desc.parameters.joinToString(
            separator = ",",
            prefix = "${descriptorToJavaType(desc.returnType)} $name(",
            postfix = ")",
        ) {
            descriptorToJavaType(it)
        }
    }

    private fun List<Entry>.patchGroupKeys(): List<Entry> {
        val result = mutableListOf<Entry>()
        val existingKeyPositions = mutableMapOf<Int, Int>()

        for (entry in this) {
            entry.group.key = entry.resolveLambdaKey()
            val key = entry.group.key ?: continue
            val previousIndex = existingKeyPositions[key]
            if (previousIndex != null) {
                if (entry.functionRoot) {
                    // prefer function roots when deduplicating
                    // they usually indicate groups from inline functions
                    result[previousIndex] = entry
                }
            } else {
                result.add(entry)
                existingKeyPositions[key] = result.lastIndex
            }
        }

        return result
    }

    private fun Entry.resolveLambdaKey(): Int? =
        if (group.key == null && group.type == GroupType.Root) {
            keyCache[cls.classId.fqName] ?: keyCache[method.id.toString()]
        } else {
            group.key
        }
}