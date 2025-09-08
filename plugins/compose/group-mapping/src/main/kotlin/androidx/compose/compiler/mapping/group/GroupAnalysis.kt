package androidx.compose.compiler.mapping.group

import androidx.compose.compiler.mapping.ClassId
import androidx.compose.compiler.mapping.ErrorReporter
import androidx.compose.compiler.mapping.MethodId
import androidx.compose.compiler.mapping.bytecode.*
import androidx.compose.compiler.mapping.bytecode.BytecodeToken.*
import org.objectweb.asm.Label
import org.objectweb.asm.tree.MethodNode

context(reporter: ErrorReporter, keyCache: LambdaKeyCache)
internal fun analyzeGroups(
    classId: ClassId,
    methodNode: MethodNode
): List<GroupInfo> {
    if (methodNode.instructions.size() == 0) {
        // Ignore functions with no bodies (e.g. abstract)
        return emptyList()
    }

    val methodId = MethodId(classId, methodNode.name, methodNode.desc)

    val isComposable = methodNode.isComposable()
    val tokens = tokenizeBytecode(methodNode.instructions, isComposable).getOrElse { e ->
        reporter.reportError(IllegalStateException("Failed to tokenize $methodId", e))
        return methodNode.emptyGroup(isComposable)
    }

    if (tokens.isEmpty()) {
        return methodNode.emptyGroup(isComposable)
    }

    return parseGroupInfoFromTokens(methodNode, tokens, isComposable).getOrElse { e ->
        reporter.reportError(IllegalStateException("Failed to parse $methodId", e))
        return methodNode.emptyGroup(isComposable)
    }
}

private class GroupInfoBuilder(
    val key: Int?,
    val type: GroupType,
    var line: Int = -1
) {
    val incompleteLabels = mutableSetOf<Label>()
    val markers = mutableSetOf<Int>()

    fun build() = GroupInfo(key, type, line)
}

private fun MethodNode.emptyGroup(isComposable: Boolean): List<GroupInfo> =
    if (isComposable) {
        listOf(
            GroupInfo(
                key = readFunctionKeyMetaAnnotation(),
                type = GroupType.Root,
                line = -1
            )
        )
    } else {
        emptyList()
    }

context(keyCache: LambdaKeyCache)
private fun parseGroupInfoFromTokens(
    methodNode: MethodNode,
    tokens: List<BytecodeToken>,
    isComposable: Boolean
): Result<List<GroupInfo>> {
    val functionKey = methodNode.readFunctionKeyMetaAnnotation()
    val nodeStack = if (isComposable) {
        mutableListOf(
            GroupInfoBuilder(
                key = functionKey,
                type = GroupType.Root,
            )
        )
    } else {
        mutableListOf()
    }

    val result = mutableSetOf<GroupInfo>()
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
                nodeStack.add(newNode)
            }

            is EndGroupToken -> {
                if (currentNode != null && currentNode.incompleteLabels.isEmpty()) {
                    if (currentNode.type != token.type) {
                        return parseError("${token.type} is not allowed in ${currentNode.type} scope")
                    }

                    val node = nodeStack.removeLast()
                    result += node.build()
                }
            }

            is JumpToken -> {
                token.labels.forEach {
                    val label = it.label
                    val labelIndex = tokens.indexOfFirst { it is LabelToken && it.labelInsn.label == label }
                    if (labelIndex > i) {
                        // Only consider forward branches, backward branches are already processed
                        currentNode?.incompleteLabels += label
                    }
                }
            }

            is LabelToken -> {
                val label = token.labelInsn.label
                nodeStack.forEach {
                    it.incompleteLabels -= label
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

                for (i in (nodeStack.size - 1) downTo nodeIndex + 1) {
                    val node = nodeStack[i]
                    if (node.incompleteLabels.isEmpty()) {
                        nodeStack.removeAt(i)
                        result += node.build()
                    }
                }
            }

            is ThrowToken -> {
                var toRemove = nodeStack.lastIndex
                while (toRemove >= 0) {
                    val node = nodeStack[toRemove]
                    if (node.type == GroupType.Root || node.incompleteLabels.isNotEmpty()) {
                        break
                    }

                    nodeStack.removeAt(toRemove)
                    result += node.build()
                    toRemove--
                }
            }

            is ComposableLambdaToken -> {
                val clsFqName = token.handle.owner.replace("/", ".")
                val desc = if (token.isIndy) {
                    "${clsFqName}.${token.handle.name}${token.handle.desc}"
                } else {
                    clsFqName
                }
                keyCache[desc] = token.key
            }
        }
    }

    if (isComposable && nodeStack.size > 1) {
        return parseError("Expected optional root group, but found ${nodeStack.size}")
    }
    if (!isComposable && nodeStack.isNotEmpty()) {
        return parseError("Expected no groups in a non-composable function.")
    }

    val rootNode = nodeStack.firstOrNull()
    if (rootNode != null && result.none { it.key == rootNode.key }) {
        result.add(rootNode.build())
    }

    return Result.success(result.toList())
}

private fun MethodNode.isComposable() = when {
    hasComposableAnnotation() || hasFunctionKeyMetaAnnotation() -> true
    name == "invoke" && desc.contains("${ComposeIds.Composer.classId.descriptor}I") -> true
    else -> false
}

private fun <T> parseError(message: String): Result<T> = Result.failure(ParseException(message))
private class ParseException(message: String) : RuntimeException(message)