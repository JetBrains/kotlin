/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping

import androidx.compose.compiler.mapping.group.GroupInfo
import androidx.compose.compiler.mapping.group.GroupType
import androidx.compose.compiler.mapping.group.LambdaKeyCache
import org.jetbrains.annotations.TestOnly

@TestOnly
fun ClassInfo.render(lambdaKeys: LambdaKeyCache): String = buildString {
    append(classId.fqName)
    append(" {")
    appendLine()

    withIndent {
        append("file: ")
        append(fileName)
        appendLine()
        appendLine(methods.joinToString("\n") {
            it.render(lambdaKeys)
        })
    }

    appendLine("}")
}

private fun MethodInfo.render(lambdaKeys: LambdaKeyCache): String = buildString {
    append(id.methodName)
    append(id.methodDescriptor)
    appendLine()

    withIndent {
        appendLine(groups.joinToString("\n") {
            if (it.type == GroupType.Root && it.key == null) {
                it.render(resolveLambdaKey(lambdaKeys) ?: it.key)
            } else {
                it.render(it.key)
            }
        })
    }
}.trim()

private fun MethodInfo.resolveLambdaKey(lambdaKeys: LambdaKeyCache): Int? =
    lambdaKeys[id.toString()] ?: lambdaKeys[id.classId.fqName]

private fun GroupInfo.render(key: Int?): String = buildString {
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