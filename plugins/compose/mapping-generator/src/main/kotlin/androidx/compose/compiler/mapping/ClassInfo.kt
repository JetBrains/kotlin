/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package androidx.compose.compiler.mapping

import androidx.compose.compiler.mapping.group.GroupInfo
import androidx.compose.compiler.mapping.group.analyzeGroups
import androidx.compose.compiler.mapping.group.hasComposableAnnotation
import androidx.compose.compiler.mapping.group.hasFunctionKeyMetaAnnotation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

data class ClassInfo(
    val classId: ClassId,
    val fileName: String?,
    val methods: List<MethodInfo>
)

data class MethodInfo(
    val id: MethodId,
    val groups: List<GroupInfo>
)

context(reporter: ComposeMappingErrorReporter)
fun ClassInfo(bytecode: ByteArray): ClassInfo {
    val reader = ClassReader(bytecode)
    val node = ClassNode(ASM9)
    reader.accept(node, 0)
    return ClassInfo(node)
}

context(reporter: ComposeMappingErrorReporter)
private fun ClassInfo(classNode: ClassNode): ClassInfo {
    val classId = ClassId(classNode)

    val methods = classNode.methods.mapNotNull { methodNode ->
        if (!methodNode.isComposable()) {
            // TODO: visit all fields and methods to process `composableLambdaInstance` calls.
            return@mapNotNull null
        }

        if (classId in InternalClasses) {
            // Ignore groups in the internal lambda class
            return@mapNotNull null
        }

        val groups = analyzeGroups(classId, methodNode).filter { it.key != null }

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

private fun MethodNode.isComposable() = when {
    hasComposableAnnotation() || hasFunctionKeyMetaAnnotation() -> true
    name == "invoke" && desc.contains("${ComposeIds.Composer.classId.descriptor}I") -> true
    else -> false
}