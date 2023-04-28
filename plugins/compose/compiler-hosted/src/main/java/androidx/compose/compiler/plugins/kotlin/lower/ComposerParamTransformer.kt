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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.KtxNameConventions
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.decoys.copyWithNewTypeParams
import androidx.compose.compiler.plugins.kotlin.lower.decoys.didDecoyHaveDefaultForValueParameter
import androidx.compose.compiler.plugins.kotlin.lower.decoys.isDecoy
import androidx.compose.compiler.plugins.kotlin.lower.decoys.isDecoyImplementation
import kotlin.math.min
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrLocalDelegatedPropertyReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.remapTypeParameters
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

class ComposerParamTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    private val decoysEnabled: Boolean,
    metrics: ModuleMetrics,
) :
    AbstractComposeLowering(context, symbolRemapper, metrics),
    ModuleLoweringPass {

    /**
     * Used to identify module fragment in case of incremental compilation
     * see [externallyTransformed]
     */
    private var currentModule: IrModuleFragment? = null

    private var inlineLambdaInfo = ComposeInlineLambdaLocator(context)

    override fun lower(module: IrModuleFragment) {
        currentModule = module

        inlineLambdaInfo.scan(module)

        module.transformChildrenVoid(this)

        module.acceptVoid(symbolRemapper)

        val typeRemapper = ComposerTypeRemapper(
            context,
            symbolRemapper,
            composerType
        )
        // for each declaration, we create a deepCopy transformer It is important here that we
        // use the "preserving metadata" variant since we are using this copy to *replace* the
        // originals, or else the module we would produce wouldn't have any metadata in it.
        val transformer = DeepCopyIrTreeWithRemappedComposableTypes(
            context,
            symbolRemapper,
            typeRemapper
        ).also { typeRemapper.deepCopy = it }
        module.transformChildren(
            transformer,
            null
        )
        // just go through and patch all of the parents to make sure things are properly wired
        // up.
        module.patchDeclarationParents()
    }

    private val transformedFunctions: MutableMap<IrSimpleFunction, IrSimpleFunction> =
        mutableMapOf()

    private val transformedFunctionSet = mutableSetOf<IrSimpleFunction>()

    private val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement =
        super.visitSimpleFunction(declaration.withComposerParamIfNeeded())

    override fun visitLocalDelegatedPropertyReference(
        expression: IrLocalDelegatedPropertyReference
    ): IrExpression {
        val transformedGetter = expression.getter.owner.withComposerParamIfNeeded()
        return super.visitLocalDelegatedPropertyReference(
            IrLocalDelegatedPropertyReferenceImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.symbol,
                expression.delegate,
                transformedGetter.symbol,
                expression.setter,
                expression.origin
            )
        )
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

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (declaration.getter?.isComposableDelegatedAccessor() == true) {
            declaration.getter!!.annotations += createComposableAnnotation()
        }

        if (declaration.setter?.isComposableDelegatedAccessor() == true) {
            declaration.setter!!.annotations += createComposableAnnotation()
        }

        return super.visitProperty(declaration)
    }

    private fun createComposableAnnotation() =
        IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = composableIrClass.defaultType,
            symbol = composableIrClass.primaryConstructor!!.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            valueArgumentsCount = 0
        )

    fun IrCall.withComposerParamIfNeeded(composerParam: IrValueParameter): IrCall {
        val ownerFn = when {
            symbol.owner.isComposableDelegatedAccessor() -> {
                if (!symbol.owner.hasComposableAnnotation()) {
                    symbol.owner.annotations += createComposableAnnotation()
                }
                symbol.owner.withComposerParamIfNeeded()
            }
            symbol.owner.hasComposableAnnotation() ->
                symbol.owner.withComposerParamIfNeeded()
            isComposableLambdaInvoke() ->
                symbol.owner.lambdaInvokeWithComposerParam()
            // Not a composable call
            else -> return this
        }

        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol,
            typeArgumentsCount,
            ownerFn.valueParameters.size,
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
                } else if (param.isVararg) {
                    // do nothing
                } else {
                    it.putValueArgument(i, defaultArgumentFor(param))
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
        if (param.varargElementType != null) return null
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
        endOffset: Int = UNDEFINED_OFFSET
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
            val ctor = classSymbol!!.constructors.first()
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
                valueArgumentsCount = 1,
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

        // if it is a decoy, no need to process
        if (isDecoy()) return this

        // some functions were transformed during previous compilations or in other modules
        if (this.externallyTransformed()) {
            return this
        }

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
        val argCount = valueParameters.size
        val extraParams = composeSyntheticParamCount(argCount)
        val newFnClass = context.function(argCount + extraParams).owner
        val newInvoke = newFnClass.functions.first {
            it.name == OperatorNameConventions.INVOKE
        }
        return newInvoke
    }

    private fun IrSimpleFunction.copy(): IrSimpleFunction {
        // TODO(lmr): use deepCopy instead?
        return context.irFactory.createFunction(
            startOffset,
            endOffset,
            origin,
            IrSimpleFunctionSymbolImpl(),
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            isTailrec,
            isSuspend,
            isOperator,
            isInfix,
            isExpect,
            isFakeOverride,
            containerSource
        ).also { fn ->
            fn.copyAttributes(this)
            val propertySymbol = correspondingPropertySymbol
            if (propertySymbol != null) {
                fn.correspondingPropertySymbol = propertySymbol
                if (propertySymbol.owner.getter == this) {
                    propertySymbol.owner.getter = fn
                }
                if (propertySymbol.owner.setter == this) {
                    propertySymbol.owner.setter = fn
                }
            }
            fn.parent = parent
            fn.copyTypeParametersFrom(this)

            fun IrType.remapTypeParameters(): IrType =
                remapTypeParameters(this@copy, fn)

            fn.returnType = returnType.remapTypeParameters()

            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            fn.valueParameters = valueParameters.map { param ->
                // Composable lambdas will always have `IrGet`s of all of their parameters
                // generated, since they are passed into the restart lambda. This causes an
                // interesting corner case with "anonymous parameters" of composable functions.
                // If a parameter is anonymous (using the name `_`) in user code, you can usually
                // make the assumption that it is never used, but this is technically not the
                // case in composable lambdas. The synthetic name that kotlin generates for
                // anonymous parameters has an issue where it is not safe to dex, so we sanitize
                // the names here to ensure that dex is always safe.
                val newName = dexSafeName(param.name)

                val newType = defaultParameterType(param).remapTypeParameters()
                param.copyTo(
                    fn,
                    name = newName,
                    type = newType,
                    isAssignable = param.defaultValue != null,
                    defaultValue = param.defaultValue?.copyWithNewTypeParams(
                        source = this, target = fn
                    )
                )
            }
            fn.contextReceiverParametersCount = contextReceiverParametersCount
            fn.annotations = annotations.toList()
            fn.metadata = metadata
            fn.body = moveBodyTo(fn)?.copyWithNewTypeParams(this, fn)
        }
    }

    private fun jvmNameAnnotation(name: String): IrConstructorCall {
        val jvmName = getTopLevelClass(StandardClassIds.Annotations.JvmName)
        val ctor = jvmName.constructors.first { it.owner.isPrimary }
        val type = jvmName.createType(false, emptyList())
        return IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            ctor,
            0, 0, 1
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

        if (context.platform.isJs() && this.isDecoyImplementation()) {
            if (didDecoyHaveDefaultForValueParameter(index)) return true
        }

        return overriddenSymbols.any {
            it.owner.hasDefaultExpressionDefinedForValueParameter(index)
        }
    }

    private fun IrSimpleFunction.copyWithComposerParam(): IrSimpleFunction {
        assert(explicitParameters.lastOrNull()?.name != KtxNameConventions.COMPOSER_PARAMETER) {
            "Attempted to add composer param to $this, but it has already been added."
        }
        return copy().also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn] = fn

            // The overridden symbols might also be composable functions, so we want to make sure
            // and transform them as well
            fn.overriddenSymbols = overriddenSymbols.map {
                it.owner.withComposerParamIfNeeded().symbol
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

            val valueParametersMapping = explicitParameters
                .zip(fn.explicitParameters)
                .toMap()

            val currentParams = fn.valueParameters.size
            val realParams = currentParams - fn.contextReceiverParametersCount

            // $composer
            val composerParam = fn.addValueParameter {
                name = KtxNameConventions.COMPOSER_PARAMETER
                type = composerType.makeNullable()
                origin = IrDeclarationOrigin.DEFINED
                isAssignable = true
            }

            // $changed[n]
            val changed = KtxNameConventions.CHANGED_PARAMETER.identifier
            for (i in 0 until changedParamCount(realParams, fn.thisParamCount)) {
                fn.addValueParameter(
                    if (i == 0) changed else "$changed$i",
                    context.irBuiltIns.intType
                )
            }

            // $default[n]
            if (oldFn.requiresDefaultParameter()) {
                val defaults = KtxNameConventions.DEFAULT_PARAMETER.identifier
                for (i in 0 until defaultParamCount(currentParams)) {
                    fn.addValueParameter(
                        if (i == 0) defaults else "$defaults$i",
                        context.irBuiltIns.intType,
                        IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
                    )
                }
            }

            inlineLambdaInfo.scan(fn)

            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                var isNestedScope = false
                override fun visitGetValue(expression: IrGetValue): IrGetValue {
                    val newParam = valueParametersMapping[expression.symbol.owner]
                    return if (newParam != null) {
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            newParam.symbol,
                            expression.origin
                        )
                    } else expression
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    if (expression.returnTargetSymbol == oldFn.symbol) {
                        // update the return statement to point to the new function, or else
                        // it will be interpreted as a non-local return
                        return super.visitReturn(
                            IrReturnImpl(
                                expression.startOffset,
                                expression.endOffset,
                                expression.type,
                                fn.symbol,
                                expression.value
                            )
                        )
                    }
                    return super.visitReturn(expression)
                }

                override fun visitFunction(declaration: IrFunction): IrStatement {
                    val wasNested = isNestedScope
                    try {
                        // we don't want to pass the composer parameter in to composable calls
                        // inside of nested scopes.... *unless* the scope was inlined.
                        isNestedScope =
                            if (declaration.isNonComposableInlinedLambda()) wasNested else true
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

    private fun defaultParameterType(param: IrValueParameter): IrType {
        val type = param.type
        if (param.defaultValue == null) return type
        return when {
            type.isPrimitiveType() -> type
            type.isInlineClassType() -> type
            else -> type.makeNullable()
        }
    }

    private fun IrFunction.isNonComposableInlinedLambda(): Boolean =
        inlineLambdaInfo.isInlineLambda(this) && !hasComposableAnnotation()

    /**
     * With klibs, composable functions are always deserialized from IR instead of being restored
     * into stubs.
     * In this case, we need to avoid transforming those functions twice (because synthetic
     * parameters are being added). We know however, that all the other modules were compiled
     * before, so if the function comes from other [IrModuleFragment], we must skip it.
     *
     * NOTE: [ModuleDescriptor] will not work here, as incremental compilation of the same module
     * can contain some functions that were transformed during previous compilation in a
     * different module fragment with the same [ModuleDescriptor]
     */
    private fun IrFunction.externallyTransformed(): Boolean =
        decoysEnabled && valueParameters.firstOrNull {
            it.name == KtxNameConventions.COMPOSER_PARAMETER
        } != null
}
