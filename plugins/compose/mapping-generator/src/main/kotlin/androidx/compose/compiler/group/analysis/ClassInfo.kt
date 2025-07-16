/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package androidx.compose.compiler.group.analysis

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.ClassNode

data class ClassInfo(
    val classId: ClassId,
    val fileName: String?,
    val methods: List<MethodInfo>
)

data class MethodInfo(
    val id: MethodId,
    val groups: List<GroupInfo>
)

fun ClassInfo(bytecode: ByteArray): ClassInfo {
    val reader = ClassReader(bytecode)
    val node = ClassNode(ASM9)
    reader.accept(node, 0)
    return ClassInfo(node)
}

internal fun ClassInfo(classNode: ClassNode): ClassInfo {
    val classId = ClassId(classNode)

    val methods = classNode.methods.mapNotNull { methodNode ->
        if (methodNode.methodType() != MethodType.Composable) {
            return@mapNotNull null
        }

        val groups = parseGroupInfo(methodNode).filter { it.key != null }

        if (groups.isNotEmpty()) {
            MethodInfo(
                MethodId(classNode, methodNode),
                groups
            )
        } else {
            null
        }
    }

    return ClassInfo(
        classId = classId,
        fileName = classNode.sourceFile,
        methods = methods,
    )
}