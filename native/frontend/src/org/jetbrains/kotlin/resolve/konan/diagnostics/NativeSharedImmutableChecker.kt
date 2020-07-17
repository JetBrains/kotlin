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
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField

object NativeSharedImmutableChecker : DeclarationChecker {
    private val sharedImmutableFqName = FqName("kotlin.native.concurrent.SharedImmutable")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        check(sharedImmutableFqName, declaration, descriptor, context, ErrorsNative.INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY) {
            val isVariable = descriptor is VariableDescriptor && descriptor.isVar
            val hasBackingField = descriptor is PropertyDescriptor && descriptor.hasBackingField(context.trace.bindingContext)
            val hasDelegate = declaration is KtProperty && declaration.delegate != null
            !isVariable && hasBackingField || hasDelegate
        }
        check(sharedImmutableFqName, declaration, descriptor, context, ErrorsNative.INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL) {
            DescriptorUtils.isTopLevelDeclaration(descriptor)
        }
    }
}
