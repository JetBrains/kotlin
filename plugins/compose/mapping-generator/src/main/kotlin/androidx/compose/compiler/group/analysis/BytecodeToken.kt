/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

import androidx.compose.compiler.group.analysis.BytecodeToken.*
import androidx.compose.compiler.group.analysis.BytecodeTokenizer.TokenizerContext
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*


internal sealed class BytecodeToken {
    abstract val instructions: List<AbstractInsnNode>

    class StartRestartGroup(
        val key: ComposeGroupKey,
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken() {
        override fun toString(): String {
            return "StartRestartGroup(key=$key)"
        }
    }

    class EndRestartGroup(
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken() {
        override fun toString(): String {
            return "EndRestartGroup()"
        }
    }

    class StartReplaceGroup(
        val key: ComposeGroupKey,
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken() {
        override fun toString(): String {
            return "StartReplaceGroup(key=$key)"
        }
    }

    class EndReplaceGroup(
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken() {
        override fun toString(): String {
            return "EndReplaceGroup()"
        }
    }


    class LineToken(
        val lineInsn: LineNumberNode
    ) : BytecodeToken() {
        override val instructions: List<AbstractInsnNode> = listOf(lineInsn)
        override fun toString(): String = "LineToken(line=${lineInsn.line})"
    }


    class BlockToken(
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken() {
        override fun toString(): String {
            return "BlockToken(${instructions.size})"
        }
    }
}

internal sealed class BytecodeTokenizer {
    data class TokenizerContext(
        val instructions: InsnList,
        val index: Int = 0,
    ) {
        fun skip(count: Int = 1): TokenizerContext? {
            val newIndex = index + count
            if (newIndex >= instructions.size()) return null
            return TokenizerContext(instructions, newIndex)
        }

        operator fun get(i: Int): AbstractInsnNode? =
            try {
                instructions.get(index + i)
            } catch (_: IndexOutOfBoundsException) {
                null
            }
    }

    abstract fun nextToken(context: TokenizerContext): Result<BytecodeToken>?
}


private class CompositeInstructionTokenizer(
    val children: List<BytecodeTokenizer>
) : BytecodeTokenizer() {
    constructor(vararg children: BytecodeTokenizer) : this(children.toList())

    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        return children.firstNotNullOfOrNull { tokenizer -> tokenizer.nextToken(context) }
    }
}

private class SingleInstructionTokenizer(
    private val token: (instruction: AbstractInsnNode) -> BytecodeToken?
) : BytecodeTokenizer() {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val instruction = context[0] ?: return null
        return Result.success(token(instruction) ?: return null)
    }
}

private val priorityTokenizer by lazy {
    CompositeInstructionTokenizer(
        LineTokenizer,
        StartRestartGroupTokenizer,
        EndRestartGroupTokenizer,
        StartReplaceGroupTokenizer,
        EndReplaceGroupTokenizer,
    )
}

private val tokenizer by lazy {
    CompositeInstructionTokenizer(
        priorityTokenizer,
        BlockTokenizer
    )
}

private val LineTokenizer = SingleInstructionTokenizer { instruction ->
    if (instruction is LineNumberNode) {
        LineToken(instruction)
    } else null
}

private object StartRestartGroupTokenizer : BytecodeTokenizer() {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedLdc = context[0] ?: return null
        val expectedMethodInsn = context[1] ?: return null

        if (expectedMethodInsn is MethodInsnNode &&
            MethodId(expectedMethodInsn) == ComposeIds.Composer.startRestartGroup
        ) {
            val keyValue = expectedLdc.intValueOrNull() ?: return Result.failure(
                IllegalStateException("Failed parsing startRestartGroup token: expected key value")
            )

            return Result.success(StartRestartGroup(ComposeGroupKey(keyValue), listOf(expectedLdc, expectedMethodInsn)))

        }

        return null
    }
}

private object EndRestartGroupTokenizer : BytecodeTokenizer() {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedMethodIns = context[0] as? MethodInsnNode ?: return null
        if (MethodId(expectedMethodIns) == ComposeIds.Composer.endRestartGroup) {
            return Result.success(EndRestartGroup(listOf(expectedMethodIns)))
        }

        return null
    }
}

private object StartReplaceGroupTokenizer : BytecodeTokenizer() {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedLdc = context[0] ?: return null
        val expectedMethodInsn = context[1] ?: return null

        if (expectedMethodInsn is MethodInsnNode) {
            val method = MethodId(expectedMethodInsn)
            if (method == ComposeIds.Composer.startReplaceGroup || method == ComposeIds.Composer.startReplaceableGroup) {
                val keyValue = expectedLdc.intValueOrNull() ?: return Result.failure(
                    IllegalStateException("Failed parsing startReplaceGroup token: expected key value")
                )

                return Result.success(
                    StartReplaceGroup(ComposeGroupKey(keyValue), listOf(expectedLdc, expectedMethodInsn))
                )

            }
        }

        return null
    }
}

private object EndReplaceGroupTokenizer : BytecodeTokenizer() {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedMethodIns = context[0] as? MethodInsnNode ?: return null
        val method = MethodId(expectedMethodIns)
        if (method == ComposeIds.Composer.endReplaceGroup || method == ComposeIds.Composer.endReplaceableGroup) {
            return Result.success(EndReplaceGroup(listOf(expectedMethodIns)))
        }

        return null
    }
}

private object BlockTokenizer : BytecodeTokenizer() {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val instructions = mutableListOf<AbstractInsnNode>()
        var currentContext = context
        while (true) {
            if (priorityTokenizer.nextToken(currentContext) != null) break
            instructions.add(currentContext[0] ?: break)
            currentContext = currentContext.skip(1) ?: break
        }
        if (instructions.isEmpty()) return null
        return Result.success(BlockToken(instructions))
    }
}

internal fun tokenizeBytecode(
    instructions: InsnList
): Result<List<BytecodeToken>> {
    if (instructions.size() == 0) return Failure("Empty list of instructions")

    var context = TokenizerContext(instructions)
    val tokens = mutableListOf<BytecodeToken>()

    while (true) {
        val nextResult = tokenizer.nextToken(context) ?: return Failure("Cannot build next token")
        val nextToken = nextResult.getOrElse { return Result.failure(it) }
        tokens.add(nextToken)
        context = context.skip(nextToken.instructions.size) ?: break
    }

    return Result.success(tokens)
}

private fun AbstractInsnNode.intValueOrNull(): Int? {
    if (this is LdcInsnNode) return this.cst as? Int
    return when (opcode) {
        Opcodes.ICONST_0 -> 0
        Opcodes.ICONST_1 -> 1
        Opcodes.ICONST_2 -> 2
        Opcodes.ICONST_3 -> 3
        Opcodes.ICONST_4 -> 4
        Opcodes.ICONST_5 -> 5
        Opcodes.ICONST_M1 -> -1
        Opcodes.BIPUSH -> (this as IntInsnNode).operand
        Opcodes.SIPUSH -> (this as IntInsnNode).operand
        else -> null
    }
}