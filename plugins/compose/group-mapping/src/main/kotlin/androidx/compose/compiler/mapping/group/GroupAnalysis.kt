package androidx.compose.compiler.mapping.group

import androidx.compose.compiler.mapping.ClassId
import androidx.compose.compiler.mapping.ErrorReporter
import androidx.compose.compiler.mapping.MethodId
import androidx.compose.compiler.mapping.bytecode.BytecodeToken
import androidx.compose.compiler.mapping.bytecode.BytecodeToken.ComposableLambdaToken
import androidx.compose.compiler.mapping.bytecode.BytecodeToken.LineToken
import androidx.compose.compiler.mapping.bytecode.ComposeIds
import androidx.compose.compiler.mapping.bytecode.StartGroupToken
import androidx.compose.compiler.mapping.bytecode.tokenizeBytecode
import org.objectweb.asm.tree.MethodNode

internal fun analyzeGroups(
    keyCache: LambdaKeyCache,
    reporter: ErrorReporter,
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

    return parseGroupInfoFromTokens(keyCache, methodNode, tokens, isComposable).getOrElse { e ->
        reporter.reportError(IllegalStateException("Failed to parse $methodId", e))
        return methodNode.emptyGroup(isComposable)
    }
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

private fun parseGroupInfoFromTokens(
    keyCache: LambdaKeyCache,
    methodNode: MethodNode,
    tokens: List<BytecodeToken>,
    isComposable: Boolean
): Result<List<GroupInfo>> {
    val functionKey = methodNode.readFunctionKeyMetaAnnotation()
    val groups = mutableSetOf<GroupInfo>()
    val rootGroup = if (isComposable) {
        GroupInfo(
            key = functionKey,
            type = GroupType.Root,
            line = -1
        ).also {
            groups.add(it)
        }
    } else {
        null
    }

    var currentLine = -1
    for (tokenIndex in tokens.indices) {
        when (val token = tokens[tokenIndex]) {
            is LineToken -> {
                val isFirstLine = currentLine == -1
                currentLine = token.lineInsn.line

                if (isFirstLine) {
                    // Groups with missing lines are usually injected before the actual code.
                    // In most cases only root group info is missing.
                    groups.forEach {
                        if (it.line == -1) {
                            it.line = currentLine
                        }
                    }
                    rootGroup?.line = currentLine
                }
            }

            is StartGroupToken -> {
                if (rootGroup?.key == token.key) {
                    continue
                }
                val newNode = GroupInfo(key = token.key, type = token.type, line = currentLine)
                groups.add(newNode)
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

    if (!isComposable && groups.isNotEmpty()) {
        return parseError("Expected no groups in a non-composable function.")
    }

    if (rootGroup != null && groups.none { it.key == rootGroup.key }) {
        groups.add(rootGroup)
    }

    return Result.success(groups.sortedBy { it.line })
}

private fun MethodNode.isComposable() = when {
    hasComposableAnnotation() || hasFunctionKeyMetaAnnotation() -> true
    name == "invoke" && desc.contains("${ComposeIds.Composer.classId.descriptor}I") -> true
    else -> false
}

private fun <T> parseError(message: String): Result<T> = Result.failure(ParseException(message))
private class ParseException(message: String) : RuntimeException(message)