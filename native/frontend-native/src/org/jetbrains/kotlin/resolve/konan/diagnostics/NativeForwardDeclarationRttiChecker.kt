/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.konan.getForwardDeclarationKindOrNull
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.RttiExpressionChecker
import org.jetbrains.kotlin.resolve.calls.checkers.RttiExpressionInformation
import org.jetbrains.kotlin.resolve.calls.checkers.RttiOperation
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ClassLiteralChecker

class NativeForwardDeclarationRttiChecker : RttiExpressionChecker, ClassLiteralChecker {
    override fun check(rttiInformation: RttiExpressionInformation, reportOn: PsiElement, trace: BindingTrace) {
        val sourceType = rttiInformation.sourceType
        val targetType = rttiInformation.targetType
        val targetDescriptor = targetType?.constructor?.declarationDescriptor
        if (sourceType != null && targetDescriptor is ClassDescriptor) {
            val kind = targetDescriptor.getForwardDeclarationKindOrNull() ?: return
            when (rttiInformation.operation) {
                RttiOperation.IS,
                RttiOperation.NOT_IS -> trace.report(ErrorsNative.CANNOT_CHECK_FOR_FORWARD_DECLARATION.on(reportOn, targetType))

                RttiOperation.AS,
                RttiOperation.SAFE_AS -> {
                    // It can make sense to avoid warning if sourceClass is subclass of class with such property,
                    // but for the sake of simplicity, we don't do it now.
                    val sourceDescriptor = sourceType.constructor.declarationDescriptor as? ClassDescriptor
                    val isAllowedCast = sourceDescriptor != null &&
                            sourceDescriptor.name == targetDescriptor.name &&
                            sourceDescriptor.kind == kind.classKind &&
                            sourceDescriptor.getAllSuperClassifiers().any { it.fqNameSafe == kind.matchSuperClassFqName }
                    if (!isAllowedCast) {
                        trace.report(ErrorsNative.UNCHECKED_CAST_TO_FORWARD_DECLARATION.on(reportOn, sourceType, targetType))
                    }
                }
            }
        }
    }

    override fun check(expression: KtClassLiteralExpression, type: KotlinType, context: ResolutionContext<*>) {
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor
        if (descriptor?.getForwardDeclarationKindOrNull() != null) {
            context.trace.report(ErrorsNative.FORWARD_DECLARATION_AS_CLASS_LITERAL.on(expression, type))
        }
    }
}
