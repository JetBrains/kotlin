/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.source.getPsi

object NativeUselessThreadLocalChecker: DeclarationChecker {
    private val threadLocal = FqName("kotlin.native.concurrent.ThreadLocal")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if ((descriptor as? ClassDescriptor)?.kind == ClassKind.OBJECT) return
        val threadLocalAnnotation = descriptor.annotations.findAnnotation(threadLocal) ?: return
        val location = threadLocalAnnotation.source.getPsi() as? KtElement ?: declaration
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            context.trace.report(ErrorsNative.USELESS_THREAD_LOCAL.on(location))
            return
        }
        val isVariable = (descriptor as? VariableDescriptor)?.isVar ?: false
        val isDelegated = (declaration as? KtProperty)?.delegate != null
        if (!isVariable || isDelegated) return
        context.trace.report(ErrorsNative.USELESS_THREAD_LOCAL.on(location))
    }
}