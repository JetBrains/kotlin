/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument

open class JsonFormatRedundantDiagnostic : CallChecker {
    private val jsonFunName = Name.identifier("Json")
    private val jsonPackageFqName = FqName("kotlinx.serialization.json")
    private val parameterNameFrom = Name.identifier("from")
    private val parameterNameBuilderAction = Name.identifier("builderAction")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val functionDescriptor = resolvedCall.resultingDescriptor as? SimpleFunctionDescriptor ?: return

        if (isJsonFormatCreation(functionDescriptor)) {
            if (isDefaultFormat(resolvedCall)) {
                context.trace.report(SerializationErrors.JSON_FORMAT_REDUNDANT_DEFAULT.on(resolvedCall.call.callElement))
            }
            return
        }

        val receiverExpression = resolvedCall.getReceiverExpression() ?: return
        val receiverResolvedCall = receiverExpression.getResolvedCall(context.trace.bindingContext) ?: return
        val receiverFunctionDescriptor = receiverResolvedCall.resultingDescriptor as? SimpleFunctionDescriptor ?: return

        if (isJsonFormatCreation(receiverFunctionDescriptor) && !isDefaultFormat(receiverResolvedCall)) {
            context.trace.report(SerializationErrors.JSON_FORMAT_REDUNDANT.on(receiverExpression.originalElement))
        }
    }

    private fun isJsonFormatCreation(descriptor: SimpleFunctionDescriptor): Boolean {
        if (!(descriptor.name == jsonFunName && descriptor.extensionReceiverParameter == null && descriptor.valueParameters.size == 2)) {
            return false
        }
        val packageDescriptor = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
        return packageDescriptor.fqName == jsonPackageFqName
    }

    private fun isDefaultFormat(resolvedCall: ResolvedCall<*>): Boolean {
        var defaultFrom = false
        var emptyBuilder = false

        resolvedCall.valueArguments.forEach { (paramDesc, arg) ->
            when (paramDesc.name) {
                parameterNameFrom -> defaultFrom = arg is DefaultValueArgument
                parameterNameBuilderAction -> emptyBuilder = isEmptyLambdaArgument(arg)
            }
        }

        return defaultFrom && emptyBuilder
    }

    private fun isEmptyLambdaArgument(arg: ResolvedValueArgument): Boolean {
        if (arg !is ExpressionValueArgument) {
            return false
        }
        val valueArgument = arg.valueArgument
        if (valueArgument !is KtLambdaArgument) {
            return false
        }

        return valueArgument.getLambdaExpression()?.bodyExpression?.statements?.isEmpty() ?: false
    }
}
