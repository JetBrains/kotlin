package androidx.compose.compiler.mapping.group

import androidx.compose.compiler.mapping.ClassId
import androidx.compose.compiler.mapping.ComposeMappingErrorReporter
import androidx.compose.compiler.mapping.MethodId
import androidx.compose.compiler.mapping.token.BytecodeToken
import androidx.compose.compiler.mapping.token.BytecodeToken.*
import androidx.compose.compiler.mapping.token.tokenizeBytecode
import org.objectweb.asm.Label
import org.objectweb.asm.tree.MethodNode

context(reporter: ComposeMappingErrorReporter)
internal fun analyzeGroups(classId: ClassId, methodNode: MethodNode): List<GroupInfo> {
    if (methodNode.instructions.size() == 0) {
        // Ignore functions with no bodies (e.g. abstract)
        return emptyList()
    }

    val methodId = MethodId(classId, methodNode.name, methodNode.desc)

    val tokens = tokenizeBytecode(methodNode.instructions).getOrElse { e ->
        reporter.reportError(IllegalStateException("Failed to tokenize $methodId", e))
        return methodNode.emptyGroup()
    }

    return parseGroupInfoFromTokens(methodNode, tokens).getOrElse { e ->
        reporter.reportError(IllegalStateException("Failed to parse $methodId", e))
        return methodNode.emptyGroup()
    }
}

private fun MethodNode.emptyGroup(): List<GroupInfo> =
    listOf(
        GroupInfo(
            key = readFunctionKeyMetaAnnotation(),
            type = GroupType.Root,
            line = -1
        )
    )

private fun parseGroupInfoFromTokens(
    methodNode: MethodNode,
    tokens: List<BytecodeToken>
): Result<List<GroupInfo>> {
    val functionKey = methodNode.readFunctionKeyMetaAnnotation()

    val nodeStack = mutableListOf(
        GroupInfo(
            key = functionKey,
            type = GroupType.Root,
            line = -1
        )
    )
    val labels = mutableListOf<Label>()
    val result = mutableListOf<GroupInfo>()
    var currentLine = -1
    for (i in tokens.indices) {
        val token = tokens[i]
        val currentNode = nodeStack.lastOrNull()
        when (token) {
            is LineToken -> {
                val unset = currentLine == -1
                currentLine = token.lineInsn.line

                if (unset) {
                    // Assume at most one line per group and work backwards
                    // In most cases only root group info is missing
                    for (i in nodeStack.indices) {
                        val node = nodeStack[nodeStack.size - 1 - i]
                        node.line = (currentLine - i).coerceAtLeast(0)
                    }
                }
            }

            is StartRestartGroup -> {
                val newNode = GroupInfo(key = token.key, type = GroupType.RestartGroup, line = currentLine)

                if (currentNode?.type == GroupType.Root && (currentNode.key == null || currentNode.key == token.key)) {
                    nodeStack.removeLast() // The restart group is the root group
                }

                nodeStack.add(newNode)
            }

            is StartReplaceGroup -> {
                val newNode = GroupInfo(key = token.key, type = GroupType.ReplaceGroup, line = currentLine)

                if (currentNode?.type == GroupType.Root && (currentNode.key == null || currentNode.key == token.key)) {
                    nodeStack.removeLast() // The restart group is the root group
                }

                nodeStack.add(newNode)
            }

            is EndRestartGroup -> {
                if (currentNode != null && currentNode.incompleteLabels.isEmpty()) {
                    if (currentNode.type != GroupType.RestartGroup) {
                        return parseError("EndRestartGroup is not allowed in ${currentNode.type} scope")
                    }

                    val node = nodeStack.removeLast()
                    result += node
                }
            }

            is EndReplaceGroup -> {
                if (currentNode != null && currentNode.incompleteLabels.isEmpty()) {
                    if (currentNode.type != GroupType.ReplaceGroup) {
                        return parseError("EndReplaceGroup is not allowed in ${currentNode?.type} scope")
                    }
                    val node = nodeStack.removeLast()
                    result += node
                }
            }

            is JumpToken -> {
                val label = token.jumpInsn.label.label
                val labelIndex = tokens.indexOfFirst { it is LabelToken && it.labelInsn == label }
                if (labelIndex > i) {
                    // Only consider forward branches, backward branches are already processed
                    currentNode?.incompleteLabels += label
                }
                if (label !in labels) {
                    labels += label
                }
            }

            is LabelToken -> {
                val label = token.labelInsn
                nodeStack.forEach {
                    it.incompleteLabels -= label.label
                }
            }

            is CurrentMarkerToken -> {
                currentNode?.markers?.add(token.variableIndex)
            }

            is EndToMarkerToken -> {
                val marker = token.variableIndex
                val nodeIndex = nodeStack.indexOfFirst { marker in it.markers }
                if (nodeIndex == -1) {
                    continue
                }

                for (i in (nodeStack.size - 1) downTo nodeIndex) {
                    val node = nodeStack[i]
                    if (node.incompleteLabels.isEmpty()) {
                        nodeStack.removeAt(i)
                        result += node
                    }
                }
            }
        }
    }

    check(nodeStack.size <= 1) {
        "Expected optional root node, but found ${nodeStack.size}"
    }

    val rootNode = nodeStack.firstOrNull()
    if (rootNode != null) {
        result.add(rootNode)
    }

    return Result.success(result)
}

private fun <T> parseError(message: String): Result<T> = Result.failure(ParseException(message))
private class ParseException(message: String) : RuntimeException(message)