/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.group.analysis

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

@JvmInline
value class ClassId(val value: String) {
    internal constructor(node: ClassNode) : this(node.name)

    val descriptor: String get() = "L$value;"

    val fqName: String get() = value.replace("/", ".")

    companion object {
        fun fromDesc(desc: String): ClassId =
            ClassId(desc.removePrefix("L").removeSuffix(";"))
    }
}

@JvmInline
value class ComposeGroupKey(val key: Int)

data class MethodId(
    val classId: ClassId,
    val methodName: String,
    val methodDescriptor: String,
)

internal fun MethodId(classNode: ClassNode, methodNode: MethodNode): MethodId = MethodId(
    classId = ClassId(classNode),
    methodName = methodNode.name,
    methodDescriptor = methodNode.desc,
)

internal fun MethodId(methodInsnNode: MethodInsnNode): MethodId = MethodId(
    classId = ClassId(methodInsnNode.owner),
    methodName = methodInsnNode.name,
    methodDescriptor = methodInsnNode.desc
)