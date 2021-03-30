/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.unpackFunctionLiteral
import org.jetbrains.kotlin.resolve.BindingContext


class JsonFormatRedundantDiagnostic : CallChecker {
    private val jsonFqName = FqName("kotlinx.serialization.json.Json")
    private val jsonDefaultFqName = FqName("kotlinx.serialization.json.Json.Default")
    private val parameterNameFrom = Name.identifier("from")
    private val parameterNameBuilderAction = Name.identifier("builderAction")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val functionDescriptor = resolvedCall.resultingDescriptor as? SimpleFunctionDescriptor ?: return
        val bindingContext = context.trace.bindingContext

        if (isJsonFormatCreation(functionDescriptor)) {
            if (isDefaultFormat(resolvedCall, bindingContext)) {
                context.trace.report(SerializationErrors.JSON_FORMAT_REDUNDANT_DEFAULT.on(resolvedCall.call.callElement))
            }
            return
        }

        val receiverExpression = resolvedCall.getReceiverExpression() ?: return
        val receiverResolvedCall = receiverExpression.getResolvedCall(bindingContext) ?: return
        val receiverFunctionDescriptor = receiverResolvedCall.resultingDescriptor as? SimpleFunctionDescriptor ?: return

        if (isJsonFormatCreation(receiverFunctionDescriptor) && !isDefaultFormat(receiverResolvedCall, bindingContext)) {
            context.trace.report(SerializationErrors.JSON_FORMAT_REDUNDANT.on(receiverExpression.originalElement))
        }
    }

    private fun isJsonFormatCreation(descriptor: SimpleFunctionDescriptor): Boolean {
        return descriptor.importableFqName == jsonFqName
    }

    private fun isDefaultFormat(resolvedCall: ResolvedCall<*>, context: BindingContext): Boolean {
        var defaultFrom = false
        var emptyBuilder = false

        resolvedCall.valueArguments.forEach { (paramDesc, arg) ->
            when (paramDesc.name) {
                parameterNameFrom -> defaultFrom = isDefaultFormatArgument(arg, context)
                parameterNameBuilderAction -> emptyBuilder = isEmptyFunctionArgument(arg)
            }
        }

        return defaultFrom && emptyBuilder
    }

    private fun isDefaultFormatArgument(arg: ResolvedValueArgument, context: BindingContext): Boolean {
        if (arg is DefaultValueArgument) return true
        if (arg !is ExpressionValueArgument) return false
        val expression = arg.valueArgument?.getArgumentExpression() ?: return false

        val fqName = context[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type?.fqName ?: return false
        return fqName == jsonDefaultFqName
    }

    private fun isEmptyFunctionArgument(arg: ResolvedValueArgument): Boolean {
        if (arg !is ExpressionValueArgument) {
            return false
        }
        val argumentExpression = arg.valueArgument?.getArgumentExpression() ?: return false

        val blockExpression = if (argumentExpression is KtNamedFunction) {
            // anonymous functions
            argumentExpression.bodyBlockExpression ?: return true
        } else {
            // function literal
            argumentExpression.unpackFunctionLiteral()?.bodyExpression
        }

        return blockExpression?.statements?.isEmpty() ?: false
    }
}
