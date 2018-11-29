/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi.asm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

internal class AbiClassBuilder(private val cv: ClassVisitor) : AbstractClassBuilder() {
    override fun getVisitor(): ClassVisitor = cv

    override fun newMethod(
        origin: JvmDeclarationOrigin,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (isPrivate(access)) return EMPTY_METHOD_VISITOR

        return super.newMethod(origin, access, name, desc, signature, exceptions)
    }

    override fun newField(
        origin: JvmDeclarationOrigin,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        if (isPrivate(access)) return EMPTY_FIELD_VISITOR

        return super.newField(origin, access, name, desc, signature, value)
    }

    override fun defineClass(
        origin: PsiElement?,
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<out String>
    ) {
        if (isPrivate(access)) return

        super.defineClass(origin, version, access, name, signature, superName, interfaces)
    }

    private fun isPrivate(access: Int): Boolean =
        (access and Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE
}