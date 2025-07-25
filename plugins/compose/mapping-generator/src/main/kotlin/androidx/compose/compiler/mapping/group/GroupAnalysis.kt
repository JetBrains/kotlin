package androidx.compose.compiler.mapping.group

import androidx.compose.compiler.mapping.ClassId
import androidx.compose.compiler.mapping.ErrorReporter
import androidx.compose.compiler.mapping.MethodId
import androidx.compose.compiler.mapping.bytecode.BytecodeToken
import androidx.compose.compiler.mapping.bytecode.BytecodeToken.*
import androidx.compose.compiler.mapping.bytecode.EndGroupToken
import androidx.compose.compiler.mapping.bytecode.StartGroupToken
import androidx.compose.compiler.mapping.bytecode.tokenizeBytecode
import org.objectweb.asm.Label
import org.objectweb.asm.tree.MethodNode

context(reporter: ErrorReporter)
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

private class GroupInfoBuilder(
    val key: Int?,
    val type: GroupType,
    var line: Int = -1
) {
    var incompleteLabels = mutableListOf<Label>()
    var markers = mutableListOf<Int>()

    fun build() = GroupInfo(key, type, line)
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
        GroupInfoBuilder(
            key = functionKey,
            type = GroupType.Root,
            line = -1
        )
    )
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

            is StartGroupToken -> {
                val newNode = GroupInfoBuilder(key = token.key, type = token.type, line = currentLine)

                if (currentNode?.type == GroupType.Root && (currentNode.key == null || currentNode.key == token.key)) {
                    nodeStack.removeLast() // This group is the root group
                }

                nodeStack.add(newNode)
            }

            is EndGroupToken -> {
                if (currentNode != null && currentNode.incompleteLabels.isEmpty()) {
                    if (currentNode.type != token.type) {
                        return parseError("EndRestartGroup is not allowed in ${currentNode.type} scope")
                    }

                    val node = nodeStack.removeLast()
                    result += node.build()
                }
            }

            is JumpToken -> {
                val label = token.jumpInsn.label.label
                val labelIndex = tokens.indexOfFirst { it is LabelToken && it.labelInsn == label }
                if (labelIndex > i) {
                    // Only consider forward branches, backward branches are already processed
                    currentNode?.incompleteLabels += label
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
                        result += node.build()
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
        result.add(rootNode.build())
    }

    return Result.success(result)
}

private fun <T> parseError(message: String): Result<T> = Result.failure(ParseException(message))
private class ParseException(message: String) : RuntimeException(message)