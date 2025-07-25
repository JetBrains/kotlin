/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.bytecode

import androidx.compose.compiler.mapping.MethodId
import androidx.compose.compiler.mapping.bytecode.BytecodeToken.*
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

internal sealed interface BytecodeTokenizer {
    fun nextToken(context: TokenizerContext): Result<BytecodeToken>?
}

internal data class TokenizerContext(
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
        return children.firstNotNullOfOrNull { tokenizer ->
            tokenizer.nextToken(context)
        }
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

private val GroupTokenizer by lazy {
    CompositeInstructionTokenizer(
        LineTokenizer,
        LabelTokenizer,
        JumpTokenizer,
        ThrowTokenizer,
        CurrentMarkerTokenizer,
        EndToMarkerTokenizer,
        StartRestartGroupTokenizer,
        EndRestartGroupTokenizer,
        StartReplaceGroupTokenizer,
        EndReplaceGroupTokenizer,
        ComposableLambdaTokenizer
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
    when (instruction) {
        is JumpInsnNode -> {
            JumpToken(instruction, listOf(instruction.label))
        }
        is LookupSwitchInsnNode -> {
            JumpToken(instruction, instruction.labels)
        }
        is TableSwitchInsnNode -> {
            JumpToken(instruction, instruction.labels)
        }
        else -> {
            null
        }
    }
}

private val ThrowTokenizer = SingleInstructionTokenizer { instruction ->
    if (instruction is InsnNode && instruction.opcode == Opcodes.ATHROW) {
        ThrowToken(instruction)
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
        val expectedMethodInsn = context[0] ?: return null

        if (expectedMethodInsn is MethodInsnNode) {
            val method = MethodId(expectedMethodInsn)
            if (method == ComposeIds.Composer.startReplaceGroup || method == ComposeIds.Composer.startReplaceableGroup) {
                val expectedLdc = when (context[-1]) {
                    // Compose compiler sometimes generates the call on a different line from LDC instruction
                    // This usually happens inside the if / else body (b/436584119)
                    is LineNumberNode -> {
                        if (context[-2] is LabelNode) context[-3] else context[-2]
                    }
                    else -> context[-1]
                }
                val keyValue = expectedLdc?.intValueOrNull()
                    ?: return tokenizerError("Failed parsing startReplaceGroup token: expected key value, got ${expectedLdc?.opcode}")

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

        val jump = context[2]?.takeIf { it.opcode == Opcodes.GOTO }

        return Result.success(
            EndToMarkerToken(
                variableIndex = expectedILoadInsn.`var`,
                instructions = listOfNotNull(expectedILoadInsn, expectedEndToMarkerInvocation, jump)
            )
        )
    }
}

private object ComposableLambdaTokenizer : BytecodeTokenizer {
    fun MethodId.toComposableLambda(): ComposeIds.ComposableLambda? =
        ComposeIds.ComposableLambda.entries.firstOrNull {
            it.methodId == this
        }

    override fun nextToken(context: TokenizerContext): Result<BytecodeToken>? {
        val composableLambdaInvoke = context[0]
        if (composableLambdaInvoke !is MethodInsnNode) return null
        val methodId = MethodId(composableLambdaInvoke)
        val composableLambda = methodId.toComposableLambda() ?: return null

        val lambdaInsn = context[-composableLambda.lambdaOffset]
        when (lambdaInsn) {
            is InvokeDynamicInsnNode -> {
                val desc = JvmDescriptor.fromString(lambdaInsn.desc)
                val capturesCount = captureInsnCount(context, -(composableLambda.lambdaOffset + 1), desc.parameters.size).getOrElse {
                    return Result.failure(it)
                }

                val keyOffset = composableLambda.lambdaOffset + capturesCount + composableLambda.keyOffset
                val keyLdcInsn = context[-keyOffset]
                val keyValue = keyLdcInsn?.intValueOrNull() ?: return tokenizerError(
                    "Failed parsing $composableLambda: expected key value"
                )

                val handle = lambdaInsn.bsmArgs.getOrNull(1) as? Handle ?: return null

                return Result.success(
                    ComposableLambdaToken(
                        key = keyValue,
                        handle = handle,
                        isIndy = true,
                        instructions = listOf(composableLambdaInvoke)
                    )
                )
            }
            is MethodInsnNode -> {
                if (lambdaInsn.opcode != Opcodes.INVOKESPECIAL) return tokenizerError(
                    "Failed parsing $composableLambda: expected INVOKESPECIAL for lambda call, got ${lambdaInsn.opcode}"
                )

                val desc = JvmDescriptor.fromString(lambdaInsn.desc)
                val capturesCount = captureInsnCount(context, -(composableLambda.lambdaOffset + 1), desc.parameters.size).getOrElse {
                    return Result.failure(it)
                }

                val keyOffset = composableLambda.lambdaOffset + capturesCount + /* new + dup */ 2 + composableLambda.keyOffset
                val keyLdcInsn = context[-keyOffset]
                val keyValue = keyLdcInsn?.intValueOrNull() ?: return tokenizerError(
                    "Failed parsing $composableLambda: expected key value"
                )

                val handle = Handle(
                    Opcodes.H_INVOKESPECIAL,
                    lambdaInsn.owner,
                    lambdaInsn.name,
                    lambdaInsn.desc,
                    lambdaInsn.itf
                )

                return Result.success(
                    ComposableLambdaToken(
                        key = keyValue,
                        handle = handle,
                        isIndy = false,
                        instructions = listOf(composableLambdaInvoke)
                    )
                )
            }
            is FieldInsnNode -> {
                if (lambdaInsn.opcode != Opcodes.GETSTATIC) {
                    return tokenizerError("Failed parsing $composableLambda: expected GETSTATIC, but got ${lambdaInsn.opcode}")
                }

                val keyOffset = composableLambda.lambdaOffset + composableLambda.keyOffset
                val keyLdcInsn = context[-keyOffset]
                val keyValue = keyLdcInsn?.intValueOrNull() ?: return tokenizerError(
                    "Failed parsing $composableLambda: expected key value"
                )

                val handle = Handle(
                    Opcodes.H_GETSTATIC,
                    lambdaInsn.owner,
                    lambdaInsn.name,
                    lambdaInsn.desc,
                    false
                )

                return Result.success(
                    ComposableLambdaToken(
                        key = keyValue,
                        handle = handle,
                        isIndy = false,
                        instructions = listOf(composableLambdaInvoke)
                    )
                )
            }
            else -> {
                return tokenizerError("Failed parsing $composableLambda: unexpected opcode ${lambdaInsn?.opcode}.")
            }
        }
    }

    private fun captureInsnCount(context: TokenizerContext, capturesOffset: Int, captureCount: Int): Result<Int> {
        var offset = capturesOffset
        repeat(captureCount) {
            when (val insn = context[offset]) {
                is FieldInsnNode -> offset -= 2
                is VarInsnNode -> offset -= 1
                else -> {
                    return tokenizerError("Unexpected ${insn?.opcode}")
                }
            }
        }
        return Result.success(-(offset - capturesOffset))
    }
}

internal fun tokenizeBytecode(
    instructions: InsnList,
    isComposable: Boolean
): Result<List<BytecodeToken>> {
    val context = TokenizerContext(instructions)
    val tokens = mutableListOf<BytecodeToken>()

    val tokenizer = if (isComposable) GroupTokenizer else ComposableLambdaTokenizer
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