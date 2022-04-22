/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils.isExternalEnum
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.isSynthesized
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

object JsExternalEnumUsageChecker : CallChecker {
    private val enumFqName = FqName("kotlin.Enum")
    private val enumValuesFqName = FqName("kotlin.enumValues")
    private val enumValueOfFqName = FqName("kotlin.enumValueOf")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.isEnumTopLevelHelperCall() && resolvedCall.hasExternalEnumAsTypeArgument()) {
            context.trace.report(ErrorsJs.ENUM_STDLIB_HELPERS_USAGE_WITH_EXTERNAL_ENUM.on(reportOn))
        }

        if (resolvedCall.isExternalEnumMethodCall() && (resolvedCall.isSynthesizedMethodCall() || resolvedCall.isFakeOverriddenSyntheticPropertyAccess())) {
            context.trace.report(ErrorsJs.ENUM_SYNTHETIC_METHOD_USAGE_WITH_EXTERNAL_ENUM.on(reportOn))
        }
    }

    private fun ResolvedCall<*>.isEnumTopLevelHelperCall(): Boolean {
        val calleeFqName = candidateDescriptor?.takeIf { it.source == SourceElement.NO_SOURCE }?.fqNameOrNull() ?: return false
        return calleeFqName == enumValueOfFqName || calleeFqName == enumValuesFqName
    }

    private fun ResolvedCall<*>.hasExternalEnumAsTypeArgument(): Boolean {
        val callee = candidateDescriptor ?: return false
        val typeParameterDescriptor = callee.typeParameters.takeIf { it.size == 1 }?.getOrNull(0) ?: return false
        return isExternalEnum(typeArguments[typeParameterDescriptor]?.constructor?.declarationDescriptor)
    }

    private fun ResolvedCall<*>.isExternalEnumMethodCall(): Boolean {
        return isExternalEnum(candidateDescriptor?.containingDeclaration)
    }

    private fun ResolvedCall<*>.isSynthesizedMethodCall(): Boolean {
        return candidateDescriptor?.isSynthesized == true
    }

    private fun ResolvedCall<*>.isFakeOverriddenSyntheticPropertyAccess(): Boolean {
        val propertyAccess = (candidateDescriptor as? PropertyDescriptor)?.takeIf { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE } ?: return false
        val original = propertyAccess.overriddenDescriptors.takeIf { it.size == 1 }?.first() ?: return false
        return original.containingDeclaration.fqNameOrNull() == enumFqName
    }
}