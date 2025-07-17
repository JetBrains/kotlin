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
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
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

    private var currentParent: IrDeclarationParent? = null

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        val parent = currentParent
        if (declaration is IrDeclarationParent) {
            currentParent = declaration
        }
        return super.visitDeclaration(declaration).also {
            currentParent = parent
        }
    }

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

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE) {
            if (inlineLambdaInfo.isInlineFunctionExpression(expression)) {
                return super.visitBlock(expression)
            }
            val functionRef =
                when (val last = expression.statements.lastOrNull()) {
                    is IrFunctionReference -> last
                    is IrTypeOperatorCall -> {
                        last.argument as? IrFunctionReference
                            ?: return super.visitBlock(expression)
                    }
                    else -> error("Unexpected adapted function reference shape: ${expression.dump()}")
                }
            if (!functionRef.type.isKComposableFunction() && !functionRef.type.isSyntheticComposableFunction()) {
                return super.visitBlock(expression)
            }

            val fn = functionRef.symbol.owner as? IrSimpleFunction ?: return super.visitBlock(expression)

            // Adapter functions are never restartable, but the original function might be.
            val adapterCall = fn.findCallInBody() ?: error("Expected a call in ${fn.dump()}")
            val originalFn = adapterCall.symbol.owner
            return if (originalFn.shouldBeRestartable()) {
                super.visitBlock(expression)
            } else {
                adaptComposableReference(functionRef, originalFn, useAdaptedOrigin = false)
            }
        }
        return super.visitBlock(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (!expression.type.isKComposableFunction() && !expression.type.isSyntheticComposableFunction()) {
            return super.visitFunctionReference(expression)
        }

        val fn = expression.symbol.owner as? IrSimpleFunction ?: return super.visitFunctionReference(expression)

        if (!fn.isComposableReferenceAdapter &&
            fn.origin != IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE &&
            !inlineLambdaInfo.isInlineFunctionExpression(expression) &&
            !fn.shouldBeRestartable()
        ) {
            // Non-restartable functions may not contain a group and should be wrapped with a separate
            // adapter. This is different from Kotlin's adapted function reference since it is treated
            // as a regular local function and is not inlined into AdaptedFunctionReference.
            // This might mess with the reflection that tries to find a containing class, but the name
            // will be preserved. This is fine, since AdaptedFunctionReference does not support reflection
            // either.
            return adaptComposableReference(expression, fn, useAdaptedOrigin = false)
        } else if (!fn.isComposableReferenceAdapter && fn.requiresDefaultParameter()) {
            // Composable functions with default parameters add a $default mask parameter that is not expected
            // by the lambda side.
            // We need to create an adapter function that will call the original function with correct parameters.
            return adaptComposableReference(expression, fn, useAdaptedOrigin = true)
        } else {
            return transformComposableFunctionReference(expression, fn)
        }
    }

    private fun IrFunction.findCallInBody(): IrCall? {
        var call: IrCall? = null
        body?.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                if (call == null) {
                    call = expression
                }
                return
            }
        })
        return call
    }

    private fun transformComposableFunctionReference(
        expression: IrFunctionReference,
        fn: IrSimpleFunction
    ): IrExpression {
        val type = expression.type as IrSimpleType
        val changedParamCount = changedParamCount(type.arguments.size - /* return type */ 1, 0)
        val arity = type.arguments.size + /* composer */ 1 + changedParamCount

        val newType = IrSimpleTypeImpl(
            classifier = if (expression.type.isKComposableFunction()) {
                context.irBuiltIns.kFunctionN(arity).symbol
            } else {
                context.irBuiltIns.functionN(arity).symbol
            },
            hasQuestionMark = type.isNullable(),
            arguments = buildList {
                addAll(type.arguments.dropLast(1))
                add(composerType)
                repeat(changedParamCount) {
                    add(context.irBuiltIns.intType)
                }
                add(type.arguments.last())
            },
            annotations = type.annotations,
            abbreviation = type.abbreviation
        )

        // Transform receiver arguments
        expression.transformChildrenVoid()

        // Adapted function calls created by Kotlin compiler don't copy annotations from the original function
        if (fn.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE && !fn.hasComposableAnnotation()) {
            fn.annotations += createComposableAnnotation()
        }

        return IrFunctionReferenceImpl(
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = newType,
            symbol = fn.withComposerParamIfNeeded().symbol,
            typeArgumentsCount = expression.typeArguments.size,
            reflectionTarget = expression.reflectionTarget?.owner?.let {
                if (it is IrSimpleFunction) it.withComposerParamIfNeeded().symbol else it.symbol
            },
            origin = expression.origin,
        ).apply {
            typeArguments.assignFrom(expression.typeArguments)
            arguments.assignFrom(expression.arguments)
            repeat(arity - expression.arguments.size) {
                arguments.add(null)
            }
        }
    }

    private fun adaptComposableReference(
        expression: IrFunctionReference,
        fn: IrSimpleFunction,
        useAdaptedOrigin: Boolean
    ): IrExpression {
        val adapter = irBlock(
            type = expression.type,
            origin = if (useAdaptedOrigin) IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE else null,
            statements = buildList {
                val localParent = currentParent ?: error("No parent found for ${expression.dump()}")
                val adapterFn = context.irFactory.buildFun {
                    origin = if (useAdaptedOrigin) IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE else origin
                    name = fn.name
                    visibility = DescriptorVisibilities.LOCAL
                    modality = Modality.FINAL
                    returnType = fn.returnType
                }
                adapterFn.copyAnnotationsFrom(fn)
                adapterFn.copyParametersFrom(fn, copyDefaultValues = false)
                require(
                    adapterFn.parameters.count {
                        it.kind == IrParameterKind.ExtensionReceiver ||
                                it.kind == IrParameterKind.DispatchReceiver
                    } <= 1
                ) {
                    "Function references are not allowed to have multiple receivers: ${expression.dump()}"
                }
                adapterFn.parameters = buildList {
                    val receiver = adapterFn.parameters.find {
                        it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver
                    }
                    if (receiver != null) {
                        // Match IR generated by the FIR2IR adapter codegen.
                        receiver.kind = IrParameterKind.ExtensionReceiver
                        receiver.name = Name.identifier("receiver")
                        add(receiver)
                    }

                    // The adapter function should have the same parameters as the KComposableFunction type.
                    // Receivers are processed separately and are not included in the parameter count.
                    val type = expression.type as IrSimpleType
                    var n = type.arguments.size - /* return type */ 1
                    adapterFn.parameters.fastForEach {
                        if (it.kind == IrParameterKind.Regular && n-- > 0) {
                            add(it)
                        }
                    }
                }
                adapterFn.isComposableReferenceAdapter = true

                adapterFn.body = context.irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET) {
                    statements.add(
                        irReturn(
                            adapterFn.symbol,
                            irCall(fn.symbol).also { call ->
                                fn.parameters.fastForEach {
                                    call.arguments[it.indexInParameters] = when (it.kind) {
                                        IrParameterKind.Context -> {
                                            // Should be unreachable (see CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION)
                                            error("Context parameters are not supported in function references")
                                        }
                                        IrParameterKind.DispatchReceiver,
                                        IrParameterKind.ExtensionReceiver -> {
                                            adapterFn.parameters.first { it.kind == IrParameterKind.ExtensionReceiver }
                                        }
                                        IrParameterKind.Regular -> {
                                            adapterFn.parameters.getOrNull(it.indexInParameters)
                                        }
                                    }?.let(::irGet)
                                }
                            }
                        )
                    )
                }
                adapterFn.parent = localParent
                add(adapterFn)

                add(
                    IrFunctionReferenceImpl(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                        type = expression.type,
                        symbol = adapterFn.symbol,
                        typeArgumentsCount = expression.typeArguments.size,
                        reflectionTarget = fn.symbol,
                        origin = if (useAdaptedOrigin) IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE else null
                    ).apply {
                        typeArguments.assignFrom(expression.typeArguments)
                        arguments.assignFrom(expression.arguments)
                    }
                )
            }
        )

        // Pass the adapted function reference to the transformer to handle the adapted function the same way as regular composables.
        return visitBlock(adapter)
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
        val newFn = when {
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
            newFn.symbol,
            typeArguments.size,
            origin,
            superQualifierSymbol
        ).also { newCall ->
            newCall.copyAttributes(this)
            newCall.copyTypeArgumentsFrom(this)

            val argumentsMissing = mutableListOf<Boolean>()
            arguments.fastForEachIndexed { i, arg ->
                val p = newFn.parameters[i]
                when (p.kind) {
                    IrParameterKind.DispatchReceiver,
                    IrParameterKind.ExtensionReceiver,
                    IrParameterKind.Context -> {
                        newCall.arguments[p.indexInParameters] = arg
                    }
                    IrParameterKind.Regular -> {
                        val hasDefault = newFn.hasDefaultForParam(i)
                        argumentsMissing.add(arg == null && hasDefault)
                        if (arg != null) {
                            newCall.arguments[p.indexInParameters] = arg
                        } else if (hasDefault) {
                            newCall.arguments[p.indexInParameters] = defaultArgumentFor(p)
                        } else {
                            // do nothing
                        }
                    }
                }
            }

            val valueParamCount = arguments.indices.count { i ->
                val p = newFn.parameters[i]
                p.kind == IrParameterKind.Regular
            }
            var argIndex = arguments.size
            newCall.arguments[argIndex++] = irGet(composerParam)

            // $changed[n]
            for (i in 0 until changedParamCount(valueParamCount, newFn.thisParamCount)) {
                if (argIndex < newFn.parameters.size) {
                    newCall.arguments[argIndex++] = irConst(0)
                } else {
                    error("1. expected value parameter count to be higher: ${this.dumpSrc()}")
                }
            }

            // $default[n]
            for (i in 0 until defaultParamCount(valueParamCount)) {
                val start = i * BITS_PER_INT
                val end = min(start + BITS_PER_INT, valueParamCount)
                if (argIndex < newFn.parameters.size) {
                    val bits = argumentsMissing
                        .toBooleanArray()
                        .sliceArray(start until end)
                    newCall.arguments[argIndex++] = irConst(bitMask(*bits))
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
                it.arguments[0] = underlyingType.defaultValue(startOffset, endOffset)
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
        val newFnClass = context.irBuiltIns.functionN(argCount + extraParams - /* dispatch receiver */ 1)
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
            it.arguments[0] = irConst(name)
        }
    }

    private fun IrSimpleFunction.requiresDefaultParameter(): Boolean =
        when {
            isDefaultParamStub -> true
            isVirtualFunctionWithDefaultParam() -> false
            isFakeOverride -> overriddenSymbols.any { it.owner.modality == Modality.FINAL && it.owner.requiresDefaultParameter() }
            else -> parameters.any { it.defaultValue != null }
        }


    private fun IrSimpleFunction.hasDefaultForParam(index: Int): Boolean {
        // checking for default value isn't enough, you need to ensure that none of the overrides
        // have it as well...
        if (parameters[index].kind != IrParameterKind.Regular) return false
        if (parameters[index].defaultValue != null) return true

        return overriddenSymbols.any {
            // ComposableFunInterfaceLowering copies extension receiver as a value
            // parameter, which breaks indices for overrides. fun interface cannot
            // have parameters defaults, however, so we can skip here if mismatch is detected.
            it.owner.parameters.size == parameters.size &&
                    it.owner.hasDefaultForParam(index)
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
        assert(parameters.lastOrNull()?.name != ComposeNames.ComposerParameter) {
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

            val currentParams = fn.parameters.count { it.kind == IrParameterKind.Regular }
            val realParams = currentParams

            // $composer
            val composerParam = fn.addValueParameter {
                name = ComposeNames.ComposerParameter
                type = composerType.makeNullable()
                origin = IrDeclarationOrigin.DEFINED
                isAssignable = true
            }

            // $changed[n]
            val changed = ComposeNames.ChangedParameter.identifier
            for (i in 0 until changedParamCount(realParams, fn.thisParamCount)) {
                fn.addValueParameter(
                    if (i == 0) changed else "$changed$i",
                    context.irBuiltIns.intType
                )
            }

            // $default[n]
            if (fn.requiresDefaultParameter()) {
                val defaults = ComposeNames.DefaultParameter.identifier
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
            fn.parameters.fastForEach { param ->
                if (fn.hasDefaultForParam(param.indexInParameters)) {
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
        for (i in parameters.indices) {
            val param = parameters[i]
            if (
                hasDefaultForParam(i) &&
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
                            copy.typeParameters.fastForEachIndexed { index, param ->
                                typeArguments[index] = param.defaultType
                            }
                            copy.parameters.fastForEachIndexed { index, param ->
                                arguments[param.indexInParameters] = irGet(param)
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
