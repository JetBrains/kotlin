/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package androidx.compose.compiler.mapping

import androidx.compose.compiler.mapping.group.GroupInfo
import androidx.compose.compiler.mapping.group.LambdaKeyCache
import androidx.compose.compiler.mapping.group.analyzeGroups
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class ClassInfo(
    val classId: ClassId,
    val fileName: String?,
    val methods: List<MethodInfo>
)

class MethodInfo(
    val id: MethodId,
    val groups: List<GroupInfo>
)

data class ClassId(val value: String) {
    internal constructor(node: ClassNode) : this(node.name)

    val descriptor: String get() = "L$value;"

    val fqName: String get() = value.replace("/", ".")
}

data class MethodId(
    val classId: ClassId,
    val methodName: String,
    val methodDescriptor: String,
) {
    override fun toString(): String =
        "${classId.fqName}.$methodName$methodDescriptor"
}

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

context(reporter: ErrorReporter, keyCache: LambdaKeyCache)
fun ClassInfo(bytecode: ByteArray): ClassInfo {
    val reader = ClassReader(bytecode)
    val node = ClassNode(ASM9)
    reader.accept(node, 0)
    return buildClassInfo(node)
}

context(reporter: ErrorReporter, keyCache: LambdaKeyCache)
private fun buildClassInfo(classNode: ClassNode): ClassInfo {
    val classId = ClassId(classNode)

    val methods = classNode.methods.mapNotNull { methodNode ->
        if (classId in InternalClasses) {
            // Ignore groups in the internal lambda class
            return@mapNotNull null
        }

        val groups = analyzeGroups(classId, methodNode)

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

private val InternalClasses = setOf(
    ClassId("androidx/compose/runtime/internal/ComposableLambdaImpl")
)