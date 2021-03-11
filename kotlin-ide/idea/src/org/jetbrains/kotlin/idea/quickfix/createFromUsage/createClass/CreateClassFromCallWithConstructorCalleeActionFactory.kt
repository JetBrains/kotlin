/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

object CreateClassFromCallWithConstructorCalleeActionFactory : CreateClassFromUsageFactory<KtCallElement>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallElement? {
        val diagElement = diagnostic.psiElement

        val callElement = PsiTreeUtil.getParentOfType(
            diagElement,
            KtAnnotationEntry::class.java,
            KtSuperTypeCallEntry::class.java
        ) as? KtCallElement ?: return null

        val callee = callElement.calleeExpression as? KtConstructorCalleeExpression ?: return null
        val calleeRef = callee.constructorReferenceExpression ?: return null

        if (!calleeRef.isAncestor(diagElement)) return null
        return callElement
    }

    override fun getPossibleClassKinds(element: KtCallElement, diagnostic: Diagnostic): List<ClassKind> {
        return listOf(if (element is KtAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS)
    }

    override fun extractFixData(element: KtCallElement, diagnostic: Diagnostic): ClassInfo? {
        val isAnnotation = element is KtAnnotationEntry
        val callee = element.calleeExpression as? KtConstructorCalleeExpression ?: return null
        val calleeRef = callee.constructorReferenceExpression ?: return null
        val typeRef = callee.typeReference ?: return null
        val userType = typeRef.typeElement as? KtUserType ?: return null

        val (context, module) = userType.analyzeAndGetResult()

        val qualifier = userType.qualifier?.referenceExpression
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParents = getTargetParentsByQualifier(element, qualifier != null, qualifierDescriptor).ifEmpty { return null }

        val anyType = module.builtIns.nullableAnyType
        val valueArguments = element.valueArguments
        val defaultParamName = if (valueArguments.size == 1) "value" else null
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                it.getArgumentName()?.asName?.asString() ?: defaultParamName
            )
        }

        val typeArgumentInfos = when {
            isAnnotation -> Collections.emptyList<TypeInfo>()
            else -> element.typeArguments.mapNotNull { it.typeReference?.let { TypeInfo(it, Variance.INVARIANT) } }
        }

        return ClassInfo(
            name = calleeRef.getReferencedName(),
            targetParents = targetParents,
            expectedTypeInfo = TypeInfo.Empty,
            parameterInfos = parameterInfos,
            open = !isAnnotation,
            typeArguments = typeArgumentInfos
        )
    }
}
