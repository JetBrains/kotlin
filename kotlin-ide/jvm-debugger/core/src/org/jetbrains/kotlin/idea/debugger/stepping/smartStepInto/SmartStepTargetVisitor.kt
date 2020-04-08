/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping.smartStepInto

import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiMethod
import com.intellij.util.Range
import com.intellij.util.containers.OrderedSet
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.debugger.breakpoints.isInlineOnly
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.isFromJava
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

// TODO support class initializers, local functions, delegated properties with specified type, setter for properties
class SmartStepTargetVisitor(
    private val element: KtElement,
    private val lines: Range<Int>,
    private val consumer: OrderedSet<SmartStepTarget>
) : KtTreeVisitorVoid() {
    private fun append(target: SmartStepTarget) {
        consumer += target
    }

    private val intrinsicMethods = run {
        val jvmTarget = element.platform.firstIsInstanceOrNull<JdkPlatform>()?.targetVersion ?: JvmTarget.DEFAULT
        IntrinsicMethods(jvmTarget)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        recordFunction(lambdaExpression.functionLiteral)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (!recordFunction(function)) {
            super.visitNamedFunction(function)
        }
    }

    private fun recordFunction(function: KtFunction): Boolean {
        val context = function.analyze()
        val resolvedCall = function.getParentCall(context).getResolvedCall(context) ?: return false
        val arguments = resolvedCall.valueArguments

        for ((param, argument) in arguments) {
            if (argument.arguments.any { getArgumentExpression(it) == function }) {
                val resultingDescriptor = resolvedCall.resultingDescriptor
                val label = KotlinLambdaSmartStepTarget.calcLabel(resultingDescriptor, param.name)
                val isInline = InlineUtil.isInline(resultingDescriptor)
                val isSuspend = param.type.isSuspendFunctionType
                val target = KotlinLambdaSmartStepTarget(
                    label,
                    function,
                    lines,
                    isInline,
                    isSuspend
                )
                append(target)
                return true
            }
        }

        return false
    }

    private fun getArgumentExpression(it: ValueArgument): KtExpression? {
        return (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        // Skip calls in object declarations
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        expression.condition?.accept(this)
    }

    override fun visitWhileExpression(expression: KtWhileExpression) {
        expression.condition?.accept(this)
    }

    override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
        expression.condition?.accept(this)
    }

    override fun visitForExpression(expression: KtForExpression) {
        expression.loopRange?.accept(this)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        expression.subjectExpression?.accept(this)
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        recordFunctionCall(expression)
        super.visitArrayAccessExpression(expression)
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression) {
        recordFunctionCall(expression.operationReference)
        super.visitUnaryExpression(expression)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        recordFunctionCall(expression.operationReference)
        super.visitBinaryExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val calleeExpression = expression.calleeExpression
        if (calleeExpression != null) {
            recordFunctionCall(calleeExpression)
        }
        super.visitCallExpression(expression)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        recordGetter(expression)
        super.visitSimpleNameExpression(expression)
    }

    private fun recordGetter(expression: KtSimpleNameExpression) {
        val bindingContext = expression.analyze()
        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return

        val getterDescriptor = propertyDescriptor.getter
        if (getterDescriptor == null || getterDescriptor.isDefault) return

        val ktDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, getterDescriptor) as? KtDeclaration ?: return

        val delegatedResolvedCall = bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor]
        if (delegatedResolvedCall != null) {
            val delegatedPropertyGetterDescriptor = delegatedResolvedCall.resultingDescriptor
            val label = "${propertyDescriptor.name}." + KotlinMethodSmartStepTarget.calcLabel(delegatedPropertyGetterDescriptor)
            append(KotlinMethodSmartStepTarget(delegatedPropertyGetterDescriptor, ktDeclaration, label, expression, lines))
        } else {
            if (ktDeclaration is KtPropertyAccessor && ktDeclaration.hasBody()) {
                val label = KotlinMethodSmartStepTarget.calcLabel(getterDescriptor)
                append(KotlinMethodSmartStepTarget(getterDescriptor, ktDeclaration, label, expression, lines))
            }
        }
    }

    private fun recordFunctionCall(expression: KtExpression) {
        val resolvedCall = expression.resolveToCall() ?: return

        val descriptor = resolvedCall.resultingDescriptor
        if (descriptor !is FunctionDescriptor || isIntrinsic(descriptor)) return

        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, descriptor)
        if (descriptor.isFromJava) {
            if (declaration is PsiMethod) {
                append(MethodSmartStepTarget(declaration, null, expression, false, lines))
            }
        } else {
            if (declaration == null && !isInvokeInBuiltinFunction(descriptor)) {
                return
            }

            if (declaration !is KtDeclaration?) return

            if (descriptor is ConstructorDescriptor && descriptor.isPrimary) {
                if (declaration is KtClass && declaration.getAnonymousInitializers().isEmpty()) {
                    // There is no constructor or init block, so do not show it in smart step into
                    return
                }
            }

            // We can't step into @InlineOnly callables as there is no LVT, so skip them
            if (declaration is KtCallableDeclaration && declaration.isInlineOnly()) {
                return
            }

            val callLabel = KotlinMethodSmartStepTarget.calcLabel(descriptor)
            val label = when (descriptor) {
                is FunctionInvokeDescriptor -> {
                    when (expression) {
                        is KtSimpleNameExpression -> "${runReadAction { expression.text }}.$callLabel"
                        else -> callLabel
                    }
                }
                else -> callLabel
            }

            append(KotlinMethodSmartStepTarget(descriptor, declaration, label, expression, lines))
        }
    }

    private fun isIntrinsic(descriptor: CallableMemberDescriptor): Boolean {
        return intrinsicMethods.getIntrinsic(descriptor) != null
    }

    private fun isInvokeInBuiltinFunction(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor !is FunctionInvokeDescriptor) return false
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return false
        return classDescriptor.defaultType.isBuiltinFunctionalType
    }
}