/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.k2

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.firTrace
import androidx.compose.compiler.plugins.kotlin.inference.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.overrides
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

private sealed class FirInferenceNode(val element: FirElement) {
    open val kind: NodeKind get() = NodeKind.Expression
    abstract val type: InferenceNodeType?
    open val referenceContainer: FirInferenceNode? get() = null
    open val parameterIndex: Int get() = -1
    override fun hashCode(): Int = 31 * element.hashCode()
    override fun equals(other: Any?): Boolean = other is FirInferenceNode && other.element == element
}

private open class FirElementInferenceNode(element: FirElement) : FirInferenceNode(element) {
    override val type: InferenceNodeType? get() = null
}

private class FirCallableElementInferenceNode(val callable: FirCallableSymbol<*>, element: FirElement) : FirElementInferenceNode(element) {
    override val type: InferenceNodeType = InferenceCallableType(callable)
    override fun toString(): String = "${callable.name.toString()}()@${element.source?.startOffset}"
}

private class FirFunctionInferenceNode(val function: FirFunction) : FirInferenceNode(function) {
    override val kind get() = NodeKind.Function
    override val type = InferenceCallableType(function.symbol)
    override fun toString(): String = function.symbol.name.toString()
}

private class FirLambdaInferenceNode(val lambda: FirAnonymousFunctionExpression): FirElementInferenceNode(lambda) {
    override val kind: NodeKind get() = NodeKind.Lambda
    override val type = InferenceCallableType(lambda.anonymousFunction.symbol)
    override fun toString() = "<lambda:${lambda.source?.startOffset}>"
}

private class FirSamInferenceNode(context: CheckerContext, val sam: FirSamConversionExpression): FirElementInferenceNode(sam) {
    override val kind: NodeKind get() = NodeKind.Lambda
    override val type: InferenceNodeType? =
        (sam.expression as? FirAnonymousFunctionExpression)?.let {
            InferenceCallableType(it.anonymousFunction.symbol)
        }

    override fun toString(): String = "<sam:${sam.source?.startOffset}>"
}

private class FirParameterReferenceNode(
    element: FirElement,
    override val parameterIndex: Int,
    override val referenceContainer: FirInferenceNode
) : FirElementInferenceNode(element) {
    override val kind: NodeKind get() = NodeKind.ParameterReference
    override fun toString(): String = "param:$parameterIndex"
}

private fun callableInferenceNodeOf(expression: FirElement, callable: FirCallableSymbol<*>, context: CheckerContext) =
    parameterInferenceNodeOrNull(expression, context) ?: (expression as? FirAnonymousFunction)?.let {
        mapping[expression]
    }?.let {
        inferenceNodeOf(it, context)
    } ?: FirCallableElementInferenceNode(callable, expression)

private sealed class InferenceNodeType {
    abstract fun toScheme(context: CheckerContext): Scheme
    abstract fun isTypeFor(callable: FirCallableSymbol<*>): Boolean
}

private class InferenceCallableType(val callable: FirCallableSymbol<*>) : InferenceNodeType() {
    override fun toScheme(context: CheckerContext): Scheme = callable.toScheme(context)
    override fun isTypeFor(callable: FirCallableSymbol<*>) = this.callable.callableId == callable.callableId
    override fun hashCode(): Int = 31 * callable.callableId.hashCode()
    override fun equals(other: Any?): Boolean =
        other is InferenceCallableType && other.callable.callableId == callable.callableId
}

fun FirCallableSymbol<*>.toScheme(context: CheckerContext): Scheme =
    declaredScheme(context) ?: Scheme(
        target = schemeItem(context).let {
            // The item is unspecified see if the containing has an annotation we can use
            if (it.isUnspecified) {
                val target = fileScopeTarget(context)
                if (target != null) return@let target
            }
            it
        },
        parameters = parameters(context).map { it.toScheme(context) }
    ).mergeWith(methodOverrides(context).map { it.toScheme(context) })

@OptIn(SymbolInternals::class)
fun FirCallableSymbol<*>.methodOverrides(context: CheckerContext) = (fir as? FirFunction)?.getDirectOverriddenFunctions(context) ?: emptyList()

fun FirCallableSymbol<*>.parameters(context: CheckerContext): List<FirValueParameterSymbol> =
    (this as? FirFunctionSymbol<*>)?.let {
        valueParameterSymbols.filter { it.isComposable(context) }
    } ?: emptyList()

@OptIn(SymbolInternals::class)
private fun FirCallableSymbol<*>.fileScopeTarget(context: CheckerContext): Item? {
    fun findFileScope(element: FirElement): Item? =
        (element as? FirAnnotationContainer)?.compositionTarget(context)?.let { Token(it) } ?: element.parent?.let { findFileScope(it) }
    return findFileScope(fir)
}

fun FirCallableSymbol<*>.declaredScheme(context: CheckerContext) =
    (annotationArgument(
        context,
        ComposeClassIds.ComposableInferredTarget,
        ComposeFqNames.ComposableInferredTargetSchemeArgument
    ) as? String)?.let {
        deserializeScheme(it)
    }

fun FirCallableSymbol<*>.schemeItem(context: CheckerContext): Item {
    val explicitTarget = compositionTarget(context)
    val explicitOpen = compositionOpenTarget(context)
    return when {
        explicitTarget != null -> Token(explicitTarget)
        explicitOpen != null -> Open(explicitOpen)
        else -> Open(-1, isUnspecified = true)
    }
}

fun FirCallableSymbol<*>.compositionTarget(context: CheckerContext): String? =
    annotationArgument(context, ComposeClassIds.ComposableTarget, ComposeFqNames.ComposableTargetApplierArgument) as? String ?: run {
        annotations.firstNotNullOfOrNull {
            it.resolvedType.targetName(context)
        }
    }

fun ConeKotlinType.targetName(context: CheckerContext): String? = toClassSymbol(context.session)?.let { cls ->
    cls.annotationArgument(context, ComposeClassIds.ComposableTargetMarker, ComposeFqNames.ComposableTargetMarkerDescriptionName)?.let {
        if (it is String && it != "") {
            it
        } else cls.classId.asFqNameString()
    }
}

fun FirCallableSymbol<*>.compositionOpenTarget(context: CheckerContext): Int? =
    annotationArgument(context, ComposeClassIds.ComposableOpenTarget, ComposeFqNames.ComposableOpenTargetIndexArgument) as? Int

fun FirBasedSymbol<*>.annotationArgument(context: CheckerContext, classId: ClassId, argumentName: Name) =
    getAnnotationByClassId(classId, context.session)?.argument(argumentName)

fun FirAnnotationContainer.compositionTarget(context: CheckerContext): String? =
    annotationArgument(context, ComposeClassIds.ComposableTarget, ComposeFqNames.ComposableTargetApplierArgument) as? String ?: run {
        annotations.firstNotNullOfOrNull {
            it.resolvedType.targetName(context)
        }
    }

fun FirAnnotationContainer.annotationArgument(context: CheckerContext, classId: ClassId, argumentName: Name) =
    getAnnotationByClassId(classId, context.session)?.argument(argumentName)

fun FirAnnotation.argument(name: Name): Any? = argumentMapping.mapping[name]?.let {
    if ((it.resolvedType.isString || it.resolvedType.isPrimitive) && it is FirLiteralExpression)
        it.value
    else null
}

object ComposableTargetChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()
            ?: return
        if (calleeFunction.isComposable(context)) {
            updateParents(context)
            val infer = getInfer(context, reporter)
            val call = inferenceNodeOf(expression, context)
            val target = callableInferenceNodeOf(expression, calleeFunction, context)
            val parameters = calleeFunction.parameters(context)
            val argumentsMapping = expression.resolvedArgumentMapping
            val arguments = parameters.mapNotNull { parameter ->
                argumentsMapping?.firstNotNullOf { entry ->
                    if (entry.value == parameter.fir)
                        inferenceNodeOf(entry.key, context)
                    else null
                }
            }
            infer.visitCall(call, target, arguments)
        }
    }
}

private class FirApplierInference(
    val context: CheckerContext,
    var reporter: DiagnosticReporter
) : ApplierInferencer<InferenceNodeType, FirInferenceNode>(
    typeAdapter = object : TypeAdapter<InferenceNodeType> {
        override fun declaredSchemaOf(type: InferenceNodeType): Scheme = type.toScheme(context)
        override fun currentInferredSchemeOf(type: InferenceNodeType): Scheme? = null
        override fun updatedInferredScheme(type: InferenceNodeType, scheme: Scheme) {}
    },
    nodeAdapter = object : NodeAdapter<InferenceNodeType, FirInferenceNode> {
        override fun containerOf(node: FirInferenceNode): FirInferenceNode {
            var current = node.element.parent
            while (current != null) {
                when (current) {
                    is FirFunction -> return inferenceNodeOf(current, context)
                }
                current = current.parent
            }
            return node
        }

        override fun kindOf(node: FirInferenceNode): NodeKind = node.kind

        override fun schemeParameterIndexOf(node: FirInferenceNode, container: FirInferenceNode): Int = node.parameterIndex

        override fun typeOf(node: FirInferenceNode): InferenceNodeType? = node.type

        override fun referencedContainerOf(node: FirInferenceNode): FirInferenceNode? = node.referenceContainer
    },
    errorReporter = object : ErrorReporter<FirInferenceNode> {
        private fun descriptionFrom(token: String): String = token // TODO: find the message if appropriate
        override fun reportCallError(node: FirInferenceNode, expected: String, received: String) {
            if (expected != received) {
                val expectedDescription = descriptionFrom(expected)
                val receivedDescription = descriptionFrom(received)
                reporter.reportOn(
                    source = node.element.source,
                    factory = ComposeErrors.COMPOSE_APPLIER_CALL_MISMATCH,
                    context = context,
                    a = expectedDescription,
                    b = receivedDescription
                )
            }
        }

        override fun reportParameterError(node: FirInferenceNode, index: Int, expected: String, received: String) {
            reporter.reportOn(
                source = node.element.source,
                factory = ComposeErrors.COMPOSE_APPLIER_PARAMETER_MISMATCH,
                context = context,
                a = expected,
                b = received
            )
        }

        override fun log(node: FirInferenceNode?, message: String) {
        }
    },
    lazySchemeStorage = object : LazySchemeStorage<FirInferenceNode> {
        override fun getLazyScheme(node: FirInferenceNode): LazyScheme? =
            lazySchemes[node]

        override fun storeLazyScheme(node: FirInferenceNode, value: LazyScheme) {
            lazySchemes[node] = value
        }
    }
)

private var lazySchemes = mutableMapOf<FirInferenceNode, LazyScheme>()

/**
 * A map of elements that, for inference, needed to be treated as if they are identical such as lambdas
 * and the anonymous function as well as sam conversions and the expression converted.
 */
private var mapping = mutableMapOf<FirElement, FirElement>()

private fun inferenceNodeOf(element: FirElement, context: CheckerContext): FirInferenceNode =
    element.firTrace[WritableSlices.NODE, element] ?: when (element) {
        is FirAnonymousFunctionExpression -> run {
            mapping[element.anonymousFunction] = element
            FirLambdaInferenceNode(element)
        }
        is FirSamConversionExpression -> run {
            (element.expression as? FirAnonymousFunctionExpression)?.let {
                mapping[it.anonymousFunction] = element
            }
            FirSamInferenceNode(context, element)
        }
        is FirAnonymousFunction -> callableInferenceNodeOf(element, element.symbol, context)
        is FirFunction ->
            FirFunctionInferenceNode(element)
        else -> parameterInferenceNodeOrNull(element, context) ?: FirElementInferenceNode(element)
    }.also {
        element.firTrace.record(WritableSlices.NODE, element, it)
    }


@OptIn(SymbolInternals::class)
private fun parameterInferenceNodeOrNull(expression: FirElement, context: CheckerContext): FirInferenceNode? {
    if (expression is FirFunctionCall) {
        val receiver = expression.explicitReceiver as? FirQualifiedAccessExpression ?: return null
        val parameterSymbol = receiver.toResolvedCallableSymbol() as? FirValueParameterSymbol ?: return null
        val function = parameterSymbol.containingFunctionSymbol
        val index = function.valueParameterSymbols.filter { it.isComposable(context) }.indexOf(parameterSymbol)
        if (index >= 0) {
            return FirParameterReferenceNode(expression, index, inferenceNodeOf(function.fir, context))
        }
    }
    return null
}

private fun getInfer(context: CheckerContext, reporter: DiagnosticReporter): FirApplierInference {
    return (context.firTrace[WritableSlices.INFER, context] ?: run { FirApplierInference(context, reporter) }).also {
        it.reporter = reporter
    }
}

private fun updateParents(context: CheckerContext) {
    val containingElements = context.containingElements
    for (i in (1..<containingElements.size).reversed()) {
        val element = containingElements[i]
        if (element.parent != null) break
        val parent = containingElements[i - 1]
        element.firTrace.record(WritableSlices.PARENT, element, parent)
    }
}

private val FirElement.parent get() = firTrace[WritableSlices.PARENT, this]

private object WritableSlices {
    val INFER: WritableSlice<CheckerContext, FirApplierInference> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val LAZY_SCHEME: WritableSlice<FirElement, LazyScheme> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val NODE: WritableSlice<FirElement, FirInferenceNode> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val PARENT: WritableSlice<FirElement, FirElement?> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
