/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ConstructorInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.Variance

object CreateConstructorFromDelegationCallActionFactory : CreateCallableMemberFromUsageFactory<KtConstructorDelegationCall>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtConstructorDelegationCall? {
        return diagnostic.psiElement.getStrictParentOfType<KtConstructorDelegationCall>()
    }

    override fun createCallableInfo(element: KtConstructorDelegationCall, diagnostic: Diagnostic): CallableInfo? {
        val calleeExpression = element.calleeExpression ?: return null
        val currentClass = element.getStrictParentOfType<KtClass>() ?: return null

        val project = currentClass.project

        val classDescriptor = currentClass.resolveToDescriptorIfAny() ?: return null

        val targetClass = if (calleeExpression.isThis) {
            currentClass
        } else {
            val superClassDescriptor =
                DescriptorUtils.getSuperclassDescriptors(classDescriptor).singleOrNull { it.kind == ClassKind.CLASS } ?: return null
            DescriptorToSourceUtilsIde.getAnyDeclaration(project, superClassDescriptor) ?: return null
        }
        if (!(targetClass.canRefactor() && (targetClass is KtClass || targetClass is PsiClass))) return null

        val anyType = classDescriptor.builtIns.nullableAnyType
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
