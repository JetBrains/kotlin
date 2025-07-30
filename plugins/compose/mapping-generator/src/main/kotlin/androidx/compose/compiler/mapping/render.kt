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
context(lambdaKeys: LambdaKeyCache)
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

context(lambdaKeys: LambdaKeyCache)
private fun MethodInfo.render(): String = buildString {
    append(id.methodName)
    append(id.methodDescriptor)
    appendLine()

    withIndent {
        appendLine(groups.joinToString("\n") {
            if (it.type == GroupType.Root && it.key == null) {
                it.render(resolveLambdaKey() ?: it.key)
            } else {
                it.render(it.key)
            }
        })
    }
}.trim()

context(lambdaKeys: LambdaKeyCache)
private fun MethodInfo.resolveLambdaKey(): Int? =
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