/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment

import org.jetbrains.kotlin.cfg.getElementParentDeclaration
import org.jetbrains.kotlin.container.assignment.diagnostics.ErrorsValueContainerAssignment.CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.container.assignment.diagnostics.ErrorsValueContainerAssignment.NO_APPLICABLE_ASSIGN_METHOD
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
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
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingComponents
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import java.util.*

class CliValueContainerAssignResolutionAltererExtension(
    private val annotations: List<String>
) : AbstractValueContainerAssignResolutionAltererExtension() {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> = annotations
}

@OptIn(InternalNonStableExtensionPoints::class)
abstract class AbstractValueContainerAssignResolutionAltererExtension : AssignResolutionAltererExtension {

    companion object {
        val ASSIGN_METHOD = Name.identifier("assign")
    }

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
        val leftType: KotlinType = leftInfo.type!!
        val receiver = create(left, leftType, context.trace.bindingContext)
        val operationSign: KtSimpleNameExpression = expression.operationReference
        val temporaryForAssignmentOperation: TemporaryTraceAndCache =
            TemporaryTraceAndCache.create(context, "trace to check assignment operation like '=' for", expression)
        val temporaryContext = context.replaceTraceAndCache(temporaryForAssignmentOperation).replaceScope(scope)
        val methodDescriptors: OverloadResolutionResults<FunctionDescriptor> =
            components.callResolver.resolveMethodCall(temporaryContext, receiver, expression)
        val methodReturnType: KotlinType? = OverloadResolutionResultsUtil.getResultingType(methodDescriptors, context)

        if (methodDescriptors.isSuccess) {
            methodReturnType?.let {
                temporaryForAssignmentOperation.commit()
                return if (!KotlinTypeChecker.DEFAULT.equalTypes(components.builtIns.unitType, methodReturnType)) {
                    context.trace.report(CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT.on(operationSign))
                    null
                } else {
                    leftInfo.replaceType(methodReturnType)
                }
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
