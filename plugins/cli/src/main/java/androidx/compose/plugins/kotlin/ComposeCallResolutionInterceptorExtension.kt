/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.extensions.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinedArgument
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.util.OperatorNameConventions

class ComposeCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
    override fun interceptCandidates(
        candidates: Collection<NewResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<NewResolutionOldInference.MyCandidate> {
        return candidates
    }

    override fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        if (candidates.isEmpty()) return candidates
        if (KtxCallResolver.resolving.get().get()) return candidates

        for (arg in resolutionContext.call.valueArguments) {
            if (arg is KtLambdaArgument) continue
            if (arg.getArgumentName() != null) continue
            return candidates
        }

        val element = resolutionContext.call.callElement as KtExpression
        val composableAnnotationChecker =
            ComposableAnnotationChecker.get(element.project)

        val context = ExpressionTypingContext.newContext(
            resolutionContext.trace,
            resolutionContext.scope,
            resolutionContext.dataFlowInfo,
            resolutionContext.expectedType,
            resolutionContext.languageVersionSettings,
            resolutionContext.dataFlowValueFactory
        )

        val temporaryTraceForKtxCall =
                TemporaryTraceAndCache.create(
                        context,
                        "trace to resolve ktx call", element
                )
        val temporaryForKtxCall = context.replaceTraceAndCache(temporaryTraceForKtxCall)

        // Ensure we are in a composable context
        var walker: PsiElement? = element
        var isComposableContext = false
        val facade = CallResolutionInterceptorExtension.facade.get().peek()
        while (walker != null) {
            val descriptor = try {
                resolutionContext.trace[BindingContext.FUNCTION, walker]
            } catch (e: Exception) {
                null
            }
            if (descriptor != null) {
                val composability = composableAnnotationChecker.analyze(
                    temporaryTraceForKtxCall.trace,
                    descriptor
                )
                isComposableContext =
                        composability != ComposableAnnotationChecker.Composability.NOT_COMPOSABLE

                // If the descriptor is for an inlined lambda, infer composability from the
                // outer scope
                if (!(walker is KtFunction) ||
                        !isInlinedArgument(
                                walker,
                                context.trace.bindingContext,
                                true))
                    break
            }
            walker = try { walker.parent } catch (e: Throwable) { null }
        }
        if (!isComposableContext) return candidates

        val callResolver =
            (scopeTower as NewResolutionOldInference.ImplicitScopeTowerImpl).callResolver
        val ktxCallResolver = KtxCallResolver(
            callResolver,
            facade,
            element.project,
            composableAnnotationChecker
        )

        val call = resolutionContext.call

        val resolvedKtxElementCall = try {

            ktxCallResolver.initializeFromCall(call, temporaryForKtxCall)

            ktxCallResolver.resolveFromCall(
                call,
                temporaryForKtxCall
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }

        // Doesn't appear to be resolvable to a composable; return normal resolution candidates
        val resolvedCall = when (resolvedKtxElementCall.emitOrCall) {
            is MemoizedCallNode -> resolvedKtxElementCall.emitOrCall.call.resolvedCalls().first()
            is EmitCallNode -> resolvedKtxElementCall.emitOrCall.resolvedCalls().first()
            is ErrorNode -> return candidates
            else ->
                throw Error("Unexpectd type: " + resolvedKtxElementCall.emitOrCall.javaClass)
        }

        if (resolvedKtxElementCall.emitOrCall is MemoizedCallNode &&
                !ComposableAnnotationChecker.get(element.project).shouldInvokeAsTag(
                    temporaryForKtxCall.trace,
                    resolvedCall
                ))
            return candidates
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (candidateDescriptor is FunctionDescriptor && candidateDescriptor.isOperator &&
                candidateDescriptor.name == OperatorNameConventions.INVOKE)
            return candidates

        // If control flow gets here, we are intercepting this call.

        val original = when (resolvedKtxElementCall.emitOrCall) {
            is MemoizedCallNode -> {
                resolvedKtxElementCall.emitOrCall.call.resolvedCalls().first().candidateDescriptor
                        as? SimpleFunctionDescriptor
            }
            else -> null
        }

        val descriptor =
            ComposableInvocationDescriptor(
                element,
                resolvedKtxElementCall,
                resolvedKtxElementCall.infixOrCall!!.candidateDescriptor.containingDeclaration,
                original,
                Annotations.EMPTY,
                name,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
            )

        val valueArgs = mutableListOf<ValueParameterDescriptor>()

        resolvedKtxElementCall.usedAttributes.forEachIndexed { index, attributeInfo ->
            valueArgs.add(
                ValueParameterDescriptorImpl(
                    descriptor, null, index,
                    Annotations.EMPTY,
                    Name.identifier(
                            if (attributeInfo.name == CHILDREN_KEY)
                                attributeInfo.descriptor.name.identifier
                            else attributeInfo.name
                    ),
                    attributeInfo.type, false,
                    false,
                    false, null,
                    SourceElement.NO_SOURCE
                )
            )
        }

        val unitLambdaType = scopeTower.module.builtIns.getFunction(
                0
        ).defaultType.replace(
                listOf(scopeTower.module.builtIns.unitType.asTypeProjection())
        ).makeComposable(scopeTower.module)
        (resolvedKtxElementCall.emitOrCall as? EmitCallNode)?.inlineChildren?.let {
            valueArgs.add(
                ValueParameterDescriptorImpl(
                    descriptor, null, valueArgs.size,
                    Annotations.EMPTY,
                    Name.identifier("\$CHILDREN"),
                    unitLambdaType, false,
                    false,
                    false, null,
                    SourceElement.NO_SOURCE
                )
            )
        }

        descriptor.initialize(
            null,
            null,
            mutableListOf(),
            valueArgs,
            scopeTower.module.builtIns.unitType,
            Modality.FINAL,
            Visibilities.DEFAULT_VISIBILITY
        )

        return listOf(descriptor)
    }

    class ComposableInvocationDescriptor(
        val element: KtExpression,
        val ktxCall: ResolvedKtxElementCall,
        containingDeclaration: DeclarationDescriptor,
        original: SimpleFunctionDescriptor?,
        annotations: Annotations,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        source: SourceElement
    ) : SimpleFunctionDescriptorImpl(
        containingDeclaration,
        original,
        annotations,
        name,
        kind,
        source)
}
