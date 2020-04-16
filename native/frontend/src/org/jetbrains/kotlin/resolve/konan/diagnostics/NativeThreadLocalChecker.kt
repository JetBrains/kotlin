/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics


import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField

internal fun DeclarationChecker.check(
    annotationFqName: FqName, declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext,
    error: DiagnosticFactory0<KtElement>, successCondition: (DeclarationDescriptor) -> Boolean
) {
    if (successCondition(descriptor)) return
    descriptor.annotations.findAnnotation(annotationFqName)?.let {
        val reportLocation = DescriptorToSourceUtils.getSourceFromAnnotation(it) ?: declaration
        context.trace.report(error.on(reportLocation))
    }
}

object NativeThreadLocalChecker : DeclarationChecker {
    private val threadLocalFqName = FqName("kotlin.native.concurrent.ThreadLocal")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        check(threadLocalFqName, declaration, descriptor, context, ErrorsNative.INAPPLICABLE_THREAD_LOCAL) {
            val isVariable = descriptor is VariableDescriptor
            val hasBackingField = descriptor is PropertyDescriptor && descriptor.hasBackingField(context.trace.bindingContext)
            val hasDelegate = declaration is KtProperty && declaration.delegate != null
            (isVariable && (hasBackingField || hasDelegate)) ||
                    (descriptor is ClassDescriptor && descriptor.kind == ClassKind.OBJECT)
        }
        check(threadLocalFqName, declaration, descriptor, context, ErrorsNative.INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL) {
            DescriptorUtils.isTopLevelDeclaration(descriptor) || descriptor is ClassDescriptor && descriptor.kind == ClassKind.OBJECT
        }
    }
}
