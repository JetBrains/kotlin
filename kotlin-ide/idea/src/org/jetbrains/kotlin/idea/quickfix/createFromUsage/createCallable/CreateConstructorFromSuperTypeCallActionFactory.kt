/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ConstructorInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.Variance

object CreateConstructorFromSuperTypeCallActionFactory : CreateCallableMemberFromUsageFactory<KtSuperTypeCallEntry>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtSuperTypeCallEntry? {
        return diagnostic.psiElement.getStrictParentOfType<KtSuperTypeCallEntry>()
    }

    override fun createCallableInfo(element: KtSuperTypeCallEntry, diagnostic: Diagnostic): CallableInfo? {
        val typeReference = element.calleeExpression.typeReference ?: return null

        val project = element.project

        val superType = typeReference.analyze()[BindingContext.TYPE, typeReference] ?: return null
        val superClassDescriptor = superType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
        if (superClassDescriptor.kind != ClassKind.CLASS) return null
        val targetClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, superClassDescriptor) ?: return null
        if (!(targetClass.canRefactor() && (targetClass is KtClass || targetClass is PsiClass))) return null

        val anyType = superClassDescriptor.builtIns.nullableAnyType
        val parameters = element.valueArguments.map {
            ParameterInfo(
                it.getArgumentExpression()?.let { expression -> TypeInfo(expression, Variance.IN_VARIANCE) } ?: TypeInfo(
                    anyType,
                    Variance.IN_VARIANCE
                ),
                it.getArgumentName()?.asName?.asString()
            )
        }

        return ConstructorInfo(parameters, targetClass)
    }
}
