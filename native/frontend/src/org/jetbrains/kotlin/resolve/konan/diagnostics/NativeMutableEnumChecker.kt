/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField

object NativeMutableEnumChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is PropertyDescriptor ) return
        (descriptor.containingDeclaration as? ClassDescriptor)?.let {
            if (it.kind != ClassKind.ENUM_CLASS && it.kind != ClassKind.ENUM_ENTRY) return
            val isDelegated = (declaration as? KtProperty)?.delegate != null
            val hasBackingField = descriptor.setter?.isDefault ?: true && descriptor.hasBackingField(context.trace.bindingContext)
            if (descriptor.isVar && !isDelegated && hasBackingField) {
                context.trace.report(ErrorsNative.MUTABLE_ENUM.on(declaration))
            }
        }
    }
}