/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinedArgument
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.util.OperatorNameConventions

open class ComposableCallChecker :
    CallChecker,
    AdditionalTypeChecker,
    StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(this)
    }

    private fun checkInlineLambdaCall(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall !is VariableAsFunctionResolvedCall) return
        val descriptor = resolvedCall.variableCall.resultingDescriptor
        if (descriptor !is ValueParameterDescriptor) return
        if (descriptor.type.hasDisallowComposableCallsAnnotation()) return
        val function = descriptor.containingDeclaration
        if (
            function is FunctionDescriptor &&
            function.isInline &&
            function.isMarkedAsComposable()
        ) {
            val bindingContext = context.trace.bindingContext
            var node: PsiElement? = reportOn
            loop@while (node != null) {
                when (node) {
                    is KtLambdaExpression -> {
                        val arg = getArgumentDescriptor(node.functionLiteral, bindingContext)
                        if (arg?.type?.hasDisallowComposableCallsAnnotation() == true) {
                            val parameterSrc = descriptor.findPsi()
                            if (parameterSrc != null) {
                                missingDisallowedComposableCallPropagation(
                                    context,
                                    parameterSrc,
                                    descriptor,
                                    arg
                                )
                            }
                        }
                    }
                    is KtFunction -> {
                        val fn = bindingContext[BindingContext.FUNCTION, node]
                        if (fn == function) {
                            return
                        }
                    }
                }
                node = node.parent as? KtElement
            }
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (!resolvedCall.isComposableInvocation()) {
            checkInlineLambdaCall(resolvedCall, reportOn, context)
            return
        }
        val bindingContext = context.trace.bindingContext
        var node: PsiElement? = reportOn
        loop@while (node != null) {
            when (node) {
                is KtFunctionLiteral -> {
                    // keep going, as this is a "KtFunction", but we actually want the
                    // KtLambdaExpression
                }
                is KtLambdaExpression -> {
                    val descriptor = bindingContext[BindingContext.FUNCTION, node.functionLiteral]
                    if (descriptor == null) {
                        illegalCall(context, reportOn)
                        return
                    }
                    val composable = descriptor.isComposableCallable(bindingContext)
                    if (composable) return
                    val arg = getArgumentDescriptor(node.functionLiteral, bindingContext)
                    if (arg?.type?.hasDisallowComposableCallsAnnotation() == true) {
                        context.trace.record(
                            ComposeWritableSlices.LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE,
                            descriptor,
                            false
                        )
                        context.trace.report(
                            ComposeErrors.CAPTURED_COMPOSABLE_INVOCATION.on(
                                reportOn,
                                arg,
                                arg.containingDeclaration
                            )
                        )
                        return
                    }

                    // TODO(lmr): in future, we should check for CALLS_IN_PLACE contract
                    val isInlined = isInlinedArgument(
                        node.functionLiteral,
                        bindingContext,
                        true
                    )
                    if (!isInlined) {
                        illegalCall(context, reportOn)
                        return
                    } else {
                        // since the function is inlined, we continue going up the PSI tree
                        // until we find a composable context. We also mark this lambda
                        context.trace.record(
                            ComposeWritableSlices.LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE,
                            descriptor,
                            true
                        )
                    }
                }
                is KtTryExpression -> {
                    val tryKeyword = node.tryKeyword
                    if (
                        node.tryBlock.textRange.contains(reportOn.textRange) &&
                        tryKeyword != null
                    ) {
                        context.trace.report(
                            ComposeErrors.ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE.on(tryKeyword)
                        )
                    }
                }
                is KtFunction -> {
                    val descriptor = bindingContext[BindingContext.FUNCTION, node]
                    if (descriptor == null) {
                        illegalCall(context, reportOn)
                        return
                    }
                    val composable = descriptor.isComposableCallable(bindingContext)
                    if (!composable) {
                        illegalCall(context, reportOn, node.nameIdentifier ?: node)
                    }
                    if (descriptor.hasReadonlyComposableAnnotation()) {
                        // enforce that the original call was readonly
                        if (!resolvedCall.isReadOnlyComposableInvocation()) {
                            illegalCallMustBeReadonly(
                                context,
                                reportOn
                            )
                        }
                    }
                    return
                }
                is KtProperty -> {
                    // NOTE: since we're explicitly going down a different branch for
                    // KtPropertyAccessor, the ONLY time we make it into this branch is when the
                    // call was done in the initializer of the property/variable.
                    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, node]
                    if (
                        descriptor !is LocalVariableDescriptor &&
                        node.annotationEntries.hasComposableAnnotation(bindingContext)
                    ) {
                        // composables shouldn't have initializers in the first place
                        illegalCall(context, reportOn)
                        return
                    }
                }
                is KtPropertyAccessor -> {
                    val property = node.property
                    val isComposable = node
                        .annotationEntries.hasComposableAnnotation(bindingContext)
                    if (!isComposable) {
                        illegalCall(context, reportOn, property.nameIdentifier ?: property)
                    }
                    val descriptor = bindingContext[BindingContext.PROPERTY_ACCESSOR, node]
                        ?: return
                    if (descriptor.hasReadonlyComposableAnnotation()) {
                        // enforce that the original call was readonly
                        if (!resolvedCall.isReadOnlyComposableInvocation()) {
                            illegalCallMustBeReadonly(
                                context,
                                reportOn
                            )
                        }
                    }
                    return
                }
                is KtCallableReferenceExpression -> {
                    illegalComposableFunctionReference(context, node)
                    return
                }
                is KtFile -> {
                    // if we've made it this far, the call was made in a non-composable context.
                    illegalCall(context, reportOn)
                    return
                }
                is KtClass -> {
                    // composable calls are never allowed in the initializers of a class
                    illegalCall(context, reportOn)
                    return
                }
            }
            node = node.parent as? KtElement
        }
    }

    private fun missingDisallowedComposableCallPropagation(
        context: CallCheckerContext,
        unmarkedParamEl: PsiElement,
        unmarkedParamDescriptor: ValueParameterDescriptor,
        markedParamDescriptor: ValueParameterDescriptor
    ) {
        context.trace.report(
            ComposeErrors.MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION.on(
                unmarkedParamEl,
                unmarkedParamDescriptor,
                markedParamDescriptor,
                markedParamDescriptor.containingDeclaration
            )
        )
    }

    private fun illegalCall(
        context: CallCheckerContext,
        callEl: PsiElement,
        functionEl: PsiElement? = null
    ) {
        context.trace.report(ComposeErrors.COMPOSABLE_INVOCATION.on(callEl))
        if (functionEl != null) {
            context.trace.report(ComposeErrors.COMPOSABLE_EXPECTED.on(functionEl))
        }
    }

    private fun illegalCallMustBeReadonly(
        context: CallCheckerContext,
        callEl: PsiElement
    ) {
        context.trace.report(ComposeErrors.NONREADONLY_CALL_IN_READONLY_COMPOSABLE.on(callEl))
    }

    private fun illegalComposableFunctionReference(
        context: CallCheckerContext,
        refExpr: KtCallableReferenceExpression
    ) {
        context.trace.report(ComposeErrors.COMPOSABLE_FUNCTION_REFERENCE.on(refExpr))
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        val bindingContext = c.trace.bindingContext
        val expectedType = c.expectedType
        if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
        if (expectedType === TypeUtils.UNIT_EXPECTED_TYPE) return
        if (expectedType.isAnyOrNullableAny()) return
        val expectedComposable = c.hasComposableExpectedType(expression)
        if (expression is KtLambdaExpression) {
            val descriptor = bindingContext[BindingContext.FUNCTION, expression.functionLiteral]
                ?: return
            val isComposable = descriptor.isComposableCallable(bindingContext)
            if (expectedComposable != isComposable) {
                val isInlineable = isInlinedArgument(
                    expression.functionLiteral,
                    c.trace.bindingContext,
                    true
                )
                if (isInlineable) return

                if (!expectedComposable && isComposable) {
                    val inferred = c.trace.bindingContext[
                        ComposeWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR,
                        descriptor
                    ] == true
                    if (inferred) {
                        return
                    }
                }

                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    ComposeErrors.TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        } else {
            val nullableAnyType = expectedType.builtIns.nullableAnyType
            val anyType = expectedType.builtIns.anyType

            if (anyType == expectedType.lowerIfFlexible() &&
                nullableAnyType == expectedType.upperIfFlexible()
            ) return

            val nullableNothingType = expectedType.builtIns.nullableNothingType

            // Handle assigning null to a nullable composable type
            if (expectedType.isMarkedNullable &&
                expressionTypeWithSmartCast == nullableNothingType
            ) return
            val isComposable = expressionType.hasComposableAnnotation()

            if (expectedComposable != isComposable) {
                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    ComposeErrors.TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        }
    }
}

fun ResolvedCall<*>.isReadOnlyComposableInvocation(): Boolean {
    if (this is VariableAsFunctionResolvedCall) {
        return false
    }
    return when (val candidateDescriptor = candidateDescriptor) {
        is ValueParameterDescriptor -> false
        is LocalVariableDescriptor -> false
        is PropertyDescriptor -> {
            val isGetter = valueArguments.isEmpty()
            val getter = candidateDescriptor.getter
            if (isGetter && getter != null) {
                getter.hasReadonlyComposableAnnotation()
            } else {
                false
            }
        }
        is PropertyGetterDescriptor -> candidateDescriptor.hasReadonlyComposableAnnotation()
        else -> candidateDescriptor.hasReadonlyComposableAnnotation()
    }
}

fun ResolvedCall<*>.isComposableInvocation(): Boolean {
    if (this is VariableAsFunctionResolvedCall) {
        if (variableCall.candidateDescriptor.type.hasComposableAnnotation())
            return true
        if (functionCall.resultingDescriptor.hasComposableAnnotation()) return true
        return false
    }
    val candidateDescriptor = candidateDescriptor
    if (candidateDescriptor is FunctionDescriptor) {
        if (candidateDescriptor.isOperator &&
            candidateDescriptor.name == OperatorNameConventions.INVOKE
        ) {
            if (dispatchReceiver?.type?.hasComposableAnnotation() == true) {
                return true
            }
        }
    }
    return when (candidateDescriptor) {
        is ValueParameterDescriptor -> false
        is LocalVariableDescriptor -> false
        is PropertyDescriptor -> {
            val isGetter = valueArguments.isEmpty()
            val getter = candidateDescriptor.getter
            if (isGetter && getter != null) {
                getter.hasComposableAnnotation()
            } else {
                false
            }
        }
        is PropertyGetterDescriptor -> candidateDescriptor.hasComposableAnnotation()
        else -> candidateDescriptor.hasComposableAnnotation()
    }
}

internal fun CallableDescriptor.isMarkedAsComposable(): Boolean {
    return when (this) {
        is PropertyGetterDescriptor -> hasComposableAnnotation()
        is ValueParameterDescriptor -> type.hasComposableAnnotation()
        is LocalVariableDescriptor -> type.hasComposableAnnotation()
        is PropertyDescriptor -> false
        else -> hasComposableAnnotation()
    }
}

// if you called this, it would need to be a composable call (composer, changed, etc.)
fun CallableDescriptor.isComposableCallable(bindingContext: BindingContext): Boolean {
    // if it's marked as composable then we're done
    if (isMarkedAsComposable()) return true
    if (
        this is FunctionDescriptor &&
        bindingContext[ComposeWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR, this] == true
    ) {
        // even though it's not marked, it is inferred as so by the type system (by being passed
        // into a parameter marked as composable or a variable typed as one. This isn't much
        // different than being marked explicitly.
        return true
    }
    val functionLiteral = findPsi() as? KtFunctionLiteral
        // if it isn't a function literal then we are out of things to try.
        ?: return false

    if (functionLiteral.annotationEntries.hasComposableAnnotation(bindingContext)) {
        // in this case the function literal itself is being annotated as composable but the
        // annotation isn't in the descriptor itself
        return true
    }
    val lambdaExpr = functionLiteral.parent as? KtLambdaExpression
    if (
        lambdaExpr != null &&
        bindingContext[ComposeWritableSlices.INFERRED_COMPOSABLE_LITERAL, lambdaExpr] == true
    ) {
        // this lambda was marked as inferred to be composable
        return true
    }
    return false
}

// the body of this function can have composable calls in it, even if it itself is not
// composable (it might capture a composer from the parent)
fun FunctionDescriptor.allowsComposableCalls(bindingContext: BindingContext): Boolean {
    // if it's callable as a composable, then the answer is yes.
    if (isComposableCallable(bindingContext)) return true
    // otherwise, this is only true if it is a lambda which can be capable of composer
    // capture
    return bindingContext[
        ComposeWritableSlices.LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE,
        this
    ] == true
}

// The resolution context usually contains a call position, which records
// the ResolvedCall and ValueParameterDescriptor for the call that we are
// currently resolving. However, it is possible to end up in the
// [ComposableCallChecker] or [ComposeTypeResolutionInterceptorExtension]
// before the frontend computes the call position (e.g., when intercepting
// function literal descriptors).
//
// In this case, the function below falls back to looking at the parse tree
// for `expression`, to determine whether we are resolving a value argument.
private fun ResolutionContext<*>.getValueArgumentPosition(
    expression: KtExpression
): CallPosition.ValueArgumentPosition? =
    when (val position = callPosition) {
        is CallPosition.ValueArgumentPosition ->
            position

        is CallPosition.Unknown ->
            getValueArgumentPositionFromPsi(expression, trace.bindingContext)

        else ->
            null
    }

private fun getValueArgumentPositionFromPsi(
    expression: KtExpression,
    context: BindingContext,
): CallPosition.ValueArgumentPosition? {
    val resolvedCall = KtPsiUtil
        .getParentCallIfPresent(expression)
        .getResolvedCall(context)
        ?: return null

    val valueArgument = resolvedCall.call.getValueArgumentForExpression(expression)
        ?: return null

    val argumentMatch = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch
        ?: return null

    return CallPosition.ValueArgumentPosition(
        resolvedCall,
        argumentMatch.valueParameter,
        valueArgument
    )
}

private fun getArgumentDescriptor(
    expression: KtExpression,
    context: BindingContext
): ValueParameterDescriptor? =
    getValueArgumentPositionFromPsi(expression, context)?.valueParameter

internal fun ResolutionContext<*>.hasComposableExpectedType(expression: KtExpression): Boolean {
    if (expectedType.hasComposableAnnotation())
        return true

    // The Kotlin frontend discards all annotations when computing function
    // types for fun interfaces. As a workaround we retrieve the fun interface
    // from the current value argument position and check the annotations on the
    // underlying method.
    if (expectedType.isSpecialType || !expectedType.isBuiltinFunctionalType)
        return false

    val position = getValueArgumentPosition(expression)
        ?: return false

    // There are two kinds of SAM conversions in Kotlin.
    //
    // - Explicit SAM conversion by calling a synthetic fun interface constructor,
    //   i.e., `A { ... }` or `A(f)` for a fun interface `A`.
    // - Implicit SAM conversion by calling a function which expects a fun interface
    //   in a value parameter.
    //
    // For explicit SAM conversion we check for the presence of a synthetic call,
    // otherwise we check the type of the value parameter descriptor.
    val callDescriptor = position.resolvedCall.resultingDescriptor.original
    val samDescriptor = if (callDescriptor is FunctionInterfaceConstructorDescriptor) {
        callDescriptor.baseDescriptorForSynthetic
    } else {
        position.valueParameter.type.constructor.declarationDescriptor as? ClassDescriptor
            ?: return false
    }

    return getSingleAbstractMethodOrNull(samDescriptor)?.hasComposableAnnotation() == true
}

fun List<KtAnnotationEntry>.hasComposableAnnotation(bindingContext: BindingContext): Boolean {
    for (entry in this) {
        val descriptor = bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
        if (descriptor.isComposableAnnotation) return true
    }
    return false
}
