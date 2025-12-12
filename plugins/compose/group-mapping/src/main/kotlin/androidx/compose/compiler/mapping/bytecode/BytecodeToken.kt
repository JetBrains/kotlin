/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.bytecode

import androidx.compose.compiler.mapping.group.GroupType
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LineNumberNode

internal sealed interface StartGroupToken : BytecodeToken {
    val key: Int
    val type: GroupType
}

internal sealed interface BytecodeToken {
    val instructions: List<AbstractInsnNode>

    class StartRestartGroup(
        override val key: Int,
        override val instructions: List<AbstractInsnNode>
    ) : StartGroupToken {
        override val type: GroupType = GroupType.RestartGroup

        override fun toString(): String = "StartRestartGroup(key=$key)"
    }

    class StartReplaceGroup(
        override val key: Int,
        override val instructions: List<AbstractInsnNode>
    ) : StartGroupToken {
        override val type: GroupType = GroupType.ReplaceGroup

        override fun toString(): String = "StartReplaceGroup(key=$key)"
    }

    class LineToken(
        val lineInsn: LineNumberNode
    ) : BytecodeToken {
        override val instructions: List<AbstractInsnNode> = listOf(lineInsn)
        override fun toString(): String = "LineToken(line=${lineInsn.line})"
    }

    class ComposableLambdaToken(
        val key: Int,
        val handle: Handle,
        val isIndy: Boolean,
        override val instructions: List<AbstractInsnNode>
    ) : BytecodeToken {
        override fun toString(): String = "ComposableLambdaToken(key=$key, handle=$handle)"
    }
}