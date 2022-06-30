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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.FunctionMetrics
import androidx.compose.compiler.plugins.kotlin.KtxNameConventions
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.analysis.Stability
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.analysis.knownStable
import androidx.compose.compiler.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.getPrimitiveArrayElementType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.isCrossinline
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isNoinline
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.DFS

abstract class AbstractComposeLowering(
    val context: IrPluginContext,
    val symbolRemapper: DeepCopySymbolRemapper,
    val metrics: ModuleMetrics
) : IrElementTransformerVoid(), ModuleLoweringPass {
    @ObsoleteDescriptorBasedAPI
    protected val typeTranslator =
        TypeTranslatorImpl(
            context.symbolTable,
            context.languageVersionSettings,
            context.moduleDescriptor
        )

    protected val builtIns = context.irBuiltIns

    protected val stabilityInferencer = StabilityInferencer(context)

    fun stabilityOf(expr: IrExpression) = stabilityInferencer.stabilityOf(expr)
    fun stabilityOf(type: IrType) = stabilityInferencer.stabilityOf(type)
    fun stabilityOf(cls: IrClass) = stabilityInferencer.stabilityOf(cls)

    fun IrAnnotationContainer.hasStableMarker(): Boolean = with(stabilityInferencer) {
        hasStableMarker()
    }

    fun IrAnnotationContainer.hasStableAnnotation(): Boolean = with(stabilityInferencer) {
        hasStableAnnotation()
    }

    private val _composerIrClass =
        context.referenceClass(ComposeFqNames.Composer)?.owner
            ?: error("Cannot find the Composer class in the classpath")

    // this ensures that composer always references up-to-date composer class symbol
    // otherwise, after remapping of symbols in DeepCopyTransformer, it results in duplicated
    // references
    protected val composerIrClass: IrClass
        get() = symbolRemapper.getReferencedClass(_composerIrClass.symbol).owner

    fun referenceFunction(symbol: IrFunctionSymbol): IrFunctionSymbol {
        return symbolRemapper.getReferencedFunction(symbol)
    }

    fun referenceSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol {
        return symbolRemapper.getReferencedSimpleFunction(symbol)
    }

    fun referenceConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol {
        return symbolRemapper.getReferencedConstructor(symbol)
    }

    fun getTopLevelClass(fqName: FqName): IrClassSymbol {
        return getTopLevelClassOrNull(fqName) ?: error("Class not found in the classpath: $fqName")
    }

    fun getTopLevelClassOrNull(fqName: FqName): IrClassSymbol? {
        return context.referenceClass(fqName)
    }

    fun getTopLevelFunction(fqName: FqName): IrFunctionSymbol {
        return getTopLevelFunctionOrNull(fqName)
            ?: error("Function not found in the classpath: $fqName")
    }

    fun getTopLevelFunctionOrNull(fqName: FqName): IrFunctionSymbol? {
        return context.referenceFunctions(fqName).firstOrNull()
    }

    fun getTopLevelFunctions(fqName: FqName): List<IrSimpleFunctionSymbol> {
        return context.referenceFunctions(fqName).toList()
    }

    fun getInternalFunction(name: String) = getTopLevelFunction(
        ComposeFqNames.internalFqNameFor(name)
    )

    fun getInternalProperty(name: String) = getTopLevelPropertyGetter(
        ComposeFqNames.internalFqNameFor(name)
    )

    fun getInternalClass(name: String) = getTopLevelClass(
        ComposeFqNames.internalFqNameFor(name)
    )

    fun getInternalClassOrNull(name: String) = getTopLevelClassOrNull(
        ComposeFqNames.internalFqNameFor(name)
    )

    fun getTopLevelPropertyGetter(fqName: FqName): IrFunctionSymbol {
        val propertySymbol = context.referenceProperties(fqName).firstOrNull()
            ?: error("Property was not found $fqName")
        return symbolRemapper.getReferencedFunction(
            propertySymbol.owner.getter!!.symbol
        )
    }

    fun metricsFor(function: IrFunction): FunctionMetrics =
        (function as? IrAttributeContainer)?.let {
            context.irTrace[ComposeWritableSlices.FUNCTION_METRICS, it] ?: run {
                val metrics = metrics.makeFunctionMetrics(function)
                context.irTrace.record(ComposeWritableSlices.FUNCTION_METRICS, it, metrics)
                metrics
            }
        } ?: metrics.makeFunctionMetrics(function)

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    fun IrType.unboxInlineClass() = unboxType() ?: this

    fun IrType.replaceArgumentsWithStarProjections(): IrType =
        when (this) {
            is IrSimpleType -> IrSimpleTypeImpl(
                classifier,
                isMarkedNullable(),
                List(arguments.size) { IrStarProjectionImpl },
                annotations,
                abbreviation
            )
            else -> this
        }

    // IR external stubs don't have their value parameters' parent properly mapped to the
    // function itself. This normally isn't a problem because nothing in the IR lowerings ask for
    // the parent of the parameters, but we do. I believe this should be considered a bug in
    // kotlin proper, but this works around it.
    fun IrValueParameter.hasDefaultValueSafe(): Boolean = DFS.ifAny(
        listOf(this),
        { current ->
            (current.parent as? IrSimpleFunction)?.overriddenSymbols?.map { fn ->
                fn.owner.valueParameters[current.index].also { p ->
                    p.parent = fn.owner
                }
            } ?: listOf()
        },
        { current -> current.defaultValue != null }
    )

    // NOTE(lmr): This implementation mimics the kotlin-provided unboxInlineClass method, except
    // this one makes sure to bind the symbol if it is unbound, so is a bit safer to use.
    fun IrType.unboxType(): IrType? {
        val klass = classOrNull?.owner ?: return null
        val representation = klass.inlineClassRepresentation ?: return null
        if (!isInlineClassType()) return null

        // TODO: Apply type substitutions
        val underlyingType = representation.underlyingType.unboxInlineClass()
        if (!isNullable()) return underlyingType
        if (underlyingType.isNullable() || underlyingType.isPrimitiveType())
            return null
        return underlyingType.makeNullable()
    }

    protected fun IrExpression.unboxValueIfInline(): IrExpression {
        if (type.isNullable()) return this
        val classSymbol = type.classOrNull ?: return this
        val klass = classSymbol.owner
        if (type.isInlineClassType()) {
            if (context.platform.isJvm()) {
                return coerceInlineClasses(
                    this,
                    type,
                    type.unboxInlineClass()
                ).unboxValueIfInline()
            } else {
                val primaryValueParameter = klass.primaryConstructor?.valueParameters?.get(0)
                    ?: error("Expected a value parameter")
                val fieldGetter = klass.getPropertyGetter(primaryValueParameter.name.identifier)
                    ?: error("Expected a getter")
                return irCall(
                    symbol = fieldGetter,
                    dispatchReceiver = this
                ).unboxValueIfInline()
            }
        }
        return this
    }

    fun IrAnnotationContainer.hasComposableAnnotation(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun IrCall.isInvoke(): Boolean {
        return origin == IrStatementOrigin.INVOKE || symbol.descriptor is FunctionInvokeDescriptor
    }

    fun IrCall.isTransformedComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_CALL, this] ?: false
    }

    fun IrCall.isSyntheticComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL, this] == true
    }

    fun IrCall.isComposableSingletonGetter(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_SINGLETON, this] == true
    }

    fun IrClass.isComposableSingletonClass(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_SINGLETON_CLASS, this] == true
    }

    protected val KotlinType.isEnum
        get() =
            (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS

    fun Stability.irStableExpression(
        resolve: (IrTypeParameter) -> IrExpression? = { null }
    ): IrExpression? = when (this) {
        is Stability.Combined -> {
            val exprs = elements.mapNotNull { it.irStableExpression(resolve) }
            when {
                exprs.size != elements.size -> null
                exprs.isEmpty() -> irConst(StabilityBits.STABLE.bitsForSlot(0))
                exprs.size == 1 -> exprs.first()
                else -> exprs.reduce { a, b ->
                    irOr(a, b)
                }
            }
        }

        is Stability.Certain ->
            if (stable)
                irConst(StabilityBits.STABLE.bitsForSlot(0))
            else
                null

        is Stability.Parameter -> resolve(parameter)
        is Stability.Runtime -> {
            val stableField = makeStabilityField().also { it.parent = declaration }
            IrGetFieldImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                stableField.symbol,
                stableField.type
            )
        }
        is Stability.Unknown -> null
    }

    fun KotlinType.isFinal(): Boolean = (constructor.declarationDescriptor as? ClassDescriptor)
        ?.modality == Modality.FINAL

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.createParameterDeclarations() {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrValueParameterSymbolImpl(this),
            this.name,
            this.index(),
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType(),
            this.isCrossinline,
            this.isNoinline,
            false,
            false
        ).also {
            it.parent = this@createParameterDeclarations
        }

        fun TypeParameterDescriptor.irTypeParameter() = IrTypeParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrTypeParameterSymbolImpl(this),
            this.name,
            this.index,
            this.isReified,
            this.variance
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        valueParameters = descriptor.valueParameters.map { it.irValueParameter() }

        typeParameters = descriptor.typeParameters.map { it.irTypeParameter() }
    }

    protected fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    protected fun IrBuilderWithScope.irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)

        require(type.isFunction()) { "Function references should always have function type" }
        val returnType = (type as IrSimpleTypeImpl).arguments.last() as IrType

        val lambda = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            symbol,
            name = descriptor.name,
            visibility = descriptor.visibility,
            modality = descriptor.modality,
            returnType = returnType,
            isInline = descriptor.isInline,
            isExternal = descriptor.isExternal,
            isTailrec = descriptor.isTailrec,
            isSuspend = descriptor.isSuspend,
            isOperator = descriptor.isOperator,
            isExpect = descriptor.isExpect,
            isInfix = descriptor.isInfix
        ).also {
            it.parent = scope.getLocalDeclarationParent()
            it.createParameterDeclarations()
            it.body = DeclarationIrBuilder(this@AbstractComposeLowering.context, symbol)
                .irBlockBody { body(it) }
        }

        return irBlock(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrStatementOrigin.LAMBDA,
            resultType = type
        ) {
            +lambda
            +IrFunctionReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = type,
                symbol = symbol,
                typeArgumentsCount = descriptor.typeParametersCount,
                reflectionTarget = null,
                origin = IrStatementOrigin.LAMBDA,
                valueArgumentsCount = symbol.owner.valueParameters.size
            )
        }
    }

    @ObsoleteDescriptorBasedAPI
    protected fun IrBuilderWithScope.createFunctionDescriptor(
        type: IrType,
        owner: DeclarationDescriptor = scope.scopeOwnerSymbol.descriptor
    ): FunctionDescriptor {
        return AnonymousFunctionDescriptor(
            owner,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            val kotlinType = type.toKotlinType()
            initialize(
                kotlinType.getReceiverTypeFromFunctionType()?.let {
                    DescriptorFactory.createExtensionReceiverParameterForCallable(
                        this,
                        it,
                        Annotations.EMPTY
                    )
                },
                null,
                emptyList(),
                emptyList(),
                kotlinType.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                    ValueParameterDescriptorImpl(
                        containingDeclaration = this,
                        original = null,
                        index = i,
                        annotations = Annotations.EMPTY,
                        name = t.type.extractParameterNameFromFunctionTypeArgument()
                            ?: Name.identifier("p$i"),
                        outType = t.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = null,
                        source = SourceElement.NO_SOURCE
                    )
                },
                kotlinType.getReturnTypeFromFunctionType(),
                Modality.FINAL,
                DescriptorVisibilities.LOCAL,
                null
            )
            isOperator = false
            isInfix = false
            isExternal = false
            isInline = false
            isTailrec = false
            isSuspend = false
            isExpect = false
            isActual = false
        }
    }

    // set the bit at a certain index
    protected fun Int.withBit(index: Int, value: Boolean): Int {
        return if (value) {
            this or (1 shl index)
        } else {
            this and (1 shl index).inv()
        }
    }

    protected operator fun Int.get(index: Int): Boolean {
        return this and (1 shl index) != 0
    }

    // create a bitmask with the following bits
    protected fun bitMask(vararg values: Boolean): Int = values.foldIndexed(0) { i, mask, bit ->
        mask.withBit(i, bit)
    }

    protected fun irGetBit(param: IrDefaultBitMaskValue, index: Int): IrExpression {
        // value and (1 shl index) != 0
        return irNotEqual(
            param.irIsolateBitAtIndex(index),
            irConst(0)
        )
    }

    protected fun irSet(variable: IrValueDeclaration, value: IrExpression): IrExpression {
        return IrSetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            variable.symbol,
            value = value,
            origin = null
        )
    }

    protected fun irCall(
        symbol: IrFunctionSymbol,
        origin: IrStatementOrigin? = null,
        dispatchReceiver: IrExpression? = null,
        extensionReceiver: IrExpression? = null,
        vararg args: IrExpression
    ): IrCallImpl {
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol.owner.returnType,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            symbol.owner.valueParameters.size,
            origin
        ).also {
            if (dispatchReceiver != null) it.dispatchReceiver = dispatchReceiver
            if (extensionReceiver != null) it.extensionReceiver = extensionReceiver
            args.forEachIndexed { index, arg ->
                it.putValueArgument(index, arg)
            }
        }
    }

    protected fun IrType.binaryOperator(name: Name, paramType: IrType): IrFunctionSymbol =
        context.symbols.getBinaryOperator(name, this, paramType)

    protected fun IrType.unaryOperator(name: Name): IrFunctionSymbol =
        context.symbols.getUnaryOperator(name, this)

    protected fun irAnd(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        return irCall(
            lhs.type.binaryOperator(OperatorNameConventions.AND, rhs.type),
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irInv(lhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        return irCall(
            int.unaryOperator(OperatorNameConventions.INV),
            null,
            lhs
        )
    }

    protected fun irOr(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        return irCall(
            int.binaryOperator(OperatorNameConventions.OR, int),
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irBooleanOr(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val boolean = context.irBuiltIns.booleanType
        return irCall(
            boolean.binaryOperator(OperatorNameConventions.OR, boolean),
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irOrOr(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = IrStatementOrigin.OROR,
            type = context.irBuiltIns.booleanType,
            branches = listOf(
                IrBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = lhs,
                    result = irConst(true)
                ),
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = irConst(true),
                    result = rhs
                )
            )
        )
    }

    protected fun irAndAnd(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = IrStatementOrigin.ANDAND,
            type = context.irBuiltIns.booleanType,
            branches = listOf(
                IrBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = lhs,
                    result = rhs
                ),
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = irConst(true),
                    result = irConst(false)
                )
            )
        )
    }

    protected fun irXor(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        return irCall(
            int.binaryOperator(OperatorNameConventions.XOR, int),
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irGreater(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        val gt = context.irBuiltIns.greaterFunByOperandType[int.classifierOrFail]
        return irCall(
            gt!!,
            IrStatementOrigin.GT,
            null,
            null,
            lhs,
            rhs
        )
    }

    protected fun irReturn(
        target: IrReturnTargetSymbol,
        value: IrExpression,
        type: IrType = value.type
    ): IrExpression {
        return IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            target,
            value
        )
    }

    protected fun irEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irCall(
            this.context.irBuiltIns.eqeqeqSymbol,
            null,
            null,
            null,
            lhs,
            rhs
        )
    }

    protected fun irNot(value: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.booleanNotSymbol,
            dispatchReceiver = value
        )
    }

    protected fun irNotEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irNot(irEqual(lhs, rhs))
    }

//        context.irIntrinsics.symbols.intAnd
//        context.irIntrinsics.symbols.getBinaryOperator(name, lhs, rhs)
//        context.irBuiltIns.booleanNotSymbol
//        context.irBuiltIns.eqeqeqSymbol
//        context.irBuiltIns.eqeqSymbol
//        context.irBuiltIns.greaterFunByOperandType

    protected fun irConst(value: Int): IrConst<Int> = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.intType,
        IrConstKind.Int,
        value
    )

    protected fun irConst(value: Long): IrConst<Long> = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.longType,
        IrConstKind.Long,
        value
    )

    protected fun irConst(value: String): IrConst<String> = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.stringType,
        IrConstKind.String,
        value
    )

    protected fun irConst(value: Boolean) = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.booleanType,
        IrConstKind.Boolean,
        value
    )

    protected fun irNull() = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.anyNType,
        IrConstKind.Null,
        null
    )

    @ObsoleteDescriptorBasedAPI
    protected fun irForLoop(
        elementType: IrType,
        subject: IrExpression,
        loopBody: (IrValueDeclaration) -> IrExpression
    ): IrStatement {
        val primitiveType = subject.type.getPrimitiveArrayElementType()
        val iteratorSymbol = primitiveType?.let {
            context.symbols.primitiveIteratorsByType[it]
        } ?: context.symbols.iterator
        val unitType = context.irBuiltIns.unitType

        val getIteratorFunction = subject.type.classOrNull!!.owner.functions
            .single { it.name.asString() == "iterator" }

        val iteratorType = iteratorSymbol.typeWith(elementType)
        val nextSymbol = iteratorSymbol.owner.functions
            .single { it.descriptor.name.asString() == "next" }
        val hasNextSymbol = iteratorSymbol.owner.functions
            .single { it.descriptor.name.asString() == "hasNext" }

        val call = IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            iteratorType,
            getIteratorFunction.symbol,
            getIteratorFunction.symbol.owner.typeParameters.size,
            getIteratorFunction.symbol.owner.valueParameters.size,
            IrStatementOrigin.FOR_LOOP_ITERATOR
        ).also {
            it.dispatchReceiver = subject
        }

        val iteratorVar = irTemporary(
            value = call,
            isVar = false,
            name = "tmp0_iterator",
            irType = iteratorType,
            origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
        )
        return irBlock(
            type = unitType,
            origin = IrStatementOrigin.FOR_LOOP,
            statements = listOf(
                iteratorVar,
                IrWhileLoopImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    unitType,
                    IrStatementOrigin.FOR_LOOP_INNER_WHILE
                ).apply {
                    val loopVar = irTemporary(
                        value = IrCallImpl(
                            symbol = nextSymbol.symbol,
                            origin = IrStatementOrigin.FOR_LOOP_NEXT,
                            startOffset = UNDEFINED_OFFSET,
                            endOffset = UNDEFINED_OFFSET,
                            typeArgumentsCount = nextSymbol.symbol.owner.typeParameters.size,
                            valueArgumentsCount = nextSymbol.symbol.owner.valueParameters.size,
                            type = elementType
                        ).also {
                            it.dispatchReceiver = irGet(iteratorVar)
                        },
                        origin = IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                        isVar = false,
                        name = "value",
                        irType = elementType
                    )
                    condition = irCall(
                        symbol = hasNextSymbol.symbol,
                        origin = IrStatementOrigin.FOR_LOOP_HAS_NEXT,
                        dispatchReceiver = irGet(iteratorVar)
                    )
                    body = irBlock(
                        type = unitType,
                        origin = IrStatementOrigin.FOR_LOOP_INNER_WHILE,
                        statements = listOf(
                            loopVar,
                            loopBody(loopVar)
                        )
                    )
                }
            )
        )
    }

    @ObsoleteDescriptorBasedAPI
    protected fun irTemporary(
        value: IrExpression,
        name: String,
        irType: IrType = value.type,
        isVar: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
    ): IrVariableImpl {
        return IrVariableImpl(
            value.startOffset,
            value.endOffset,
            origin,
            IrVariableSymbolImpl(),
            Name.identifier(name),
            irType,
            isVar,
            false,
            false
        ).apply {
            initializer = value
        }
    }

    protected fun irGet(type: IrType, symbol: IrValueSymbol): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol
        )
    }

    protected fun irGet(variable: IrValueDeclaration): IrExpression {
        return irGet(variable.type, variable.symbol)
    }

    protected fun irIf(condition: IrExpression, body: IrExpression): IrExpression {
        return IrIfThenElseImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            origin = IrStatementOrigin.IF
        ).also {
            it.branches.add(
                IrBranchImpl(condition, body)
            )
        }
    }

    protected fun irIfThenElse(
        type: IrType = context.irBuiltIns.unitType,
        condition: IrExpression,
        thenPart: IrExpression,
        elsePart: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ) =
        IrIfThenElseImpl(startOffset, endOffset, type, IrStatementOrigin.IF).apply {
            branches.add(
                IrBranchImpl(
                    startOffset,
                    endOffset,
                    condition,
                    thenPart
                )
            )
            branches.add(irElseBranch(elsePart, startOffset, endOffset))
        }

    protected fun irWhen(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        branches: List<IrBranch>
    ) = IrWhenImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        origin,
        branches
    )
    protected fun irBranch(
        condition: IrExpression,
        result: IrExpression
    ): IrBranch {
        return IrBranchImpl(condition, result)
    }

    protected fun irElseBranch(
        expression: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ) = IrElseBranchImpl(startOffset, endOffset, irConst(true), expression)

    protected fun irBlock(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        statements: List<IrStatement>
    ): IrExpression {
        return IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            origin,
            statements
        )
    }

    protected fun irComposite(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        statements: List<IrStatement>
    ): IrExpression {
        return IrCompositeImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            origin,
            statements
        )
    }

    protected fun irLambda(function: IrFunction, type: IrType): IrExpression {
        return irBlock(
            type,
            origin = IrStatementOrigin.LAMBDA,
            statements = listOf(
                function,
                IrFunctionReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    type,
                    function.symbol,
                    function.typeParameters.size,
                    function.valueParameters.size,
                    null,
                    IrStatementOrigin.LAMBDA
                )
            )
        )
    }

    fun makeStabilityField(): IrField {
        return context.irFactory.buildField {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = KtxNameConventions.STABILITY_FLAG
            isStatic = context.platform.isJvm()
            isFinal = true
            type = context.irBuiltIns.intType
            visibility = DescriptorVisibilities.PUBLIC
        }
    }

    protected fun makeStabilityProp(): IrProperty {
        return context.irFactory.buildProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = KtxNameConventions.STABILITY_PROP_FLAG
            visibility = DescriptorVisibilities.PRIVATE
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun IrExpression.isStatic(): Boolean {
        return when (this) {
            // A constant by definition is static
            is IrConst<*> -> true
            // We want to consider all enum values as static
            is IrGetEnumValue -> true
            // Getting a companion object or top level object can be considered static if the
            // type of that object is Stable. (`Modifier` for instance is a common example)
            is IrGetObjectValue -> {
                if (symbol.owner.isCompanion) true
                else stabilityOf(symbol.owner).knownStable()
            }
            is IrConstructorCall -> isStatic()
            is IrCall -> isStatic()
            is IrGetValue -> {
                val owner = symbol.owner
                when (owner) {
                    is IrVariable -> {
                        // If we have an immutable variable whose initializer is also static,
                        // then we can determine that the variable reference is also static.
                        !owner.isVar && owner.initializer?.isStatic() == true
                    }
                    else -> false
                }
            }
            is IrFunctionExpression ->
                context.irTrace[ComposeWritableSlices.IS_STATIC_FUNCTION_EXPRESSION, this] ?: false
            else -> false
        }
    }

    fun IrConstructorCall.isStatic(): Boolean {
        // special case constructors of inline classes as static if their underlying
        // value is static.
        if (type.isInlineClassType()) {
            return stabilityOf(type.unboxInlineClass()).knownStable() &&
                getValueArgument(0)?.isStatic() == true
        }
        return false
    }

    @ObsoleteDescriptorBasedAPI
    fun IrCall.isStatic(): Boolean {
        val function = symbol.owner
        val fqName = function.descriptor.fqNameSafe
        // todo: special case, for instance, listOf(...statics...)
        return when (origin) {
            is IrStatementOrigin.GET_PROPERTY -> {
                // If we are in a GET_PROPERTY call, then this should usually resolve to
                // non-null, but in case it doesn't, just return false
                val prop = (function as? IrSimpleFunction)
                    ?.correspondingPropertySymbol?.owner ?: return false

                // if the property is a top level constant, then it is static.
                if (prop.isConst) return true

                val typeIsStable = stabilityOf(type).knownStable()
                val dispatchReceiverIsStatic = dispatchReceiver?.isStatic() != false
                val extensionReceiverIsStatic = extensionReceiver?.isStatic() != false

                // if we see that the property is read-only with a default getter and a
                // stable return type , then reading the property can also be considered
                // static if this is a top level property or the subject is also static.
                if (!prop.isVar &&
                    prop.getter?.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                    typeIsStable &&
                    dispatchReceiverIsStatic && extensionReceiverIsStatic
                ) {
                    return true
                }

                val getterIsStable = prop.hasStableAnnotation() ||
                    function.hasStableAnnotation()

                if (
                    getterIsStable &&
                    typeIsStable &&
                    dispatchReceiverIsStatic &&
                    extensionReceiverIsStatic
                ) {
                    return true
                }

                false
            }
            is IrStatementOrigin.PLUS,
            is IrStatementOrigin.MUL,
            is IrStatementOrigin.MINUS,
            is IrStatementOrigin.ANDAND,
            is IrStatementOrigin.OROR,
            is IrStatementOrigin.DIV,
            is IrStatementOrigin.EQ,
            is IrStatementOrigin.EQEQ,
            is IrStatementOrigin.EQEQEQ,
            is IrStatementOrigin.GT,
            is IrStatementOrigin.GTEQ,
            is IrStatementOrigin.LT,
            is IrStatementOrigin.LTEQ -> {
                // special case mathematical operators that are in the stdlib. These are
                // immutable operations so the overall result is static if the operands are
                // also static
                val isStableOperator = fqName.topLevelName() == "kotlin" ||
                    function.hasStableAnnotation()

                val typeIsStable = stabilityOf(type).knownStable()
                if (!typeIsStable) return false

                if (!isStableOperator) {
                    return false
                }

                getArguments().all { it.second.isStatic() }
            }
            null -> {
                if (fqName == ComposeFqNames.remember) {
                    // if it is a call to remember with 0 input arguments, then we can
                    // consider the value static if the result type of the lambda is stable
                    val syntheticRememberParams = 1 + // composer param
                        1 // changed param
                    val expectedArgumentsCount = 1 + syntheticRememberParams // 1 for lambda
                    if (
                        valueArgumentsCount == expectedArgumentsCount &&
                        stabilityOf(type).knownStable()
                    ) {
                        return true
                    }
                }
                if (fqName == ComposeFqNames.composableLambda) {
                    // calls to this function are generated by the compiler, and this
                    // function behaves similar to a remember call in that the result will
                    // _always_ be the same and the resulting type is _always_ stable, so
                    // thus it is static.
                    return true
                }
                if (context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_SINGLETON, this] == true) {
                    return true
                }
                // normal function call. If the function is marked as Stable and the result
                // is Stable, then the static-ness of it is the static-ness of its arguments
                val isStable = symbol.owner.hasStableAnnotation()
                if (!isStable) return false

                val typeIsStable = stabilityOf(type).knownStable()
                if (!typeIsStable) return false

                // getArguments includes the receivers!
                getArguments().all { it.second.isStatic() }
            }
            else -> false
        }
    }

    protected fun dexSafeName(name: Name): Name {
        return if (
            name.isSpecial || name.asString().contains(unsafeSymbolsRegex)
        ) {
            val sanitized = name
                .asString()
                .replace(unsafeSymbolsRegex, "\\$")
            Name.identifier(sanitized)
        } else name
    }

    fun coerceInlineClasses(argument: IrExpression, from: IrType, to: IrType) =
        IrCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            to,
            unsafeCoerceIntrinsic!!
        ).apply {
            putTypeArgument(0, from)
            putTypeArgument(1, to)
            putValueArgument(0, argument)
        }

    fun IrExpression.coerceToUnboxed() =
        coerceInlineClasses(this, this.type, this.type.unboxInlineClass())

    // Construct a reference to the JVM specific <unsafe-coerce> intrinsic.
    // This code should be kept in sync with the declaration in JvmSymbols.kt.
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol? by lazy {
        if (context.platform.isJvm()) {
            context.irFactory.buildFun {
                name = Name.special("<unsafe-coerce>")
                origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            }.apply {
                parent = IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
                    context.moduleDescriptor,
                    FqName("kotlin.jvm.internal")
                )
                val src = addTypeParameter("T", context.irBuiltIns.anyNType)
                val dst = addTypeParameter("R", context.irBuiltIns.anyNType)
                addValueParameter("v", src.defaultType)
                returnType = dst.defaultType
            }.symbol
        } else {
            null
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun IrSimpleFunction.sourceKey(): Int {
        val info = context.irTrace[
            ComposeWritableSlices.DURABLE_FUNCTION_KEY,
            this
        ]
        if (info != null) {
            info.used = true
            return info.key
        }
        val signature = symbol.descriptor.computeJvmDescriptor(withName = false)
        val name = fqNameForIrSerialization
        val stringKey = "$name$signature"
        return stringKey.hashCode()
    }
}

private val unsafeSymbolsRegex = "[ <>]".toRegex()

fun IrFunction.composerParam(): IrValueParameter? {
    for (param in valueParameters.asReversed()) {
        if (param.isComposerParam()) return param
        if (!param.name.asString().startsWith('$')) return null
    }
    return null
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrValueParameter.isComposerParam(): Boolean =
    (descriptor as? ValueParameterDescriptor)?.isComposerParam() ?: false

fun ValueParameterDescriptor.isComposerParam(): Boolean =
    name == KtxNameConventions.COMPOSER_PARAMETER &&
        type.constructor.declarationDescriptor?.fqNameSafe == ComposeFqNames.Composer

fun IrPluginContext.function(arity: Int): IrClassSymbol =
    referenceClass(FqName("kotlin.Function$arity"))!!

@ObsoleteDescriptorBasedAPI
val DeclarationDescriptorWithSource.startOffset: Int? get() =
    (this.source as? PsiSourceElement)?.psi?.startOffset

@ObsoleteDescriptorBasedAPI
val DeclarationDescriptorWithSource.endOffset: Int? get() =
    (this.source as? PsiSourceElement)?.psi?.endOffset

@ObsoleteDescriptorBasedAPI
fun IrAnnotationContainer.hasAnnotationSafe(fqName: FqName): Boolean =
    annotations.any {
        // compiler helper getAnnotation fails during remapping in [ComposableTypeRemapper], so we
        // use this impl
        fqName == it.annotationClass?.descriptor?.fqNameSafe
    }

// workaround for KT-45361
val IrConstructorCall.annotationClass get() =
        type.classOrNull

fun ParameterDescriptor.index(): Int =
    when (this) {
        is ReceiverParameterDescriptor -> -1
        is ValueParameterDescriptor -> index
        else -> error("expected either receiver or value parameter, but got: $this")
    }

inline fun <T> includeFileNameInExceptionTrace(file: IrFile, body: () -> T): T {
    try {
        return body()
    } catch (e: Exception) {
        throw Exception("IR lowering failed at: ${file.name}", e)
    }
}

fun FqName.topLevelName() =
    asString().substringBefore(".")
