/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.source.getPsi

object NativeUselessSharedImmutableChecker : DeclarationChecker {
    private val sharedImmutable = FqName("kotlin.native.concurrent.SharedImmutable")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val sharedImmutableAnnotation = descriptor.annotations.findAnnotation(sharedImmutable) ?: return
        val location = sharedImmutableAnnotation.source.getPsi() as? KtElement ?: declaration
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            context.trace.report(ErrorsNative.USELESS_SHARED_IMMUTABLE.on(location))
            return
        }
        val isVariable = (descriptor as? VariableDescriptor)?.isVar ?: false
        val isDelegated = (declaration as? KtProperty)?.delegate != null
        if (!isVariable || isDelegated) return
        context.trace.report(ErrorsNative.USELESS_SHARED_IMMUTABLE.on(location))
    }
}