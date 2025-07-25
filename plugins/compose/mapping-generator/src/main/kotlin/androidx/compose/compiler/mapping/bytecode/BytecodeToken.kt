/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.bytecode

import androidx.compose.compiler.mapping.group.GroupType
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode

internal sealed interface StartGroupToken : BytecodeToken {
    val key: Int
    val type: GroupType
}

internal sealed interface EndGroupToken : BytecodeToken {
    val type: GroupType
}

internal sealed interface BytecodeToken {
    val instructions: List<AbstractInsnNode>

    class StartRestartGroup(
        override val key: Int,
        override val instructions: List<AbstractInsnNode>
    ) : StartGroupToken {
        override val type: GroupType = GroupType.RestartGroup

        override fun toString(): String {
            return "StartRestartGroup(key=$key)"
        }
    }

    class EndRestartGroup(
        override val instructions: List<AbstractInsnNode>
    ) : EndGroupToken {
        override val type: GroupType = GroupType.RestartGroup

        override fun toString(): String {
            return "EndRestartGroup()"
        }
    }

    class StartReplaceGroup(
        override val key: Int,
        override val instructions: List<AbstractInsnNode>
    ) : StartGroupToken {
        override val type: GroupType = GroupType.ReplaceGroup

        override fun toString(): String {
            return "StartReplaceGroup(key=$key)"
        }
    }

    class EndReplaceGroup(
        override val instructions: List<AbstractInsnNode>
    ) : EndGroupToken {
        override val type: GroupType = GroupType.ReplaceGroup

        override fun toString(): String {
            return "EndReplaceGroup()"
        }
    }


    class LineToken(
        val lineInsn: LineNumberNode
    ) : BytecodeToken {
        override val instructions: List<AbstractInsnNode> = listOf(lineInsn)
        override fun toString(): String = "LineToken(line=${lineInsn.line})"
    }

    class LabelToken(
        val labelInsn: LabelNode
    ) : BytecodeToken {
        override val instructions: List<AbstractInsnNode> = listOf(labelInsn)
        override fun toString(): String = "LabelToken(label=${labelInsn.label})"
    }

    class JumpToken(
        val jumpInsn: JumpInsnNode
    ) : BytecodeToken {
        override val instructions: List<AbstractInsnNode> = listOf(jumpInsn)
        override fun toString(): String = "JumpToken(jump=${jumpInsn.opcode})"
    }

    class CurrentMarkerToken(
        val variableIndex: Int,
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken {
        override fun toString(): String {
            return "CurrentMarkerToken(index=$variableIndex)"
        }
    }

    class EndToMarkerToken(
        var variableIndex: Int,
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken {
        override fun toString(): String {
            return "EndToMarkerToken(index=$variableIndex)"
        }
    }
}