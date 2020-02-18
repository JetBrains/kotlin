/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics


import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.hasBackingField

object NativeTopLevelSingletonChecker : DeclarationChecker {
    private val threadLocalFqName = FqName("kotlin.native.concurrent.ThreadLocal")

    private val DeclarationDescriptor.isInsideTopLevelSingletonWithoutThreadLocal: Boolean
        get() {
            val parent = containingDeclaration as? ClassDescriptor
            return parent != null && parent.kind.isSingleton &&
                    parent.annotations.findAnnotation(threadLocalFqName) == null
        }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtProperty || descriptor !is PropertyDescriptor ||
            !descriptor.isInsideTopLevelSingletonWithoutThreadLocal
        ) return
        if (descriptor.isVar && declaration.delegate == null && descriptor.hasBackingField(context.trace.bindingContext) &&
            descriptor.setter?.isDefault == true
        ) {
            context.trace.report(ErrorsNative.VARIABLE_IN_TOP_LEVEL_SINGLETON_WITHOUT_THERAD_LOCAL.on(declaration))
        }
    }
}
