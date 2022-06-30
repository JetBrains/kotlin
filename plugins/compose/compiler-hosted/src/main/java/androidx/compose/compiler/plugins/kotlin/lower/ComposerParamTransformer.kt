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
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.irTrace
import androidx.compose.compiler.plugins.kotlin.lower.decoys.copyWithNewTypeParams
import androidx.compose.compiler.plugins.kotlin.lower.decoys.didDecoyHaveDefaultForValueParameter
import androidx.compose.compiler.plugins.kotlin.lower.decoys.isDecoy
import androidx.compose.compiler.plugins.kotlin.lower.decoys.isDecoyImplementation
import kotlin.math.min
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleExpectsForActual
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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun lower(module: IrModuleFragment) {
        currentModule = module

        inlineLambdaInfo.scan(module)

        module.transformChildrenVoid(this)

        module.acceptVoid(symbolRemapper)

        val typeRemapper = ComposerTypeRemapper(
            context,
            symbolRemapper,
            typeTranslator,
            composerType
        )
        // for each declaration, we create a deepCopy transformer It is important here that we
        // use the "preserving metadata" variant since we are using this copy to *replace* the
        // originals, or else the module we would produce wouldn't have any metadata in it.
        val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
            context,
            symbolRemapper,
            typeRemapper,
            typeTranslator
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

    private val transformedFunctionSet = mutableSetOf<IrFunction>()

    private val composerType = composerIrClass.defaultType.replaceArgumentsWithStarProjections()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val v1 = declaration.withComposerParamIfNeeded()
        val v2 = super.visitFunction(v1)
        return v2
    }

    fun IrCall.withComposerParamIfNeeded(composerParam: IrValueParameter): IrCall {
        val isComposableLambda = isComposableLambdaInvoke()

        if (!symbol.owner.hasComposableAnnotation() && !isComposableLambda)
            return this
        val ownerFn = when {
            isComposableLambda -> {
                symbol.owner.lambdaInvokeWithComposerParam()
            }
            else -> (symbol.owner).withComposerParamIfNeeded()
        }

        // externally transformed functions are already remapped from decoys, so we only need to
        // add the parameters to the call
        if (!ownerFn.externallyTransformed()) {
            if (!isComposableLambda && !transformedFunctionSet.contains(ownerFn))
                return this
            if (symbol.owner == ownerFn)
                return this
        }

        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol as IrSimpleFunctionSymbol,
            typeArgumentsCount,
            ownerFn.valueParameters.size,
            origin,
            superQualifierSymbol
        ).also {
            it.copyAttributes(this)
            context.irTrace.record(
                ComposeWritableSlices.IS_COMPOSABLE_CALL,
                it,
                true
            )
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
    private fun IrFunction.withComposerParamIfNeeded(): IrFunction {
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

        // if this function is an inlined lambda passed as an argument to an inline function (and
        // is NOT a composable lambda), then we don't want to transform it. Ideally, this
        // wouldn't have gotten this far because the `isComposable()` check above should return
        // false, but right now the composable annotation checker seems to produce a
        // false-positive here. It is important that we *DO NOT* transform this, but we should
        // probably fix the annotation checker instead.
        // TODO(b/147250515)
        if (isNonComposableInlinedLambda()) return this

        // we don't bother transforming expect functions. They exist only for type resolution and
        // don't need to be transformed to have a composer parameter
        if (isExpect) return this

        // cache the transformed function with composer parameter
        return transformedFunctions[this] ?: copyWithComposerParam()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.lambdaInvokeWithComposerParam(): IrFunction {
        val descriptor = descriptor
        val argCount = descriptor.valueParameters.size
        val extraParams = composeSyntheticParamCount(argCount)
        val newFnClass = context.function(argCount + extraParams).owner
        val newInvoke = newFnClass.functions.first {
            it.name == OperatorNameConventions.INVOKE
        }
        return newInvoke
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.copy(
        isInline: Boolean = this.isInline,
        modality: Modality = descriptor.modality
    ): IrSimpleFunction {
        // TODO(lmr): use deepCopy instead?
        val descriptor = descriptor

        return IrFunctionImpl(
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
            descriptor.isTailrec,
            descriptor.isSuspend,
            descriptor.isOperator,
            descriptor.isInfix,
            isExpect,
            isFakeOverride,
            containerSource
        ).also { fn ->
            if (this is IrSimpleFunction) {
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
            }
            fn.parent = parent
            fn.copyTypeParametersFrom(this)

            fun IrType.remapTypeParameters() =
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
        val jvmName = getTopLevelClass(DescriptorUtils.JVM_NAME)
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

    private fun IrFunction.requiresDefaultParameter(): Boolean {
        val expectDescriptor = expectDescriptor()
        // we only add a default mask parameter if one of the parameters has a default
        // expression. Note that if this is a "fake override" method, then only the overridden
        // symbols will have the default value expressions
        return this is IrSimpleFunction && (
            valueParameters.any { it.defaultValue != null } ||
                (
                    expectDescriptor != null &&
                        expectDescriptor.valueParameters.any { it.declaresDefaultValue() }
                    ) ||
                overriddenSymbols.any { it.owner.requiresDefaultParameter() }
            )
    }

    private fun IrFunction.hasDefaultExpressionDefinedForValueParameter(index: Int): Boolean {
        // checking for default value isn't enough, you need to ensure that none of the overrides
        // have it as well...
        if (this !is IrSimpleFunction) return false
        if (valueParameters[index].defaultValue != null) return true

        if (context.platform.isJs() && this.isDecoyImplementation()) {
            if (didDecoyHaveDefaultForValueParameter(index)) return true
        }

        return overriddenSymbols.any {
            it.owner.hasDefaultExpressionDefinedForValueParameter(index)
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.copyWithComposerParam(): IrSimpleFunction {
        assert(explicitParameters.lastOrNull()?.name != KtxNameConventions.COMPOSER_PARAMETER) {
            "Attempted to add composer param to $this, but it has already been added."
        }
        return copy().also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn as IrSimpleFunction] = fn

            // The overridden symbols might also be composable functions, so we want to make sure
            // and transform them as well
            if (this is IrOverridableDeclaration<*>) {
                fn.overriddenSymbols = overriddenSymbols.map {
                    it as IrSimpleFunctionSymbol
                    val owner = it.owner
                    val newOwner = owner.withComposerParamIfNeeded()
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }

            // if we are transforming a composable property, the jvm signature of the
            // corresponding getters and setters have a composer parameter. Since Kotlin uses the
            // lack of a parameter to determine if it is a getter, this breaks inlining for
            // composable property getters since it ends up looking for the wrong jvmSignature.
            // In this case, we manually add the appropriate "@JvmName" annotation so that the
            // inliner doesn't get confused.
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations += jvmNameAnnotation(name)
                fn.correspondingPropertySymbol?.owner?.getter = fn
            }

            // same thing for the setter
            if (descriptor is PropertySetterDescriptor &&
                fn.annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                fn.annotations += jvmNameAnnotation(name)
                fn.correspondingPropertySymbol?.owner?.setter = fn
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

    private fun IrCall.isComposableLambdaInvoke(): Boolean {
        return isInvoke() && dispatchReceiver?.type?.hasComposableAnnotation() == true
    }

    private fun IrFunction.isNonComposableInlinedLambda(): Boolean =
        inlineLambdaInfo.isInlineLambda(this) && !hasComposableAnnotation()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.expectDescriptor(): CallableDescriptor? =
        if (descriptor !is IrBasedDeclarationDescriptor<*>) {
            descriptor.findCompatibleExpectsForActual {
                it == module
            }.singleOrNull() as? CallableDescriptor
        } else {
            null
        }

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
        decoysEnabled && currentModule?.files?.contains(fileOrNull) != true
}
