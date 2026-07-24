/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.LocalVariableNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class OriginCollectingClassBuilderFactory(private val builderMode: ClassBuilderMode) : ClassBuilderFactory {
    val compiledClasses = mutableListOf<ClassNode>()
    val origins = mutableMapOf<Any, KaptIrOrigin>()

    override fun getClassBuilderMode(): ClassBuilderMode = builderMode

    override fun newClassBuilder(origin: IrClass?): AbstractClassBuilder.Concrete {
        val classNode = ClassNode()
        compiledClasses += classNode
        saveOrigin(classNode, origin)
        return OriginCollectingClassBuilder(classNode)
    }

    private inner class OriginCollectingClassBuilder(val classNode: ClassNode) : AbstractClassBuilder.Concrete(classNode) {
        override fun newField(
            origin: IrField?,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            val fieldNode = super.newField(origin, access, name, desc, signature, value) as FieldNode
            saveOrigin(fieldNode, origin)
            return fieldNode
        }

        override fun newMethod(
            origin: IrFunction?,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val methodNode = super.newMethod(origin, access, name, desc, signature, exceptions) as MethodNode
            saveOrigin(methodNode, origin)

            // ASM doesn't read information about local variables for the `abstract` methods so we need to get it manually
            if ((access and Opcodes.ACC_ABSTRACT) != 0 && methodNode.localVariables == null) {
                methodNode.localVariables = mutableListOf<LocalVariableNode>()
            }

            return methodNode
        }
    }

    private fun saveOrigin(node: Any, origin: IrDeclaration?) {
        if (origin != null) {
            origins[node] = KaptIrOrigin(origin)
        }
    }

    override fun asBytes(builder: ClassBuilder): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        (builder as OriginCollectingClassBuilder).classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    override fun asText(builder: ClassBuilder) = throw UnsupportedOperationException()
}
