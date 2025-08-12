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
        val group: GroupInfo
    )

    fun append(bytecode: ByteArray) {
        val cls = context(reporter, keyCache) { ClassInfo(bytecode) }
        val entries = buildList {
            cls.methods.forEach { method ->
                method.groups.forEach { group ->
                    add(Entry(cls, method, group))
                }
            }
        }
        this.entries.addAll(entries)
    }

    fun asProguardMapping(): String = buildString {
        appendLine("ComposeStackTrace -> ${"$$"}compose:")
        val additionalFileNames = mutableListOf<Entry>()
        entries.forEach { entry ->
            appendEntry(entry.cls, entry.method, entry.group)
            if (entry.cls.classId.value.contains("ComposableSingletons$")) {
                additionalFileNames.add(entry)
            }
        }

        additionalFileNames.forEach { entry ->
            appendFileName(entry.cls)
        }
    }

    private fun StringBuilder.appendEntry(
        cls: ClassInfo,
        method: MethodInfo,
        group: GroupInfo
    ) {
        var key = group.key
        if (key == null) {
            if (group.type != GroupType.Root) return

            key = keyCache[cls.classId.fqName] ?: keyCache[method.id.toString()]
        }

        if (key == null) return
        if (group.line == -1) return

        append("  ")
        append("1:1:")
        append(descriptorToProguardString("${cls.classId.fqName}.${method.id.methodName}", method.id.methodDescriptor))
        append(":")
        append(group.line)
        append(":")
        append(group.line)
        append(" -> ")
        append("m$")
        append(key)
        appendLine()
    }

    private fun StringBuilder.appendFileName(cls: ClassInfo) {
        val fqName = cls.classId.fqName
        append(fqName)
        append(" -> ")
        append(fqName)
        append(":")
        appendLine()
        append("# {\"id\":\"sourceFile\",\"fileName\":\"")
        append(cls.fileName)
        append("\"}")
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
}