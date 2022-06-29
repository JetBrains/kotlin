/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.compiler.plugins.kotlin.inference.ErrorReporter
import androidx.compose.compiler.plugins.kotlin.inference.TypeAdapter
import androidx.compose.compiler.plugins.kotlin.inference.ApplierInferencer
import androidx.compose.compiler.plugins.kotlin.inference.Item
import androidx.compose.compiler.plugins.kotlin.inference.LazyScheme
import androidx.compose.compiler.plugins.kotlin.inference.LazySchemeStorage
import androidx.compose.compiler.plugins.kotlin.inference.NodeAdapter
import androidx.compose.compiler.plugins.kotlin.inference.NodeKind
import androidx.compose.compiler.plugins.kotlin.inference.Open
import androidx.compose.compiler.plugins.kotlin.inference.Scheme
import androidx.compose.compiler.plugins.kotlin.inference.Token
import androidx.compose.compiler.plugins.kotlin.inference.deserializeScheme
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType

private sealed class InferenceNode(val element: PsiElement) {
    open val kind: NodeKind get() = when (element) {
        is KtLambdaExpression, is KtFunctionLiteral -> NodeKind.Lambda
        is KtFunction -> NodeKind.Function
        else -> NodeKind.Expression
    }
    abstract val type: InferenceNodeType
}

private sealed class InferenceNodeType {
    abstract fun toScheme(callContext: CallCheckerContext): Scheme
    abstract fun isTypeFor(descriptor: CallableDescriptor): Boolean
}

private class InferenceDescriptorType(val descriptor: CallableDescriptor) : InferenceNodeType() {
    override fun toScheme(callContext: CallCheckerContext): Scheme =
        descriptor.toScheme(callContext)
    override fun isTypeFor(descriptor: CallableDescriptor) = this.descriptor == descriptor
    override fun hashCode(): Int = 31 * descriptor.original.hashCode()
    override fun equals(other: Any?): Boolean =
        other is InferenceDescriptorType && other.descriptor.original == descriptor.original
}

private class InferenceKotlinType(val type: KotlinType) : InferenceNodeType() {
    override fun toScheme(callContext: CallCheckerContext): Scheme = type.toScheme()
    override fun isTypeFor(descriptor: CallableDescriptor): Boolean = false
    override fun hashCode(): Int = 31 * type.hashCode()
    override fun equals(other: Any?): Boolean =
        other is InferenceKotlinType && other.type == type
}

private class InferenceUnknownType : InferenceNodeType() {
    override fun toScheme(callContext: CallCheckerContext): Scheme = Scheme(Open(-1))
    override fun isTypeFor(descriptor: CallableDescriptor): Boolean = false
    override fun hashCode(): Int = System.identityHashCode(this)
    override fun equals(other: Any?): Boolean = other === this
}

private class PsiElementNode(
    element: PsiElement,
    val bindingContext: BindingContext
) : InferenceNode(element) {
    override val type: InferenceNodeType = when (element) {
        is KtLambdaExpression -> descriptorTypeOf(element.functionLiteral)
        is KtFunctionLiteral, is KtFunction -> descriptorTypeOf(element)
        is KtProperty -> kotlinTypeOf(element)
        is KtPropertyAccessor -> kotlinTypeOf(element)
        is KtExpression -> kotlinTypeOf(element)
        else -> descriptorTypeOf(element)
    }

    private fun descriptorTypeOf(element: PsiElement): InferenceNodeType =
        bindingContext[BindingContext.FUNCTION, element]?.let {
            InferenceDescriptorType(it)
        } ?: InferenceUnknownType()

    private fun kotlinTypeOf(element: KtExpression) = element.kotlinType(bindingContext)?.let {
        InferenceKotlinType(it)
    } ?: InferenceUnknownType()
}

private class ResolvedPsiElementNode(
    element: PsiElement,
    override val type: InferenceNodeType,
) : InferenceNode(element) {
    override val kind: NodeKind get() = NodeKind.Function
}

private class ResolvedPsiParameterReference(
    element: PsiElement,
    override val type: InferenceNodeType,
    val index: Int,
    val container: PsiElement
) : InferenceNode(element) {
    override val kind: NodeKind get() = NodeKind.ParameterReference
}

class ComposableTargetChecker : CallChecker, StorageComponentContainerContributor {

    private lateinit var callContext: CallCheckerContext

    private fun containerOf(element: PsiElement): PsiElement? {
        var current: PsiElement? = element.parent
        while (current != null) {
            when (current) {
                is KtLambdaExpression, is KtFunction, is KtProperty, is KtPropertyAccessor ->
                    return current
                is KtClass, is KtFile -> break
            }
            current = current.parent as? KtElement
        }
        return null
    }

    private fun containerNodeOf(element: PsiElement) =
        containerOf(element)?.let {
            PsiElementNode(it, callContext.trace.bindingContext)
        }

    // Create an InferApplier instance with adapters for the Psi front-end
    private val infer = ApplierInferencer(
        typeAdapter = object : TypeAdapter<InferenceNodeType> {
            override fun declaredSchemaOf(type: InferenceNodeType): Scheme =
                type.toScheme(callContext)
            override fun currentInferredSchemeOf(type: InferenceNodeType): Scheme? = null
            override fun updatedInferredScheme(type: InferenceNodeType, scheme: Scheme) { }
        },
        nodeAdapter = object : NodeAdapter<InferenceNodeType, InferenceNode> {
            override fun containerOf(node: InferenceNode): InferenceNode =
                containerNodeOf(node.element) ?: node

            override fun kindOf(node: InferenceNode) = node.kind

            override fun schemeParameterIndexOf(
                node: InferenceNode,
                container: InferenceNode
            ): Int = (node as? ResolvedPsiParameterReference)?.let {
                if (it.container == container.element) it.index else -1
            } ?: -1

            override fun typeOf(node: InferenceNode): InferenceNodeType = node.type

            override fun referencedContainerOf(node: InferenceNode): InferenceNode? {
                return null
            }
        },

        errorReporter = object : ErrorReporter<InferenceNode> {

            /**
             * Find the `description` value from ComposableTargetMarker if the token refers to an
             * annotation with the marker or just return [token] if it cannot be found.
             */
            private fun descriptionFrom(token: String): String {
                val fqName = FqName(token)
                val cls = callContext.moduleDescriptor.findClassAcrossModuleDependencies(
                    ClassId.topLevel(fqName)
                )
                return cls?.let {
                    it.annotations.findAnnotation(
                        ComposeFqNames.ComposableTargetMarker
                    )?.let { marker ->
                      marker.allValueArguments.firstNotNullOfOrNull { entry ->
                          val name = entry.key
                          if (
                              !name.isSpecial &&
                              name.identifier == ComposeFqNames.ComposableTargetMarkerDescription
                          ) {
                              (entry.value as? StringValue)?.value
                          } else null
                      }
                    }
                } ?: token
            }

            override fun reportCallError(node: InferenceNode, expected: String, received: String) {
                if (expected != received) {
                    val expectedDescription = descriptionFrom(expected)
                    val receivedDescription = descriptionFrom(received)
                    callContext.trace.report(
                        ComposeErrors.COMPOSE_APPLIER_CALL_MISMATCH.on(
                            node.element,
                            expectedDescription,
                            receivedDescription
                        )
                    )
                }
            }

            override fun reportParameterError(
                node: InferenceNode,
                index: Int,
                expected: String,
                received: String
            ) {
                if (expected != received) {
                    val expectedDescription = descriptionFrom(expected)
                    val receivedDescription = descriptionFrom(received)
                    callContext.trace.report(
                        ComposeErrors.COMPOSE_APPLIER_PARAMETER_MISMATCH.on(
                            node.element,
                            expectedDescription,
                            receivedDescription
                        )
                    )
                }
            }

            override fun log(node: InferenceNode?, message: String) {
                // ignore log messages in the front-end
            }
        },
        lazySchemeStorage = object : LazySchemeStorage<InferenceNode> {
            override fun getLazyScheme(node: InferenceNode): LazyScheme? =
                callContext.trace.bindingContext.get(
                    ComposeWritableSlices.COMPOSE_LAZY_SCHEME,
                    node.type
                )

            override fun storeLazyScheme(node: InferenceNode, value: LazyScheme) {
                callContext.trace.record(
                    ComposeWritableSlices.COMPOSE_LAZY_SCHEME,
                    node.type,
                    value
                )
            }
        }
    )

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(this)
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (!resolvedCall.isComposableInvocation()) return
        callContext = context
        val bindingContext = callContext.trace.bindingContext
        val parameters = resolvedCall.candidateDescriptor.valueParameters.filter {
            (it.type.isFunctionType && it.type.hasComposableAnnotation()) || it.isSamComposable()
        }
        val arguments = parameters.map {
            val argument = resolvedCall.valueArguments.entries.firstOrNull { entry ->
                entry.key.original == it
            }?.value

            if (argument is ExpressionValueArgument) {
                argumentToInferenceNode(it, argument.valueArgument?.asElement() ?: reportOn)
            } else {
                // Generate a node that is ignored
                PsiElementNode(reportOn, bindingContext)
            }
        }
        infer.visitCall(
            call = PsiElementNode(reportOn, bindingContext),
            target = resolvedCallToInferenceNode(resolvedCall),
            arguments = arguments
        )
    }

    private fun resolvedCallToInferenceNode(resolvedCall: ResolvedCall<*>) =
        when (resolvedCall) {
            is VariableAsFunctionResolvedCall ->
                descriptorToInferenceNode(
                    resolvedCall.variableCall.candidateDescriptor,
                    resolvedCall.call.callElement
                )
            else -> {
                val receiver = resolvedCall.dispatchReceiver
                val expression = (receiver as? ExpressionReceiver)?.expression
                val referenceExpression = expression as? KtReferenceExpression
                val candidate = referenceExpression?.let { r ->
                    val callableReference =
                        callContext.trace[BindingContext.REFERENCE_TARGET, r] as?
                            CallableDescriptor
                    callableReference?.let { reference ->
                        descriptorToInferenceNode(reference, resolvedCall.call.callElement)
                    }
                }
                candidate ?: descriptorToInferenceNode(
                    resolvedCall.resultingDescriptor,
                    resolvedCall.call.callElement
                )
            }
        }

    private fun argumentToInferenceNode(
        descriptor: ValueParameterDescriptor,
        element: PsiElement
    ): InferenceNode {
        val bindingContext = callContext.trace.bindingContext
        val lambda = lambdaOrNull(element)
        if (lambda != null) return PsiElementNode(lambda, bindingContext)
        val parameter = findParameterReferenceOrNull(descriptor, element)
        if (parameter != null) return parameter
        return PsiElementNode(element, bindingContext)
    }

    private fun lambdaOrNull(element: PsiElement): KtFunctionLiteral? {
        var container = (element as? KtLambdaArgument)?.children?.singleOrNull()
        while (true) {
            container = when (container) {
                null -> return null
                is KtLabeledExpression -> container.lastChild
                is KtFunctionLiteral -> return container
                is KtLambdaExpression -> container.children.single()
                else -> throw Error("Unknown type: ${container.javaClass}")
            }
        }
    }

    private fun descriptorToInferenceNode(
        descriptor: CallableDescriptor,
        element: PsiElement
    ): InferenceNode = when (descriptor) {
        is ValueParameterDescriptor -> parameterDescriptorToInferenceNode(descriptor, element)
        else -> {
            // If this is a call to the accessor of the variable find the original descriptor
            val original = descriptor.original
            if (original is ValueParameterDescriptor)
                parameterDescriptorToInferenceNode(original, element)
            else ResolvedPsiElementNode(element, InferenceDescriptorType(descriptor))
        }
    }

    private fun parameterDescriptorToInferenceNode(
        descriptor: ValueParameterDescriptor,
        element: PsiElement
    ): InferenceNode {
        val parameter = findParameterReferenceOrNull(descriptor, element)
        return parameter ?: PsiElementNode(element, callContext.trace.bindingContext)
    }

    private fun findParameterReferenceOrNull(
        descriptor: ValueParameterDescriptor,
        element: PsiElement
    ): InferenceNode? {
        val bindingContext = callContext.trace.bindingContext
        val declaration = descriptor.containingDeclaration
        var currentContainer: InferenceNode? = containerNodeOf(element)
        while (currentContainer != null) {
            val type = currentContainer.type
            if (type.isTypeFor(declaration)) {
                val index =
                    declaration.valueParameters.filter {
                        it.isComposableCallable(bindingContext) ||
                            it.isSamComposable()
                    }.indexOf(descriptor)
                return ResolvedPsiParameterReference(
                    element,
                    InferenceDescriptorType(descriptor),
                    index,
                    currentContainer.element
                )
            }
            currentContainer = containerNodeOf(currentContainer.element)
        }
        return null
    }
}

private fun Annotated.schemeItem(): Item {
    val explicitTarget = compositionTarget()
    val explicitOpen = if (explicitTarget == null) compositionOpenTarget() else null
    return when {
        explicitTarget != null -> Token(explicitTarget)
        explicitOpen != null -> Open(explicitOpen)
        else -> Open(-1, isUnspecified = true)
    }
}

private fun Annotated.scheme(): Scheme? = compositionScheme()?.let { deserializeScheme(it) }

internal fun CallableDescriptor.toScheme(callContext: CallCheckerContext?): Scheme =
    scheme()
        ?: Scheme(
            target = schemeItem().let {
                // The item is unspecified see if the containing has an annotation we can use
                if (it.isUnspecified) {
                    val target = callContext?.let { context -> fileScopeTarget(context) }
                    if (target != null) return@let target
                }
                it
            },
            parameters = valueParameters.filter {
                it.type.hasComposableAnnotation() || it.isSamComposable()
            }.map {
                it.samComposableOrNull()?.toScheme(callContext) ?: it.type.toScheme()
            }
        ).mergeWith(overriddenDescriptors.map { it.toScheme(null) })

private fun CallableDescriptor.fileScopeTarget(callContext: CallCheckerContext): Item? =
    (psiElement?.containingFile as? KtFile)?.let {
        for (entry in it.annotationEntries) {
            val annotationDescriptor =
                callContext.trace.bindingContext[BindingContext.ANNOTATION, entry]
            annotationDescriptor?.compositionTarget()?.let { token ->
                return Token(token)
            }
        }
        null
    }

private fun KotlinType.toScheme(): Scheme = Scheme(
    target = schemeItem(),
    parameters = arguments.filter { it.type.hasComposableAnnotation() }.map { it.type.toScheme() }
)

private fun ValueParameterDescriptor.samComposableOrNull() =
    (type.constructor.declarationDescriptor as? ClassDescriptor)?.let {
        getSingleAbstractMethodOrNull(it)
    }

private fun ValueParameterDescriptor.isSamComposable() =
    samComposableOrNull()?.hasComposableAnnotation() == true

internal fun Scheme.mergeWith(schemes: List<Scheme>): Scheme {
    if (schemes.isEmpty()) return this

    val lazyScheme = LazyScheme(this)
    val bindings = lazyScheme.bindings

    fun unifySchemes(a: LazyScheme, b: LazyScheme) {
        bindings.unify(a.target, b.target)
        for ((ap, bp) in a.parameters.zip(b.parameters)) {
            unifySchemes(ap, bp)
        }
    }

    schemes.forEach {
        val overrideScheme = LazyScheme(it, bindings = lazyScheme.bindings)
        unifySchemes(lazyScheme, overrideScheme)
    }

    return lazyScheme.toScheme()
}