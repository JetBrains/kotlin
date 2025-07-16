/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

import androidx.compose.compiler.group.analysis.BytecodeToken.*
import org.objectweb.asm.tree.MethodNode

class GroupInfo(
    val key: ComposeGroupKey?,
    val type: GroupType,
    var line: Int = -1,
)

enum class GroupType {
    NoGroup,
    RestartGroup,
    ReplaceGroup,
}

internal fun parseGroupInfo(methodNode: MethodNode): List<GroupInfo> {
    // TODO: log failures if possible

    val tokens = tokenizeBytecode(methodNode.instructions).getOrElse { e ->
        return methodNode.emptyGroup()
    }

    return parseGroupInfoFromTokens(methodNode, tokens).getOrElse { e ->
        return methodNode.emptyGroup()
    }
}

private fun MethodNode.emptyGroup(): List<GroupInfo> =
    listOf(
        GroupInfo(
            key = readFunctionKeyMetaAnnotation(),
            type = GroupType.NoGroup,
        )
    )

private fun parseGroupInfoFromTokens(
    methodNode: MethodNode,
    tokens: List<BytecodeToken>
): Result<List<GroupInfo>> {
    if (tokens.isEmpty()) return Failure("empty tokens")

    val functionKey = methodNode.readFunctionKeyMetaAnnotation()

    val nodeStack = mutableListOf(
        GroupInfo(
            key = functionKey,
            type = GroupType.NoGroup
        )
    )
    val result = mutableListOf<GroupInfo>()
    var currentLine = -1
    for (i in tokens.indices) {
        val token = tokens[i]
        val currentNode = nodeStack.lastOrNull()
        when (token) {
            is BlockToken -> {
                // Do nothing
            }

            is LineToken -> {
                currentLine = token.lineInsn.line
            }

            is StartRestartGroup -> {
                val newNode = GroupInfo(key = token.key, type = GroupType.RestartGroup, line = currentLine)

                if (currentNode?.key == token.key) {
                    nodeStack.removeLast() // The restart group is the root group
                }

                nodeStack.add(newNode)
            }

            is EndRestartGroup -> {
                if (currentNode?.type != GroupType.RestartGroup) {
                    return Failure("EndRestartGroup is not allowed in ${currentNode?.type} scope")
                }
                val node = nodeStack.removeLast()
                result += node
            }

            is StartReplaceGroup -> {
                val newNode = GroupInfo(key = token.key, type = GroupType.ReplaceGroup, line = currentLine)

                if (currentNode?.key == token.key) {
                    nodeStack.removeLast() // The replace group is the root group
                    newNode.line = currentNode.line
                }

                nodeStack.add(newNode)
            }

            is EndReplaceGroup -> {
                if (currentNode?.type != GroupType.ReplaceGroup) {
                    return Failure("EndReplaceGroup is not allowed in ${currentNode?.type} scope")
                }
                val node = nodeStack.removeLast()
                result += node
            }
        }
    }

    check(nodeStack.size <= 1) { "Expected optional root node, but found ${nodeStack.size}" }

    val rootNode = nodeStack.firstOrNull()
    if (rootNode != null) {
        result.add(rootNode)
    }

    return Result.success(result)
}

internal fun <T> Failure(message: String) = Result.failure<T>(
    IllegalStateException(message)
)