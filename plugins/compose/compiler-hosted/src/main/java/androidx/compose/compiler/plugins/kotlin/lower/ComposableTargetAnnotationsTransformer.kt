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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.KtxNameConventions
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.inference.ApplierInferencer
import androidx.compose.compiler.plugins.kotlin.inference.ErrorReporter
import androidx.compose.compiler.plugins.kotlin.inference.TypeAdapter
import androidx.compose.compiler.plugins.kotlin.inference.Item
import androidx.compose.compiler.plugins.kotlin.inference.LazyScheme
import androidx.compose.compiler.plugins.kotlin.inference.LazySchemeStorage
import androidx.compose.compiler.plugins.kotlin.inference.NodeAdapter
import androidx.compose.compiler.plugins.kotlin.inference.NodeKind
import androidx.compose.compiler.plugins.kotlin.inference.Open
import androidx.compose.compiler.plugins.kotlin.inference.Scheme
import androidx.compose.compiler.plugins.kotlin.inference.Token
import androidx.compose.compiler.plugins.kotlin.inference.deserializeScheme
import androidx.compose.compiler.plugins.kotlin.irTrace
import androidx.compose.compiler.plugins.kotlin.mergeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.toBuilder
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This transformer walks the IR tree to infer the applier annotations such as ComposableTarget,
 * ComposableOpenTarget, and
 */
class ComposableTargetAnnotationsTransformer(
    context: IrPluginContext,
    symbolRemapper: ComposableSymbolRemapper,
    metrics: ModuleMetrics
) : AbstractComposeLowering(context, symbolRemapper, metrics) {
    private val ComposableTargetClass = symbolRemapper.getReferencedClassOrNull(
        getTopLevelClassOrNull(ComposeFqNames.ComposableTarget)
    )
    private val ComposableOpenTargetClass = symbolRemapper.getReferencedClassOrNull(
        getTopLevelClassOrNull(ComposeFqNames.ComposableOpenTarget)
    )
    private val ComposableInferredTargetClass = symbolRemapper.getReferencedClassOrNull(
        getTopLevelClassOrNull(ComposeFqNames.ComposableInferredTarget)
    )

    /**
     * A map of element to the owning function of the element.
     */
    private val ownerMap = mutableMapOf<IrElement, IrFunction>()

    /**
     * Map of a parameter symbol to its function and parameter index.
     */
    private val parameterOwners = mutableMapOf<IrSymbol, Pair<IrFunction, Int>>()

    /**
     * A map of variables to their corresponding inference node.
     */
    private val variableDeclarations = mutableMapOf<IrSymbol, InferenceVariable>()

    private var currentOwner: IrFunction? = null
    private var currentFile: IrFile? = null

    private val transformer get() = this

    private fun lineInfoOf(element: IrElement?): String {
        val file = currentFile
        if (element != null && file != null) {
            return " ${file.name}:${
                file.fileEntry.getLineNumber(element.startOffset) + 1
            }:${
                file.fileEntry.getColumnNumber(element.startOffset) + 1
            }"
        }
        return ""
    }

    private val infer = ApplierInferencer(
        typeAdapter = object : TypeAdapter<InferenceFunction> {
            val current = mutableMapOf<InferenceFunction, Scheme>()
            override fun declaredSchemaOf(type: InferenceFunction): Scheme =
                type.toDeclaredScheme().also {
                    type.recordScheme(it)
                }

            override fun currentInferredSchemeOf(type: InferenceFunction): Scheme? =
                if (type.schemeIsUpdatable) current[type] ?: declaredSchemaOf(type) else null

            override fun updatedInferredScheme(type: InferenceFunction, scheme: Scheme) {
                type.recordScheme(scheme)
                type.updateScheme(scheme)
                current[type] = scheme
            }
        },
        nodeAdapter = object : NodeAdapter<InferenceFunction, InferenceNode> {
            override fun containerOf(node: InferenceNode): InferenceNode =
                ownerMap[node.element]?.let {
                    inferenceNodeOf(it, transformer)
                } ?: (node as? InferenceResolvedParameter)?.referenceContainer ?: node

            override fun kindOf(node: InferenceNode): NodeKind = node.kind

            override fun schemeParameterIndexOf(
                node: InferenceNode,
                container: InferenceNode
            ): Int = node.parameterIndex(container)

            override fun typeOf(node: InferenceNode): InferenceFunction? = node.function

            override fun referencedContainerOf(node: InferenceNode): InferenceNode? =
                node.referenceContainer
        },
        lazySchemeStorage = object : LazySchemeStorage<InferenceNode> {
            // The transformer is transitory so we can just store this in a map.
            val map = mutableMapOf<InferenceNode, LazyScheme>()
            override fun getLazyScheme(node: InferenceNode): LazyScheme? = map[node]
            override fun storeLazyScheme(node: InferenceNode, value: LazyScheme) {
                map[node] = value
            }
        },
        errorReporter = object : ErrorReporter<InferenceNode> {
            override fun reportCallError(node: InferenceNode, expected: String, received: String) {
                // Ignored, should be reported by the front-end
            }

            override fun reportParameterError(
                node: InferenceNode,
                index: Int,
                expected: String,
                received: String
            ) {
                // Ignored, should be reported by the front-end
            }

            override fun log(node: InferenceNode?, message: String) {
                val element = node?.element
                if (!metrics.isEmpty)
                    metrics.log("applier inference${lineInfoOf(element)}: $message")
            }
        }
    )

    override fun lower(module: IrModuleFragment) {
        // Only transform if the attributes being inferred are in the runtime
        if (
            ComposableTargetClass != null &&
            ComposableInferredTargetClass != null &&
            ComposableOpenTargetClass != null
        ) {
            module.transformChildrenVoid(this)
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        includeFileNameInExceptionTrace(declaration) {
            currentFile = declaration
            return super.visitFile(declaration).also { currentFile = null }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (
            declaration.hasSchemeSpecified() ||
            (!declaration.isComposable && !declaration.hasComposableParameter()) ||
            declaration.hasOverlyWideParameters() ||
            declaration.hasOpenTypeParameters()
        ) {
            return super.visitFunction(declaration)
        }
        val oldOwner = currentOwner
        currentOwner = declaration
        var currentParameter = 0
        fun recordParameter(parameter: IrValueParameter) {
            if (parameter.type.isOrHasComposableLambda) {
                parameterOwners[parameter.symbol] = declaration to currentParameter++
            }
        }
        declaration.valueParameters.forEach { recordParameter(it) }
        declaration.extensionReceiverParameter?.let { recordParameter(it) }

        val result = super.visitFunction(declaration)
        currentOwner = oldOwner
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        if (declaration.type.isOrHasComposableLambda) {
            currentOwner?.let { ownerMap[declaration] = it }

            val initializerNode = declaration.initializer
            if (initializerNode != null) {
                val initializer = resolveExpressionOrNull(initializerNode)
                    ?: InferenceElementExpression(transformer, initializerNode)
                val variable = InferenceVariable(this, declaration)
                variableDeclarations[declaration.symbol] = variable
                infer.visitVariable(variable, initializer)
            }
        }

        return super.visitVariable(declaration)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        val result = super.visitLocalDelegatedProperty(declaration)
        if (declaration.type.isOrHasComposableLambda) {
            // Find the inference variable for the delegate which should have been created
            // when visiting the delegate node. If not, then ignore this declaration
            val variable = variableDeclarations[declaration.delegate.symbol] ?: return result

            // Allow the variable to be found from the getter as this is what is used to access
            // the variable, not the delegate directly.
            variableDeclarations[declaration.getter.symbol] = variable
        }
        return result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val owner = currentOwner
        if (
            owner == null || (
                    !expression.isTransformedComposableCall() &&
                    !expression.hasComposableArguments()
                ) || when (expression.symbol.owner.fqNameWhenAvailable) {
                    ComposeFqNames.getCurrentComposerFullName,
                    ComposeFqNames.composableLambdaFullName -> true
                    else -> false
                }

        ) {
            return super.visitCall(expression)
        }
        ownerMap[expression] = owner
        val result = super.visitCall(expression)

        val target = (
            if (
                expression.isInvoke() ||
                expression.dispatchReceiver?.type?.isSamComposable == true
            ) {
                expression.dispatchReceiver?.let {
                    resolveExpressionOrNull(it)
                }
            } else resolveExpressionOrNull(expression)
            ) ?: InferenceCallTargetNode(this, expression)
        if (target.isOverlyWide()) return result

        val arguments = expression.arguments.filterIndexed { index, argument ->
            argument?.let {
                it.isComposableLambda || it.isComposableParameter || (
                    if (
                    // There are three cases where the expression type is not good enough here,
                    // one, the type is a default parameter and there is no actual expression
                    // and, two, when the expression is a SAM conversion where the type is
                    // too specific (it is the class) and we need the SAM interface, and three
                    // the value is null for a nullable type.
                    (
                        argument is IrContainerExpression &&
                            argument.origin == IrStatementOrigin.DEFAULT_VALUE
                        ) || (
                            argument is IrBlock
                        ) || (
                            argument.isNullConst()
                        )
                ) {
                    // If the parameter is a default value, grab the type from the function
                    // being called.
                    expression.symbol.owner.valueParameters.let { parameters ->
                        if (index < parameters.size) parameters[index].type else null
                    }
                } else it.type)?.isOrHasComposableLambda == true
            } == true
        }.filterNotNull().toMutableList()
        fun recordArgument(argument: IrExpression?) {
            if (
                argument != null && (
                    argument.isComposableLambda ||
                    argument.isComposableParameter ||
                    argument.type.isOrHasComposableLambda
                )
            ) {
                arguments.add(argument)
            }
        }

        recordArgument(expression.extensionReceiver)

        infer.visitCall(
            call = inferenceNodeOf(expression, transformer),
            target = target,
            arguments = arguments.map {
                resolveExpressionOrNull(it) ?: inferenceNodeOf(it, transformer)
            }
        )

        return result
    }

    private fun inferenceParameterOrNull(getValue: IrGetValue): InferenceResolvedParameter? =
        parameterOwners[getValue.symbol]?.let {
            InferenceResolvedParameter(
                getValue,
                inferenceFunctionOf(it.first),
                inferenceNodeOf(it.first, this),
                it.second
            )
        }

    /**
     * Resolve references to local variables and parameters.
     */
    private fun resolveExpressionOrNull(expression: IrElement?): InferenceNode? =
        when (expression) {
            is IrGetValue ->
                // Get the inference node for referencing a local variable or parameter if this
                // expression does.
                inferenceParameterOrNull(expression) ?: variableDeclarations[expression.symbol]
            is IrCall ->
                // If this call is a call to the getter of a local delegate get the inference
                // node of the delegate.
                variableDeclarations[expression.symbol]
            else -> null
        }

    val List<IrConstructorCall>.target: Item get() =
        firstOrNull { it.isComposableTarget }?.let { constructor ->
            constructor.firstParameterOrNull<String>()?.let { Token(it) }
        } ?: firstOrNull { it.isComposableOpenTarget }?.let { constructor ->
            constructor.firstParameterOrNull<Int>()?.let { Open(it) }
        } ?: firstOrNull { it.isComposableTargetMarked }?.let { constructor ->
            val fqName = constructor.symbol.owner.parentAsClass.fqNameWhenAvailable
            fqName?.let {
                Token(it.asString())
            }
        } ?: Open(-1, isUnspecified = true)

    val IrFunction.scheme: Scheme? get() =
        annotations.firstOrNull { it.isComposableInferredTarget }?.let { constructor ->
            constructor.firstParameterOrNull<String>()?.let {
                deserializeScheme(it)
            }
        }

    fun IrFunction.hasSchemeSpecified(): Boolean =
        annotations.any {
            it.isComposableTarget || it.isComposableOpenTarget || it.isComposableInferredTarget ||
                it.isComposableTargetMarked
        }

    fun IrType.toScheme(defaultTarget: Item): Scheme =
        when {
            this is IrSimpleType && isFunction() -> arguments
            else -> emptyList()
        }.let { typeArguments ->
            val target = annotations.target.let {
                if (it.isUnspecified) defaultTarget else it
            }

            fun toScheme(argument: IrTypeArgument): Scheme? =
                if (argument is IrTypeProjection && argument.type.isOrHasComposableLambda)
                    argument.type.toScheme(defaultTarget)
                else null

            val parameters = typeArguments.takeUpTo(typeArguments.size - 1).mapNotNull { argument ->
                toScheme(argument)
            }

            val result = typeArguments.lastOrNull()?.let { argument ->
                toScheme(argument)
            }

            Scheme(target, parameters, result)
        }

    private val IrElement?.isComposableLambda: Boolean get() = when (this) {
        is IrFunctionExpression -> function.isComposable
        is IrCall -> isComposableSingletonGetter() || hasTransformedLambda()
        is IrGetField -> symbol.owner.initializer?.findTransformedLambda() != null
        else -> false
    }

    private val IrElement?.isComposableParameter: Boolean get() = when (this) {
        is IrGetValue -> parameterOwners[symbol] != null && type.isComposable
        else -> false
    }

    internal fun IrCall.hasTransformedLambda() =
        context.irTrace[ComposeWritableSlices.HAS_TRANSFORMED_LAMBDA, this] == true

    private fun IrElement.findTransformedLambda(): IrFunctionExpression? =
        when (this) {
            is IrCall -> arguments.firstNotNullOfOrNull { it?.findTransformedLambda() }
            is IrGetField -> symbol.owner.initializer?.findTransformedLambda()
            is IrBody -> statements.firstNotNullOfOrNull { it.findTransformedLambda() }
            is IrReturn -> value.findTransformedLambda()
            is IrFunctionExpression -> if (isTransformedLambda()) this else null
            else -> null
        }

    private fun IrFunctionExpression.isTransformedLambda() =
        context.irTrace[ComposeWritableSlices.IS_TRANSFORMED_LAMBDA, this] == true

    internal fun IrElement.transformedLambda(): IrFunctionExpression =
        findTransformedLambda() ?: error("Could not find the lambda for ${dump()}")

    // If this function throws an error it is because the IR transformation for singleton functions
    // changed. This function should be updated to account for the change.
    internal fun IrCall.singletonFunctionExpression(): IrFunctionExpression =
        symbol.owner.body?.findTransformedLambda()
            ?: error("Could not find the singleton lambda for ${dump()}")

    private fun Item.toAnnotation(): IrConstructorCall? =
        if (ComposableTargetClass != null && ComposableOpenTargetClass != null) {
            when (this) {
                is Token -> annotation(ComposableTargetClass).also {
                    it.putValueArgument(0, irConst(value))
                }
                is Open ->
                    if (index < 0) null else annotation(
                        ComposableOpenTargetClass
                    ).also {
                        it.putValueArgument(0, irConst(index))
                    }
            }
        } else null

    private fun Item.toAnnotations(): List<IrConstructorCall> =
        toAnnotation()?.let { listOf(it) } ?: emptyList()

    private fun Scheme.toAnnotations(): List<IrConstructorCall> =
        if (ComposableInferredTargetClass != null) {
            listOf(
                annotation(ComposableInferredTargetClass).also {
                    it.putValueArgument(0, irConst(serialize()))
                }
            )
        } else emptyList()

    private fun annotation(classSymbol: IrClassSymbol) =
        IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            classSymbol.defaultType,
            classSymbol.constructors.first(),
            0,
            0,
            1,
            null
        )

    private fun filteredAnnotations(annotations: List<IrConstructorCall>) = annotations
        .filter {
            !it.isComposableTarget &&
                !it.isComposableOpenTarget &&
                !it.isComposableInferredTarget
        }

    fun updatedAnnotations(annotations: List<IrConstructorCall>, target: Item) =
        filteredAnnotations(annotations) + target.toAnnotations()

    fun updatedAnnotations(annotations: List<IrConstructorCall>, scheme: Scheme) =
        filteredAnnotations(annotations) + scheme.toAnnotations()

    fun inferenceFunctionOf(function: IrFunction) =
        InferenceFunctionDeclaration(this, function)

    fun inferenceFunctionTypeOf(type: IrType) =
        InferenceFunctionType(this, type)

    /**
     * A function is composable if it has a composer parameter added by the
     * [ComposerParamTransformer] or it still has the @Composable annotation which
     * can be because it is external and hasn't been transformed as the symbol remapper
     * only remaps what is referenced as a symbol this method might not have been
     * referenced directly in this module.
     */
    private val IrFunction.isComposable get() =
        valueParameters.any { it.name == KtxNameConventions.COMPOSER_PARAMETER } ||
            annotations.hasAnnotation(ComposeFqNames.Composable)

    private val IrType.isSamComposable get() =
        samOwnerOrNull()?.isComposable == true

    private val IrType.isComposableLambda get() =
        (this.classFqName == ComposeFqNames.composableLambdaType) ||
        (this as? IrSimpleType)
        ?.arguments
        ?.any {
            it.typeOrNull?.classFqName == ComposeFqNames.Composer
        } == true

    internal val IrType.isOrHasComposableLambda: Boolean get() =
        isComposableLambda || isSamComposable ||
            (this as? IrSimpleType)?.arguments?.any {
                it.typeOrNull?.isOrHasComposableLambda == true
            } == true

    private val IrType.isComposable get() = isComposableLambda || isSamComposable

    private fun IrFunction.hasComposableParameter() =
        valueParameters.any { it.type.isComposable }

    private fun IrCall.hasComposableArguments() =
        arguments.any { argument ->
            argument?.type?.let { type ->
                (type.isOrHasComposableLambda || type.isSamComposable)
            } == true
        }
}

/**
 * An [InferenceFunction] is an abstraction to allow inference to translate a type into a scheme
 * and update the declaration of a type if inference determines a more accurate scheme.
 */
sealed class InferenceFunction(
    val transformer: ComposableTargetAnnotationsTransformer
) {
    /**
     * The name of the function. This is only supplied for debugging.
     */
    abstract val name: String

    /**
     * Can the scheme be updated. If not, tell inference not to track it.
     */
    abstract val schemeIsUpdatable: Boolean

    /**
     * Record a scheme for the function in metrics (if applicable).
     */
    open fun recordScheme(scheme: Scheme) { }

    /**
     * The scheme has changed so the corresponding attributes should be updated to match the
     * scheme provided.
     */
    abstract fun updateScheme(scheme: Scheme)

    /**
     * Return a declared scheme for the function.
     */
    abstract fun toDeclaredScheme(defaultTarget: Item = Open(0)): Scheme

    /**
     * Return true if this is a type with overly wide parameter types such as Any or
     * unconstrained or insufficiently constrained type parameters.
     */
    open fun isOverlyWide(): Boolean = false

    /**
     * Helper routine to produce an updated annotations list.
     */
    fun updatedAnnotations(annotations: List<IrConstructorCall>, target: Item) =
        transformer.updatedAnnotations(annotations, target)

    /**
     * Helper routine to produce an updated annotations list.
     */
    fun updatedAnnotations(annotations: List<IrConstructorCall>, scheme: Scheme) =
        transformer.updatedAnnotations(annotations, scheme)
}

/**
 * An [InferenceFunctionDeclaration] refers to the type implied by a function declaration.
 *
 * Storing [Scheme] information is complicated by the current IR transformer limitation that
 * annotations added to types are not serialized. Instead of updating the parameter types
 * directly (for example adding a annotation to the IrType of the parameter declaration) the
 * [Scheme] is serialized into a string and stored on the function declaration.
 */
class InferenceFunctionDeclaration(
    transformer: ComposableTargetAnnotationsTransformer,
    val function: IrFunction
) : InferenceFunction(transformer) {
    override val name: String get() = function.name.toString()

    override val schemeIsUpdatable: Boolean get() = true

    override fun recordScheme(scheme: Scheme) {
        if (!scheme.allAnonymous()) {
            with(transformer) {
                metricsFor(function).recordScheme(scheme.toString())
            }
        }
    }

    override fun updateScheme(scheme: Scheme) {
        if (scheme.shouldSerialize) {
            function.annotations = updatedAnnotations(function.annotations, scheme)
        } else {
            function.annotations = updatedAnnotations(function.annotations, scheme.target)
            parameters().zip(scheme.parameters) { parameter, parameterScheme ->
                parameter.updateScheme(parameterScheme)
            }
        }
    }

    override fun toDeclaredScheme(defaultTarget: Item): Scheme = with(transformer) {
        function.scheme ?: function.toScheme(defaultTarget)
    }

    private fun IrFunction.toScheme(defaultTarget: Item): Scheme = with(transformer) {
        val target = function.annotations.target.let { target ->
            if (target.isUnspecified && function.body == null) {
                defaultTarget
            } else if (target.isUnspecified) {
                // Default to the target specified at the file scope, if one.
                function.file.annotations.target
            } else target
        }
        val effectiveDefault =
            if (function.body == null) defaultTarget
            else Open(-1, isUnspecified = true)
        val result = function.returnType.let { resultType ->
            if (resultType.isOrHasComposableLambda)
                resultType.toScheme(effectiveDefault)
            else null
        }

        Scheme(
            target,
            parameters().map { it.toDeclaredScheme(effectiveDefault) },
            result
        ).let { scheme ->
            ancestorScheme(defaultTarget)?.let { scheme.mergeWith(listOf(it)) } ?: scheme
        }
    }

    private fun IrFunction.ancestorScheme(defaultTarget: Item): Scheme? =
        if (this is IrSimpleFunction && this.overriddenSymbols.isNotEmpty()) {
            getLastOverridden().toScheme(defaultTarget)
        } else null

    override fun hashCode(): Int = function.hashCode() * 31
    override fun equals(other: Any?) =
        other is InferenceFunctionDeclaration && other.function == function

    private fun parameters(): List<InferenceFunction> =
        with(transformer) {
            function.valueParameters.filter { it.type.isOrHasComposableLambda }.map { parameter ->
                InferenceFunctionParameter(transformer, parameter)
            }.let { parameters ->
                function.extensionReceiverParameter?.let {
                    if (it.type.isOrHasComposableLambda) {
                        parameters + listOf(InferenceFunctionParameter(transformer, it))
                    } else parameters
                } ?: parameters
            }
        }

    private val Scheme.shouldSerialize get(): Boolean = parameters.isNotEmpty()
    private fun Scheme.allAnonymous(): Boolean = target.isAnonymous &&
        (result == null || result.allAnonymous()) &&
        parameters.all { it.allAnonymous() }
}

/**
 * An [InferenceFunctionCallType] is the type abstraction for a call. This is used for [IrCall]
 * because it has the substituted types for generic types and the function's symbol has the original
 * unsubstituted types. It is important, for example for calls to [let], that the arguments
 * and result are after generic resolution so calls like `content?.let { it.invoke() }` correctly
 * infer that the scheme is `[0[0]]` if `content` is a of type `(@Composable () -> Unit)?. This
 * can only be determined after the generic parameters have been substituted.
 */
class InferenceFunctionCallType(
    transformer: ComposableTargetAnnotationsTransformer,
    private val call: IrCall
) : InferenceFunction(transformer) {
    override val name: String get() = "Call(${call.symbol.owner.name})"

    override val schemeIsUpdatable: Boolean get() = false

    override fun toDeclaredScheme(defaultTarget: Item): Scheme =
        with(transformer) {
            val target = call.symbol.owner.annotations.target.let { target ->
                if (target.isUnspecified) defaultTarget else target
            }
            val parameters = call.arguments.filterNotNull().filter {
                 it.type.isOrHasComposableLambda
            }.map {
                it.type.toScheme(defaultTarget)
            }.toMutableList()
            fun recordParameter(expression: IrExpression?) {
                if (expression != null && expression.type.isOrHasComposableLambda) {
                    parameters.add(expression.type.toScheme(defaultTarget))
                }
            }
            recordParameter(call.extensionReceiver)
            val result = if (call.type.isOrHasComposableLambda)
                call.type.toScheme(defaultTarget)
            else null
            Scheme(target, parameters, result)
        }

    override fun isOverlyWide(): Boolean =
        call.symbol.owner.hasOverlyWideParameters()

    override fun updateScheme(scheme: Scheme) {
        // Ignore the updated scheme for the call as it can always be re-inferred.
    }
}

/**
 * Produce the scheme from a function type.
 */
class InferenceFunctionType(
    transformer: ComposableTargetAnnotationsTransformer,
    private val type: IrType
) : InferenceFunction(transformer) {
    override val name: String get() = "<type>"
    override val schemeIsUpdatable: Boolean get() = false
    override fun toDeclaredScheme(defaultTarget: Item): Scheme = with(transformer) {
        type.toScheme(defaultTarget)
    }

    override fun updateScheme(scheme: Scheme) {
        // Cannot update the scheme of a type yet. This is worked around for parameters by recording
        // the inferred scheme for the parameter in a serialized scheme for the function. For other
        // types we don't need to record the inference we just need the declared scheme.
    }
}

/**
 * The type of a function parameter. The parameter of a function needs to update where it came from.
 */
class InferenceFunctionParameter(
    transformer: ComposableTargetAnnotationsTransformer,
    val parameter: IrValueParameter
) : InferenceFunction(transformer) {
    override val name: String get() = "<parameter>"
    override fun hashCode(): Int = parameter.hashCode() * 31
    override fun equals(other: Any?) =
        other is InferenceFunctionParameter && other.parameter == parameter

    override val schemeIsUpdatable: Boolean get() = false

    override fun toDeclaredScheme(defaultTarget: Item): Scheme = with(transformer) {
        val samAnnotations = parameter.type.samOwnerOrNull()?.annotations ?: emptyList()
        val annotations = parameter.type.annotations + samAnnotations
        val target = annotations.target.let { if (it.isUnspecified) defaultTarget else it }
        parameter.type.toScheme(target)
    }

    override fun updateScheme(scheme: Scheme) {
        // Note that this is currently not called. Type annotations are serialized into an
        // ComposableInferredAnnotation. This is kept here as example of how the type should
        // be updated once such a modification is correctly serialized by Kotlin.
        val type = parameter.type
        if (type is IrSimpleType) {
            val newType = type.toBuilder().apply {
                annotations = updatedAnnotations(annotations, scheme.target)
            }.buildSimpleType()
            parameter.type = newType
        }
    }
}

/**
 * A wrapper around IrElement to return the information requested by inference.
 */
sealed class InferenceNode {
    /**
     * The element being wrapped
     */
    abstract val element: IrElement

    /**
     * The node kind of the node
     */
    abstract val kind: NodeKind

    /**
     * The function type abstraction used by inference
     */
    abstract val function: InferenceFunction?

    /**
     * The container node being referred to by the this node, if there is one. For example, if this
     * is a parameter reference then this is the function that contains the parameter (which
     * parameterIndex can be used to determine the parameter index). If it is a call to a static
     * function then the reference is to the IrFunction that is being called.
     */
    open val referenceContainer: InferenceNode? = null

    /**
     * [node] is one of the parameters of this container node then return its index. -1 indicates
     * that [node] is not a parameter of this container (or this is not a container).
     */
    open fun parameterIndex(node: InferenceNode): Int = -1

    /**
     * An overly wide function (a function with Any types) is too wide to use for to infer an
     * applier (that is it contains parameters of type Any or Any?).
     */
    open fun isOverlyWide(): Boolean = function?.isOverlyWide() == true

    override fun hashCode() = element.hashCode() * 31
    override fun equals(other: Any?) = other is InferenceNode && other.element == element
}

val IrSimpleFunctionSymbol.isGenericFunction get(): Boolean =
    owner.typeParameters.isNotEmpty() || owner.dispatchReceiverParameter?.type?.let {
        it is IrSimpleType && it.arguments.isNotEmpty()
    } == true

/**
 * An [InferenceCallTargetNode] is a wrapper around an [IrCall] which represents the target of
 * the call, not the call itself. That its type is the type of the target of the call not the
 * result of the call.
 */
class InferenceCallTargetNode(
    private val transformer: ComposableTargetAnnotationsTransformer,
    override val element: IrCall
) : InferenceNode() {
    override fun equals(other: Any?): Boolean =
        other is InferenceCallTargetNode && super.equals(other)
    override fun hashCode(): Int = super.hashCode() * 31
    override val kind: NodeKind get() = NodeKind.Function
    override val function = with(transformer) {
        if (element.symbol.owner.hasSchemeSpecified())
            InferenceFunctionDeclaration(transformer, element.symbol.owner)
        else InferenceFunctionCallType(transformer, element)
    }

    override val referenceContainer: InferenceNode? =
        // If this is a generic function then don't redirect the scheme to the declaration
        if (element.symbol.isGenericFunction) null else
        with(transformer) {
            val function = when {
                element.isComposableSingletonGetter() ->
                    // If this was a lambda transformed into a singleton, find the singleton function
                    element.singletonFunctionExpression().function
                element.hasTransformedLambda() ->
                    // If this is a normal lambda, find the lambda's IrFunction
                    element.transformedLambda().function
                else -> element.symbol.owner
            }
            // If this is a call to a non-generic function with a body (e.g. non-abstract), return its
            // function. Generic or abstract functions (interface members, lambdas, open methods, etc.)
            // do not contain a body to infer anything from so we just use the declared scheme if
            // there is one. Returning null from this function cause the scheme to be determined from
            // the target expression (using, for example, the substituted type parameters) instead of
            // the definition.
            function.takeIf { it.body != null && it.typeParameters.isEmpty() }?.let {
                inferenceNodeOf(function, transformer)
            }
        }
}

/**
 * A node representing a variable declaration.
 */
class InferenceVariable(
    private val transformer: ComposableTargetAnnotationsTransformer,
    override val element: IrVariable,
) : InferenceNode() {
    override val kind: NodeKind get() = NodeKind.Variable
    override val function: InferenceFunction get() =
        transformer.inferenceFunctionTypeOf(element.type)
    override val referenceContainer: InferenceNode? get() = null
}

fun inferenceNodeOf(
    element: IrElement,
    transformer: ComposableTargetAnnotationsTransformer
): InferenceNode =
    when (element) {
        is IrFunction -> InferenceFunctionDeclarationNode(transformer, element)
        is IrFunctionExpression -> InferenceFunctionExpressionNode(transformer, element)
        is IrTypeOperatorCall -> inferenceNodeOf(element.argument, transformer)
        is IrCall -> InferenceCallExpression(transformer, element)
        is IrExpression -> InferenceElementExpression(transformer, element)
        else -> InferenceUnknownElement(element)
    }

/**
 * A node wrapper for function declarations.
 */
class InferenceFunctionDeclarationNode(
    transformer: ComposableTargetAnnotationsTransformer,
    override val element: IrFunction
) : InferenceNode() {
    override val kind: NodeKind get() = NodeKind.Function
    override val function: InferenceFunction = transformer.inferenceFunctionOf(element)
    override val referenceContainer: InferenceNode?
        get() = this.takeIf { element.body != null }
}

/**
 * A node wrapper for function expressions (i.e. lambdas).
 */
class InferenceFunctionExpressionNode(
    private val transformer: ComposableTargetAnnotationsTransformer,
    override val element: IrFunctionExpression
) : InferenceNode() {
    override val kind: NodeKind get() = NodeKind.Lambda
    override val function: InferenceFunction = transformer.inferenceFunctionOf(element.function)
    override val referenceContainer: InferenceNode
        get() = inferenceNodeOf(element.function, transformer)
}

/**
 * A node wrapper for a call. This represents the result of a call. Use [InferenceCallTargetNode]
 * to represent the target of a call.
 */
class InferenceCallExpression(
    private val transformer: ComposableTargetAnnotationsTransformer,
    override val element: IrCall
) : InferenceNode() {
    private val isSingletonLambda = with(transformer) { element.isComposableSingletonGetter() }
    private val isTransformedLambda = with(transformer) { element.hasTransformedLambda() }
    override val kind: NodeKind get() =
        if (isSingletonLambda || isTransformedLambda) NodeKind.Lambda else NodeKind.Expression

    override val function: InferenceFunction = with(transformer) {
        when {
            isSingletonLambda ->
                inferenceFunctionOf(element.singletonFunctionExpression().function)
            isTransformedLambda ->
                inferenceFunctionOf(element.transformedLambda().function)
            else -> transformer.inferenceFunctionTypeOf(element.type)
        }
    }

    override val referenceContainer: InferenceNode?
        get() = with(transformer) {
            when {
                isSingletonLambda ->
                    inferenceNodeOf(element.singletonFunctionExpression().function, transformer)
                isTransformedLambda ->
                    inferenceNodeOf(element.transformedLambda().function, transformer)
                else -> null
            }
        }
}

/**
 * An expression node whose scheme is determined by the type of the node.
 */
class InferenceElementExpression(
    transformer: ComposableTargetAnnotationsTransformer,
    override val element: IrExpression,
) : InferenceNode() {
    override val kind: NodeKind get() = NodeKind.Expression
    override val function: InferenceFunction = transformer.inferenceFunctionTypeOf(element.type)
}

/**
 * An [InferenceUnknownElement] is a general wrapper around function declarations and lambda.
 */
class InferenceUnknownElement(
    override val element: IrElement,
) : InferenceNode() {
    override val kind: NodeKind get() = NodeKind.Expression
    override val function: InferenceFunction? get() = null
    override val referenceContainer: InferenceNode? get() = null
}

/**
 * An [InferenceResolvedParameter] is a node that references a parameter of a container of this
 * node. For example, if the parameter is captured by a nested lambda this is resolved to the
 * captured parameter.
 */
class InferenceResolvedParameter(
    override val element: IrGetValue,
    override val function: InferenceFunction,
    val container: InferenceNode,
    val index: Int
) : InferenceNode() {
    override val kind: NodeKind get() = NodeKind.ParameterReference
    override fun parameterIndex(node: InferenceNode): Int =
        if (node.function == function) index else -1

    override val referenceContainer: InferenceNode get() = container

    override fun equals(other: Any?): Boolean =
        other is InferenceResolvedParameter && other.element == element

    override fun hashCode(): Int = element.hashCode() * 31 + 103
}

private inline fun <reified T> IrConstructorCall.firstParameterOrNull() =
    if (valueArgumentsCount >= 1) {
        (getValueArgument(0) as? IrConst<*>)?.value as? T
    } else null

private val IrConstructorCall.isComposableTarget get() =
    annotationClass?.isClassWithFqName(
        ComposeFqNames.ComposableTarget.toUnsafe()
    ) == true

private val IrConstructorCall.isComposableTargetMarked: Boolean get() =
    annotationClass?.owner?.annotations?.hasAnnotation(
        ComposeFqNames.ComposableTargetMarker
    ) == true

private val IrConstructorCall.isComposableInferredTarget get() =
    annotationClass?.isClassWithFqName(
        ComposeFqNames.ComposableInferredTarget.toUnsafe()
    ) == true

private val IrConstructorCall.isComposableOpenTarget get() =
    annotationClass?.isClassWithFqName(
        ComposeFqNames.ComposableOpenTarget.toUnsafe()
    ) == true

private fun IrType.samOwnerOrNull() =
    classOrNull?.let { cls ->
        if (cls.owner.kind == ClassKind.INTERFACE) {
            cls.functions.singleOrNull {
                it.owner.modality == Modality.ABSTRACT
            }?.owner
        } else null
    }

private val IrCall.arguments get() = Array(valueArgumentsCount) {
    getValueArgument(it)
}.toList()

private fun <T> Iterable<T>.takeUpTo(n: Int): List<T> =
    if (n <= 0) emptyList() else take(n)

/**
 * A function with overly wide parameters should be ignored for traversal as well as when
 * it is called.
 */
private fun IrFunction.hasOverlyWideParameters(): Boolean =
    valueParameters.any {
        it.type.isAny() || it.type.isNullableAny()
    }

private fun IrFunction.hasOpenTypeParameters(): Boolean =
    valueParameters.any { it.type.isTypeParameter() } ||
        dispatchReceiverParameter?.type?.isTypeParameter() == true ||
        extensionReceiverParameter?.type?.isTypeParameter() == true
