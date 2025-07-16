/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

object ComposeMapping {
    fun fromBytecode(bytecode: ByteArray): String = buildString {
        val cls = ClassInfo(bytecode)
        cls.methods.forEach { method ->
            method.groups.forEach { group ->
                if (group.key != null) {
                    appendEntry(cls, method, group)
                    appendLine()
                }
            }
        }
    }

    private fun StringBuilder.appendEntry(
        cls: ClassInfo,
        method: MethodInfo,
        group: GroupInfo
    ) {
        if (group.key == null) return

        append("  ")
        append("1:1:")
        append(descriptorToProguardString("${cls.classId.fqName}.${method.id.methodName}", method.id.methodDescriptor))
        append(":")
        append(group.line)
        append(":")
        append(group.line)
        append(" -> ")
        append("m$")
        append(group.key.key.toString())
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

        val parameterString = descriptor.takeWhile { it != ')' }.dropWhile { it == '(' }
        val parameters = sequence {
            var i = 0
            while (i < parameterString.length) {
                val start = i
                var current = parameterString[i]
                while (current == '[') {
                    i++
                    current = parameterString[i]
                }
                val end = if (current == 'L') {
                    parameterString.indexOf(';', i) + 1
                } else {
                    i + 1
                }
                yield(parameterString.substring(start, end))
                i = end
            }
        }.map {
            descriptorToJavaType(it)
        }
        val returnType = descriptor.takeLastWhile { it != ')' }

        return parameters.joinToString(
            separator = ",",
            prefix = "${descriptorToJavaType(returnType)} $name(",
            postfix = ")",
        )
    }
}