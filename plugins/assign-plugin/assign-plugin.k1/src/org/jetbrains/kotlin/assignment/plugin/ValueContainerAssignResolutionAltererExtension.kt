/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.assignment.plugin.diagnostics.ErrorsAssignmentPlugin.CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.assignment.plugin.diagnostics.ErrorsAssignmentPlugin.NO_APPLICABLE_ASSIGN_METHOD
import org.jetbrains.kotlin.cfg.getElementParentDeclaration
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.util.CallMaker.makeCallWithExpressions
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver.Companion.create
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingComponents
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

class CliAssignPluginResolutionAltererExtension(
    private val annotations: List<String>
) : AbstractAssignPluginResolutionAltererExtension() {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> = annotations
}

@OptIn(InternalNonStableExtensionPoints::class)
abstract class AbstractAssignPluginResolutionAltererExtension : AssignResolutionAltererExtension {

    override fun needOverloadAssign(expression: KtBinaryExpression, leftType: KotlinType?, bindingContext: BindingContext): Boolean {
        return expression.isValPropertyAssignment(bindingContext) && leftType.hasSpecialAnnotation(expression)
    }

    private fun KtBinaryExpression.isValPropertyAssignment(bindingContext: BindingContext): Boolean {
        val descriptor: VariableDescriptor? = BindingContextUtils.extractVariableFromResolvedCall(bindingContext, this.left)
        return descriptor is PropertyDescriptor && !descriptor.isVar
    }

    private fun KotlinType?.hasSpecialAnnotation(expression: KtBinaryExpression): Boolean =
        this?.constructor?.declarationDescriptor?.hasSpecialAnnotation(expression.getElementParentDeclaration()) ?: false

    override fun resolveAssign(
        bindingContext: BindingContext,
        expression: KtBinaryExpression,
        leftOperand: KtExpression,
        left: KtExpression,
        leftInfo: KotlinTypeInfo,
        context: ExpressionTypingContext,
        components: ExpressionTypingComponents,
        scope: LexicalWritableScope
    ): KotlinTypeInfo? {
        var leftType: KotlinType = leftInfo.type!!
        if (leftOperand is KtSafeQualifiedExpression) {
            leftType = leftType.makeNotNullable()
        }
        val receiver = create(left, leftType, context.trace.bindingContext)
        val operationSign: KtSimpleNameExpression = expression.operationReference
        val temporaryForAssignmentOperation: TemporaryTraceAndCache =
            TemporaryTraceAndCache.create(context, "trace to check assignment operation like '=' for", expression)
        val temporaryContext = context.replaceTraceAndCache(temporaryForAssignmentOperation).replaceScope(scope)
        val methodDescriptors: OverloadResolutionResults<FunctionDescriptor> =
            components.callResolver.resolveMethodCall(temporaryContext, receiver, expression)
        val methodReturnType: KotlinType? = OverloadResolutionResultsUtil.getResultingType(methodDescriptors, context)

        if (methodDescriptors.isSuccess && methodReturnType != null) {
            temporaryForAssignmentOperation.commit()
            return if (methodReturnType.isUnit()) {
                leftInfo.replaceType(methodReturnType)
            } else {
                context.trace.report(CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT.on(operationSign))
                null
            }
        }
        context.trace.report(NO_APPLICABLE_ASSIGN_METHOD.on(operationSign))
        return null
    }

    private fun CallResolver.resolveMethodCall(
        context: ExpressionTypingContext, receiver: ExpressionReceiver, binaryExpression: KtBinaryExpression
    ): OverloadResolutionResults<FunctionDescriptor> {
        val call = makeCallWithExpressions(
            binaryExpression, receiver, null, binaryExpression.operationReference, Collections.singletonList(binaryExpression.right)
        )
        return resolveCallWithGivenName(context, call, binaryExpression.operationReference, ASSIGN_METHOD)
    }
}
