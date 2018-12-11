/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi.asm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.*

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
        // if both descriptor's and access's visibilities are private, we can generate an empty method
        // 1. we need to check a descriptor, because inline reified functions
        //    might have a non-private visibility in ABI, but they are private in bytecode
        // 2. we need to check an access, because synthetic methods
        //    for default parameters have private visibility, but public in bytecode
        val descriptor = origin.descriptor as? MemberDescriptor
        if (isPrivate(access) && descriptor != null && isPrivate(descriptor) || isClinit(name, access)) return EMPTY_METHOD_VISITOR

        val mv = super.newMethod(origin, access, name, desc, signature, exceptions)
        // inline function bodies are part of ABI,
        // but non-inline functions can be thrown out
        if (descriptor is FunctionDescriptor && descriptor.isInline) return mv

        return ReplaceWithEmptyMethodVisitor(
            delegate = mv,
            access = access,
            name = name,
            desc = desc,
            signature = signature,
            exceptions = exceptions
        )
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

    private fun isPrivate(descriptor: MemberDescriptor): Boolean =
        descriptor.visibility == Visibilities.PRIVATE

    private fun isPrivate(access: Int): Boolean =
        (access and Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE

    private fun isClinit(name: String, access: Int): Boolean =
        name == "<clinit>" && (access and Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC
}