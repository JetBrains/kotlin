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

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeNames
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.math.min

class ComposerParamTransformer(
    context: IrPluginContext,
    stabilityInferencer: StabilityInferencer,
    metrics: ModuleMetrics,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(context, metrics, stabilityInferencer, featureFlags),
    ModuleLoweringPass {

    private var inlineLambdaInfo = ComposeInlineLambdaLocator(context)

    override fun lower(irModule: IrModuleFragment) {
        inlineLambdaInfo.scan(irModule)

        irModule.transformChildrenVoid(this)

        val typeRemapper = ComposableTypeRemapper(
            context,
            composerType
        )
        val transformer = ComposableTypeTransformer(context, typeRemapper)
        // for each declaration, we remap types to ensure that @Composable lambda types are realized
        irModule.transformChildrenVoid(transformer)

        // just go through and patch all of the parents to make sure things are properly wired
        // up.
        irModule.patchDeclarationParents()
    }

    private val transformedFunctions: MutableMap<IrSimpleFunction, IrSimpleFunction> =
        mutableMapOf()

    private val transformedFunctionSet = mutableSetOf<IrSimpleFunction>()

    private val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement =
        super.visitSimpleFunction(declaration.withComposerParamIfNeeded())

    override fun visitLocalDelegatedPropertyReference(
        expression: IrLocalDelegatedPropertyReference,
    ): IrExpression {
        expression.getter = expression.getter.owner.withComposerParamIfNeeded().symbol
        expression.setter = expression.setter?.run { owner.withComposerParamIfNeeded().symbol }
        return super.visitLocalDelegatedPropertyReference(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        expression.getter = expression.getter?.run { owner.withComposerParamIfNeeded().symbol }
        expression.setter = expression.setter?.run { owner.withComposerParamIfNeeded().symbol }
        return super.visitPropertyReference(expression)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        if (declaration.getter.isComposableDelegatedAccessor()) {
            declaration.getter.annotations += createComposableAnnotation()
        }

        if (declaration.setter?.isComposableDelegatedAccessor() == true) {
            declaration.setter!!.annotations += createComposableAnnotation()
        }

        return super.visitLocalDelegatedProperty(declaration)
    }

    private fun createComposableAnnotation() =
        IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = composableIrClass.defaultType,
            symbol = composableIrClass.primaryConstructor!!.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        )

    fun IrCall.withComposerParamIfNeeded(composerParam: IrValueParameter): IrCall {
        val ownerFn = when {
            symbol.owner.isComposableDelegatedAccessor() -> {
                if (!symbol.owner.hasComposableAnnotation()) {
                    symbol.owner.annotations += createComposableAnnotation()
                }
                symbol.owner.withComposerParamIfNeeded()
            }
            isComposableLambdaInvoke() ->
                symbol.owner.lambdaInvokeWithComposerParam()
            symbol.owner.hasComposableAnnotation() ->
                symbol.owner.withComposerParamIfNeeded()
            // Not a composable call
            else -> return this
        }

        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol,
            typeArguments.size,
            origin,
            superQualifierSymbol
        ).also {
            it.copyAttributes(this)
            it.copyTypeArgumentsFrom(this)
            it.dispatchReceiver = dispatchReceiver
            it.extensionReceiver = extensionReceiver
            val argumentsMissing = mutableListOf<Boolean>()
            for (i in 0 until valueArgumentsCount) {
                val arg = getValueArgument(i)
                val param = ownerFn.valueParameters[i]
                val hasDefault = ownerFn.hasDefaultExpressionDefinedForValueParameter(i)
                argumentsMissing.add(arg == null && hasDefault)
                if (arg != null) {
                    it.putValueArgument(i, arg)
                } else if (hasDefault) {
                    it.putValueArgument(i, defaultArgumentFor(param))
                } else {
                    // do nothing
                }
            }
            val valueParams = valueArgumentsCount
            val realValueParams = valueParams - ownerFn.contextReceiverParametersCount
            var argIndex = valueArgumentsCount
            it.putValueArgument(
                argIndex++,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    composerParam.symbol
                )
            )

            // $changed[n]
            for (i in 0 until changedParamCount(realValueParams, ownerFn.thisParamCount)) {
                if (argIndex < ownerFn.valueParameters.size) {
                    it.putValueArgument(
                        argIndex++,
                        irConst(0)
                    )
                } else {
                    error("1. expected value parameter count to be higher: ${this.dumpSrc()}")
                }
            }

            // $default[n]
            for (i in 0 until defaultParamCount(valueParams)) {
                val start = i * BITS_PER_INT
                val end = min(start + BITS_PER_INT, valueParams)
                if (argIndex < ownerFn.valueParameters.size) {
                    val bits = argumentsMissing
                        .toBooleanArray()
                        .sliceArray(start until end)
                    it.putValueArgument(
                        argIndex++,
                        irConst(bitMask(*bits))
                    )
                } else if (argumentsMissing.any { it }) {
                    error("2. expected value parameter count to be higher: ${this.dumpSrc()}")
                }
            }
        }
    }

    private fun defaultArgumentFor(param: IrValueParameter): IrExpression? {
        return param.type.defaultValue().let {
            IrCompositeImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                IrStatementOrigin.DEFAULT_VALUE,
                listOf(it)
            )
        }
    }

    // TODO(lmr): There is an equivalent function in IrUtils, but we can't use it because it
    //  expects a JvmBackendContext. That implementation uses a special "unsafe coerce" builtin
    //  method, which is only available on the JVM backend. On the JS and Native backends we
    //  don't have access to that so instead we are just going to construct the inline class
    //  itself and hope that it gets lowered properly.
    private fun IrType.defaultValue(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrExpression {
        val classSymbol = classOrNull
        if (this !is IrSimpleType || isMarkedNullable() || !isInlineClassType()) {
            return if (isMarkedNullable()) {
                IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            } else {
                IrConstImpl.defaultValueForType(startOffset, endOffset, this)
            }
        }

        if (context.platform.isJvm()) {
            val underlyingType = unboxInlineClass()
            return coerceInlineClasses(
                IrConstImpl.defaultValueForType(startOffset, endOffset, underlyingType),
                underlyingType,
                this
            )
        } else {
            val ctor = classSymbol!!.constructors.first { it.owner.isPrimary }
            val underlyingType = getInlineClassUnderlyingType(classSymbol.owner)

            // TODO(lmr): We should not be calling the constructor here, but this seems like a
            //  reasonable interim solution.
            return IrConstructorCallImpl(
                startOffset,
                endOffset,
                this,
                ctor,
                typeArgumentsCount = 0,
                constructorTypeArgumentsCount = 0,
                origin = null
            ).also {
                it.putValueArgument(0, underlyingType.defaultValue(startOffset, endOffset))
            }
        }
    }

    // Transform `@Composable fun foo(params): RetType` into `fun foo(params, $composer: Composer): RetType`
    private fun IrSimpleFunction.withComposerParamIfNeeded(): IrSimpleFunction {
        // don't transform functions that themselves were produced by this function. (ie, if we
        // call this with a function that has the synthetic composer parameter, we don't want to
        // transform it further).
        if (transformedFunctionSet.contains(this)) return this

        // if not a composable fn, nothing we need to do
        if (!this.hasComposableAnnotation()) {
            return this
        }

        // we don't bother transforming expect functions. They exist only for type resolution and
        // don't need to be transformed to have a composer parameter
        if (isExpect) return this

        // cache the transformed function with composer parameter
        return transformedFunctions[this] ?: copyWithComposerParam()
    }

    private fun IrSimpleFunction.lambdaInvokeWithComposerParam(): IrSimpleFunction {
        val argCount = parameters.size
        val extraParams = composeSyntheticParamCount(argCount)
        val newFnClass = context.function(argCount + extraParams - /* dispatch receiver */ 1).owner
        val newInvoke = newFnClass.functions.first {
            it.name == OperatorNameConventions.INVOKE
        }
        return newInvoke
    }

    private fun jvmNameAnnotation(name: String): IrConstructorCall {
        val jvmName = getTopLevelClass(JvmStandardClassIds.Annotations.JvmName)
        val ctor = jvmName.constructors.first { it.owner.isPrimary }
        val type = jvmName.createType(false, emptyList())
        return IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = type,
            symbol = ctor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        ).also {
            it.putValueArgument(
                0,
                IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    builtIns.stringType,
                    name
                )
            )
        }
    }

    private fun IrSimpleFunction.requiresDefaultParameter(): Boolean =
    // we only add a default mask parameter if one of the parameters has a default
    // expression. Note that if this is a "fake override" method, then only the overridden
        // symbols will have the default value expressions
        valueParameters.any { it.defaultValue != null } ||
                overriddenSymbols.any { it.owner.requiresDefaultParameter() }

    private fun IrSimpleFunction.hasDefaultExpressionDefinedForValueParameter(index: Int): Boolean {
        // checking for default value isn't enough, you need to ensure that none of the overrides
        // have it as well...
        if (valueParameters[index].defaultValue != null) return true

        return overriddenSymbols.any {
            // ComposableFunInterfaceLowering copies extension receiver as a value
            // parameter, which breaks indices for overrides. fun interface cannot
            // have parameters defaults, however, so we can skip here if mismatch is detected.
            it.owner.valueParameters.size == valueParameters.size &&
                    it.owner.hasDefaultExpressionDefinedForValueParameter(index)
        }
    }

    internal inline fun <reified T : IrElement> T.deepCopyWithSymbolsAndMetadata(
        initialParent: IrDeclarationParent? = null,
        createTypeRemapper: (SymbolRemapper) -> TypeRemapper = ::DeepCopyTypeRemapper,
    ): T {
        val symbolRemapper = DeepCopySymbolRemapper()
        acceptVoid(symbolRemapper)
        val typeRemapper = createTypeRemapper(symbolRemapper)
        return (transform(DeepCopyPreservingMetadata(symbolRemapper, typeRemapper), null) as T).patchDeclarationParents(initialParent)
    }

    private fun IrSimpleFunction.copyWithComposerParam(): IrSimpleFunction {
        assert(parameters.lastOrNull()?.name != ComposeNames.COMPOSER_PARAMETER) {
            "Attempted to add composer param to $this, but it has already been added."
        }
        return deepCopyWithSymbolsAndMetadata(parent).also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn] = fn

            fn.metadata = oldFn.metadata

            // The overridden symbols might also be composable functions, so we want to make sure
            // and transform them as well
            fn.overriddenSymbols = oldFn.overriddenSymbols.map {
                it.owner.withComposerParamIfNeeded().symbol
            }

            val propertySymbol = oldFn.correspondingPropertySymbol
            if (propertySymbol != null) {
                fn.correspondingPropertySymbol = propertySymbol
                if (propertySymbol.owner.getter == oldFn) {
                    propertySymbol.owner.getter = fn
                }
                if (propertySymbol.owner.setter == oldFn) {
                    propertySymbol.owner.setter = fn
                }
            }
            // if we are transforming a composable property, the jvm signature of the
            // corresponding getters and setters have a composer parameter. Since Kotlin uses the
            // lack of a parameter to determine if it is a getter, this breaks inlining for
            // composable property getters since it ends up looking for the wrong jvmSignature.
            // In this case, we manually add the appropriate "@JvmName" annotation so that the
            // inliner doesn't get confused.
            fn.correspondingPropertySymbol?.let { propertySymbol ->
                if (!fn.hasAnnotation(DescriptorUtils.JVM_NAME)) {
                    val propertyName = propertySymbol.owner.name.identifier
                    val name = if (fn.isGetter) {
                        JvmAbi.getterName(propertyName)
                    } else {
                        JvmAbi.setterName(propertyName)
                    }
                    fn.annotations += jvmNameAnnotation(name)
                }
            }

            fn.parameters.fastForEach { param ->
                // Composable lambdas will always have `IrGet`s of all of their parameters
                // generated, since they are passed into the restart lambda. This causes an
                // interesting corner case with "anonymous parameters" of composable functions.
                // If a parameter is anonymous (using the name `_`) in user code, you can usually
                // make the assumption that it is never used, but this is technically not the
                // case in composable lambdas. The synthetic name that kotlin generates for
                // anonymous parameters has an issue where it is not safe to dex, so we sanitize
                // the names here to ensure that dex is always safe.
                if (param.kind == IrParameterKind.Regular || param.kind == IrParameterKind.Context) {
                    val newName = dexSafeName(param.name)
                    param.name = newName
                }
                param.isAssignable = param.defaultValue != null
            }

            val currentParams = fn.valueParameters.size
            val realParams = currentParams - fn.contextReceiverParametersCount

            // $composer
            val composerParam = fn.addValueParameter {
                name = ComposeNames.COMPOSER_PARAMETER
                type = composerType.makeNullable()
                origin = IrDeclarationOrigin.DEFINED
                isAssignable = true
            }

            // $changed[n]
            val changed = ComposeNames.CHANGED_PARAMETER.identifier
            for (i in 0 until changedParamCount(realParams, fn.thisParamCount)) {
                fn.addValueParameter(
                    if (i == 0) changed else "$changed$i",
                    context.irBuiltIns.intType
                )
            }

            // $default[n]
            if (fn.requiresDefaultParameter()) {
                val defaults = ComposeNames.DEFAULT_PARAMETER.identifier
                for (i in 0 until defaultParamCount(currentParams)) {
                    fn.addValueParameter(
                        if (i == 0) defaults else "$defaults$i",
                        context.irBuiltIns.intType,
                        IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
                    )
                }
            }

            fn.makeStubForDefaultValueClassIfNeeded()?.also {
                when (val parent = fn.parent) {
                    is IrClass -> parent.addChild(it)
                    is IrFile -> parent.addChild(it)
                    else -> {
                        // ignore
                    }
                }
            }

            // update parameter types so they are ready to accept the default values
            fn.valueParameters.fastForEach { param ->
                if (fn.hasDefaultExpressionDefinedForValueParameter(param.indexInOldValueParameters)) {
                    param.type = param.type.defaultParameterType()
                }
            }

            inlineLambdaInfo.scan(fn)

            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                var isNestedScope = false
                override fun visitFunction(declaration: IrFunction): IrStatement {
                    val wasNested = isNestedScope
                    try {
                        // we don't want to pass the composer parameter in to composable calls
                        // inside of nested scopes.... *unless* the scope was inlined.
                        isNestedScope = wasNested ||
                                !inlineLambdaInfo.isInlineLambda(declaration) ||
                                declaration.hasComposableAnnotation()
                        return super.visitFunction(declaration)
                    } finally {
                        isNestedScope = wasNested
                    }
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val expr = if (!isNestedScope) {
                        expression.withComposerParamIfNeeded(composerParam)
                    } else
                        expression
                    return super.visitCall(expr)
                }
            })
        }
    }

    /**
     * Creates stubs for @Composable function with value class parameters that have a default and
     * are wrapping a non-primitive instance.
     * Before Compose compiler 1.5.12, not specifying such parameter resulted in a nullptr exception
     * (b/330655412) at runtime, caused by Kotlin compiler inserting checkParameterNotNull.
     *
     * Converting such parameters to be nullable caused a binary compatibility issue because
     * nullability changed the value class mangle on a function signature. This stub creates a
     * binary compatible function to support old compilers while redirecting to a new function.
     */
    private fun IrSimpleFunction.makeStubForDefaultValueClassIfNeeded(): IrSimpleFunction? {
        if (!isPublicComposableFunction()) {
            return null
        }

        var makeStub = false
        for (i in valueParameters.indices) {
            val param = valueParameters[i]
            if (
                hasDefaultExpressionDefinedForValueParameter(i) &&
                param.type.isInlineClassType() &&
                !param.type.isNullable() &&
                param.type.unboxInlineClass().let {
                    !it.isPrimitiveType() && !it.isNullable()
                }
            ) {
                makeStub = true
                break
            }
        }

        if (!makeStub) {
            return null
        }

        val source = this
        return makeStub().also { copy ->
            transformedFunctions[copy] = copy
            transformedFunctionSet += copy

            copy.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                statements.add(
                    irReturn(
                        copy.symbol,
                        irCall(source).apply {
                            dispatchReceiver = copy.dispatchReceiverParameter?.let { irGet(it) }
                            extensionReceiver = copy.extensionReceiverParameter?.let { irGet(it) }
                            copy.typeParameters.fastForEachIndexed { index, param ->
                                typeArguments[index] = param.defaultType
                            }
                            copy.valueParameters.fastForEachIndexed { index, param ->
                                putValueArgument(index, irGet(param))
                            }
                        },
                        copy.returnType
                    )
                )
            }
        }
    }

    private fun IrSimpleFunction.isPublicComposableFunction(): Boolean =
        hasComposableAnnotation() && (visibility.isPublicAPI || isPublishedApi())
}

private val PublishedApiFqName = StandardClassIds.Annotations.PublishedApi.asSingleFqName()