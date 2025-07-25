/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.bytecode

import androidx.compose.compiler.mapping.MethodId
import androidx.compose.compiler.mapping.bytecode.BytecodeToken.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

private sealed interface BytecodeTokenizer {
    fun nextToken(context: TokenizerContext): Result<BytecodeToken>?
}

private data class TokenizerContext(
    val instructions: InsnList,
    var index: Int = 0,
) {
    fun advance(count: Int = 1): Boolean {
        index += count
        return index < instructions.size()
    }

    operator fun get(i: Int): AbstractInsnNode? =
        try {
            instructions.get(index + i)
        } catch (_: IndexOutOfBoundsException) {
            null
        }
}


private class CompositeInstructionTokenizer(
    val children: List<BytecodeTokenizer>
) : BytecodeTokenizer {
    constructor(vararg children: BytecodeTokenizer) : this(children.toList())

    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        return children.firstNotNullOfOrNull { tokenizer -> tokenizer.nextToken(context) }
    }
}

private class SingleInstructionTokenizer(
    private val token: (instruction: AbstractInsnNode) -> BytecodeToken?
) : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val instruction = context[0] ?: return null
        return Result.success(token(instruction) ?: return null)
    }
}

private val tokenizer by lazy {
    CompositeInstructionTokenizer(
        LineTokenizer,
        LabelTokenizer,
        JumpTokenizer,
        CurrentMarkerTokenizer,
        EndToMarkerTokenizer,
        StartRestartGroupTokenizer,
        EndRestartGroupTokenizer,
        StartReplaceGroupTokenizer,
        EndReplaceGroupTokenizer,
    )
}

private val LineTokenizer = SingleInstructionTokenizer { instruction ->
    if (instruction is LineNumberNode) {
        LineToken(instruction)
    } else {
        null
    }
}

private val LabelTokenizer = SingleInstructionTokenizer { instruction ->
    if (instruction is LabelNode) {
        LabelToken(instruction)
    } else {
        null
    }
}

private val JumpTokenizer = SingleInstructionTokenizer { instruction ->
    if (instruction is JumpInsnNode) {
        JumpToken(instruction)
    } else {
        null
    }
}

private object StartRestartGroupTokenizer : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedLdc = context[0] ?: return null
        val expectedMethodInsn = context[1] ?: return null

        if (expectedMethodInsn is MethodInsnNode &&
            MethodId(expectedMethodInsn) == ComposeIds.Composer.startRestartGroup
        ) {
            val keyValue = expectedLdc.intValueOrNull()
                ?: return tokenizerError("Failed parsing startRestartGroup token: expected key value")

            return Result.success(
                StartRestartGroup(keyValue, listOf(expectedLdc, expectedMethodInsn))
            )

        }

        return null
    }
}

private object EndRestartGroupTokenizer : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedMethodIns = context[0] as? MethodInsnNode ?: return null
        if (MethodId(expectedMethodIns) == ComposeIds.Composer.endRestartGroup) {
            return Result.success(EndRestartGroup(listOf(expectedMethodIns)))
        }

        return null
    }
}

private object StartReplaceGroupTokenizer : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedLdc = context[0] ?: return null
        val expectedMethodInsn = context[1] ?: return null

        if (expectedMethodInsn is MethodInsnNode) {
            val method = MethodId(expectedMethodInsn)
            if (method == ComposeIds.Composer.startReplaceGroup || method == ComposeIds.Composer.startReplaceableGroup) {
                val keyValue = expectedLdc.intValueOrNull()
                    ?: return tokenizerError("Failed parsing startReplaceGroup token: expected key value")

                return Result.success(
                    StartReplaceGroup(keyValue, listOf(expectedLdc, expectedMethodInsn))
                )

            }
        }

        return null
    }
}

private object EndReplaceGroupTokenizer : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedMethodIns = context[0] as? MethodInsnNode ?: return null
        val method = MethodId(expectedMethodIns)
        if (method == ComposeIds.Composer.endReplaceGroup || method == ComposeIds.Composer.endReplaceableGroup) {
            return Result.success(EndReplaceGroup(listOf(expectedMethodIns)))
        }

        return null
    }
}

private object CurrentMarkerTokenizer : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedGetCurrentMarkerInvocation = context[0] ?: return null
        val expectedIStoreInsn = context[1] ?: return null
        if (expectedGetCurrentMarkerInvocation !is MethodInsnNode) return null
        if (MethodId(expectedGetCurrentMarkerInvocation) != ComposeIds.Composer.currentMarker) return null
        if (expectedIStoreInsn !is VarInsnNode) return null
        if (expectedIStoreInsn.opcode != Opcodes.ISTORE) return null
        return Result.success(
            CurrentMarkerToken(
                variableIndex = expectedIStoreInsn.`var`,
                instructions = listOf(expectedGetCurrentMarkerInvocation, expectedIStoreInsn)
            )
        )
    }
}

private object EndToMarkerTokenizer : BytecodeTokenizer {
    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val expectedILoadInsn = context[0] ?: return null
        val expectedEndToMarkerInvocation = context[1] ?: return null

        if (expectedILoadInsn !is VarInsnNode) return null
        if (expectedEndToMarkerInvocation !is MethodInsnNode) return null
        if (expectedILoadInsn.opcode != Opcodes.ILOAD) return null
        if (MethodId(expectedEndToMarkerInvocation) != ComposeIds.Composer.endToMarker) return null

        return Result.success(
            EndToMarkerToken(
                variableIndex = expectedILoadInsn.`var`,
                instructions = listOf(expectedILoadInsn, expectedEndToMarkerInvocation)
            )
        )
    }
}

internal fun tokenizeBytecode(
    instructions: InsnList
): Result<List<BytecodeToken>> {
    val context = TokenizerContext(instructions)
    val tokens = mutableListOf<BytecodeToken>()

    val tokenizer = tokenizer
    while (true) {
        val nextResult = tokenizer.nextToken(context)
        val count = if (nextResult != null) {
            val nextToken = nextResult.getOrElse { return Result.failure(it) }
            tokens.add(nextToken)
            nextToken.instructions.size
        } else {
            1 // Advance one instruction
        }
        if (!context.advance(count)) break
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

private fun <T> tokenizerError(message: String) = Result.failure<T>(TokenizerError(message))
private class TokenizerError(message: String) : RuntimeException(message)