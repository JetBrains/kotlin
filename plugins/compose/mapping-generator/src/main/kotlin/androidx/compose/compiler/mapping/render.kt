/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping

import androidx.compose.compiler.mapping.group.GroupInfo
import org.jetbrains.annotations.TestOnly

@TestOnly
fun ClassInfo.render(): String = buildString {
    append(classId.fqName)
    append(" {")
    appendLine()

    withIndent {
        append("file: ")
        append(fileName)
        appendLine()
        appendLine(methods.joinToString("\n") { it.render() })
    }

    appendLine("}")
}

private fun MethodInfo.render(): String = buildString {
    append(id.methodName)
    append(id.methodDescriptor)
    appendLine()

    withIndent {
        appendLine(groups.joinToString("\n") { it.render() })
    }
}.trim()

private fun GroupInfo.render(): String = buildString {
    append(type)
    append(" { ")
    append("key: ")
    append(key)
    append(", ")
    append("line: ")
    append(line)
    append(" }")
}

private fun StringBuilder.withIndent(builder: StringBuilder.() -> Unit) {
    appendLine(buildString(builder).trim().prependIndent("    "))
}