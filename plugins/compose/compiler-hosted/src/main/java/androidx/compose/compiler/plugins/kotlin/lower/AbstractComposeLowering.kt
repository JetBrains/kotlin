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

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.*
import androidx.compose.compiler.plugins.kotlin.ComposeFqNames.InternalPackage
import androidx.compose.compiler.plugins.kotlin.analysis.*
import androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc.hiddenFromObjCClassId
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.library.metadata.DeserializedSourceFile
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

object ComposeCompilerKey : GeneratedDeclarationKey()

abstract class AbstractComposeLowering(
    val context: IrPluginContext,
    val metrics: ModuleMetrics,
    val stabilityInferencer: StabilityInferencer,
    private val featureFlags: FeatureFlags,
) : IrElementTransformerVoid(), ModuleLoweringPass {
    protected val builtIns = context.irBuiltIns

    private val finderForBuiltins = context.finderForBuiltins()

    protected val composerIrClass =
        finderForBuiltins.findClass(ComposeClassIds.Composer)?.owner
            ?: error("Cannot find the Composer class in the classpath")

    protected val composableIrClass =
        finderForBuiltins.findClass(ComposeClassIds.Composable)?.owner
            ?: error("Cannot find the Composable annotation class in the classpath")

    fun getTopLevelClass(classId: ClassId): IrClassSymbol {
        return getTopLevelClassOrNull(classId)
            ?: error("Class not found in the classpath: ${classId.asSingleFqName()}")
    }

    fun getTopLevelClassOrNull(classId: ClassId): IrClassSymbol? {
        return finderForBuiltins.findClass(classId)
    }

    fun getTopLevelFunction(callableId: CallableId): IrSimpleFunctionSymbol {
        return getTopLevelFunctionOrNull(callableId)
            ?: error("Function not found in the classpath: ${callableId.asSingleFqName()}")
    }

    fun getTopLevelFunctionOrNull(callableId: CallableId): IrSimpleFunctionSymbol? {
        return finderForBuiltins.findFunctions(callableId).firstOrNull()
    }

    fun getTopLevelFunctions(callableId: CallableId): List<IrSimpleFunctionSymbol> {
        return finderForBuiltins.findFunctions(callableId).toList()
    }

    fun getTopLevelPropertyGetter(callableId: CallableId): IrFunctionSymbol {
        val propertySymbol = finderForBuiltins.findProperties(callableId).firstOrNull()
            ?: error("Property was not found ${callableId.asSingleFqName()}")
        return propertySymbol.owner.getter!!.symbol
    }

    val FeatureFlag.enabled get() = featureFlags.isEnabled(this)

    fun metricsFor(function: IrFunction): FunctionMetrics =
        function.functionMetrics
            ?: metrics.makeFunctionMetrics(function).also {
                function.functionMetrics = it
            }

    fun IrType.unboxInlineClass() = unboxType() ?: this

    fun IrType.defaultParameterType(): IrType {
        val type = this

        return when {
            type.isPrimitiveType() -> type
            type.isInlineClassType() -> {
                // TODO migrate to more precise constructor accessibility test in k2.4
                val constructorAccessible = type.classOrNull?.owner?.primaryConstructor != null
                if (context.platform.isJvm() || constructorAccessible) {
                    if (type.unboxInlineClass().isPrimitiveType()) {
                        type
                    } else {
                        type.makeNullable()
                    }
                } else {
                    // k/js and k/native: private constructors of value classes can be not accessible.
                    // Therefore it won't be possible to create a "fake" default argument for calls.
                    // Making it nullable allows to pass null.
                    type.makeNullable()
                }
            }
            else -> type.makeNullable()
        }
    }

    fun IrType.replaceArgumentsWithStarProjections(): IrType =
        when (this) {
            is IrSimpleType -> IrSimpleTypeImpl(
                classifier,
                isMarkedNullable(),
                List(arguments.size) { IrStarProjectionImpl },
                annotations
            )

            else -> this
        }

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
                val primaryValueParameter = klass.primaryConstructor?.parameters?.singleOrNull { it.kind == IrParameterKind.Regular }
                val cantUnbox = primaryValueParameter == null || klass.properties.none {
                    it.name == primaryValueParameter.name && it.getter != null
                }
                if (cantUnbox) {
                    // LazyIr (external module) doesn't show a getter of a private property.
                    // So we can't unbox the value
                    return this
                }
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
        return hasAnnotation(ComposeFqNames.Composable)
    }

    fun IrCall.isInvoke(): Boolean {
        if (origin == IrStatementOrigin.INVOKE)
            return true
        val function = symbol.owner
        return function.name == OperatorNameConventions.INVOKE &&
                function.parentClassOrNull?.defaultType?.let {
                    it.isFunction() || it.isSyntheticComposableFunction()
                } ?: false
    }

    fun IrCall.isComposableCall(): Boolean {
        return symbol.owner.hasComposableAnnotation() || isComposableLambdaInvoke()
    }

    fun IrCall.isSyntheticComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL, this] == true
    }

    fun IrCall.isComposableLambdaInvoke(): Boolean {
        if (!isInvoke()) return false
        // [ComposerParamTransformer] replaces composable function types of the form
        // `@Composable Function1<T1, T2>` with ordinary functions with extra parameters, e.g.,
        // `Function3<T1, Composer, Int, T2>`. After this lowering runs we have to check the
        // `attributeOwnerId` to recover the original type.
        val receiver = dispatchReceiver?.let { it.attributeOwnerId as? IrExpression ?: it }
        return receiver?.type?.let {
            it.hasComposableAnnotation() || it.isSyntheticComposableFunction()
        } ?: false
    }

    fun IrCall.isComposableSingletonGetter(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_SINGLETON, this] == true
    }

    fun IrClass.isComposableSingletonClass(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_SINGLETON_CLASS, this] == true
    }

    fun Stability.irStableExpression(
        resolve: (IrTypeParameter) -> IrExpression? = { null },
        reportUnknownStability: (IrClass) -> Unit = { },
    ): IrExpression? = when (this) {
        is Stability.Combined -> {
            val exprs = elements.mapNotNull { it.irStableExpression(resolve, reportUnknownStability) }
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
            val stabilityExpr = declaration.getRuntimeStabilityValue()
            if (stabilityExpr == null) {
                reportUnknownStability(declaration)
            }
            stabilityExpr
        }

        is Stability.Unknown -> null
    }

    // set the bit at a certain index
    private fun Int.withBit(index: Int, value: Boolean): Int {
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
        symbol: IrFunctionSymbol
    ): IrCallImpl =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol.owner.returnType,
            symbol as IrSimpleFunctionSymbol,
        )

    protected fun irCall(
        symbol: IrFunctionSymbol,
        origin: IrStatementOrigin? = null,
        returnType: IrType = symbol.owner.returnType,
        dispatchReceiver: IrExpression? = null,
        extensionReceiver: IrExpression? = null,
        vararg args: IrExpression,
    ): IrCallImpl {
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            returnType,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            origin
        ).also { call ->
            var argIndex = 0
            symbol.owner.parameters.forEach {
                when (it.kind) {
                    IrParameterKind.DispatchReceiver -> {
                        call.arguments[it.indexInParameters] = dispatchReceiver
                    }
                    IrParameterKind.ExtensionReceiver -> {
                        call.arguments[it.indexInParameters] = extensionReceiver
                    }
                    IrParameterKind.Context,
                    IrParameterKind.Regular -> {
                        call.arguments[it.indexInParameters] = args[argIndex++]
                    }
                }
            }
        }
    }

    protected fun IrType.binaryOperator(name: Name, paramType: IrType): IrFunctionSymbol {
        return finderForBuiltins.findFunctions(CallableId(this.classOrFail.owner.classId!!, name))
            .single {
                it.owner.hasShape(
                    dispatchReceiver = true,
                    regularParameters = 1,
                    parameterTypes = listOf(this, paramType)
                )
            }
    }

    private fun binaryOperatorCall(
        lhs: IrExpression,
        rhs: IrExpression,
        name: Name,
        lhsType: IrType = lhs.type,
        rhsType: IrType = rhs.type
    ): IrCallImpl {
        val symbol = lhsType.binaryOperator(name, rhsType)
        return irCall(
            symbol = symbol,
            dispatchReceiver = lhs,
            args = arrayOf(rhs)
        )
    }

    protected fun irAnd(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.AND)
    }

    protected fun irShl(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.SHL, lhsType = int, rhsType = int)
    }

    protected fun irOr(lhs: IrExpression, rhs: IrExpression): IrExpression {
        if (rhs is IrConst && rhs.value == 0) return lhs
        if (lhs is IrConst && lhs.value == 0) return rhs
        val int = context.irBuiltIns.intType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.OR, lhsType = int, rhsType = int)
    }

    protected fun irBooleanOr(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val boolean = context.irBuiltIns.booleanType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.OR, lhsType = boolean, rhsType = boolean)
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
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.XOR, lhsType = int, rhsType = int)
    }

    protected fun irGreater(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        val gt = context.irBuiltIns.greaterFunByOperandType[int.classifierOrFail]
        return irCall(
            symbol = gt!!,
            origin = IrStatementOrigin.GT,
            args = arrayOf(lhs, rhs)
        )
    }

    protected fun irReturn(
        target: IrReturnTargetSymbol,
        value: IrExpression,
        type: IrType = value.type,
    ): IrExpression {
        return IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            target,
            value
        )
    }

    protected fun irReturnVar(
        target: IrReturnTargetSymbol,
        value: IrVariable,
    ): IrExpression {
        return IrReturnImpl(
            value.initializer?.startOffset ?: UNDEFINED_OFFSET,
            value.initializer?.endOffset ?: UNDEFINED_OFFSET,
            value.type,
            target,
            irGet(value)
        )
    }

    /** Compare [lhs] and [rhs] using structural equality (`==`). */
    protected fun irEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irCall(
            symbol = context.irBuiltIns.eqeqSymbol,
            args = arrayOf(lhs, rhs)
        )
    }

    protected fun irNot(value: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.booleanNotSymbol,
            dispatchReceiver = value
        )
    }

    /** Compare [lhs] and [rhs] using structural inequality (`!=`). */
    protected fun irNotEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irNot(irEqual(lhs, rhs))
    }

//        context.irIntrinsics.symbols.intAnd
//        context.irIntrinsics.symbols.getBinaryOperator(name, lhs, rhs)
//        context.irBuiltIns.booleanNotSymbol
//        context.irBuiltIns.eqeqeqSymbol
//        context.irBuiltIns.eqeqSymbol
//        context.irBuiltIns.greaterFunByOperandType

    protected fun irConst(value: Int): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.intType,
        IrConstKind.Int,
        value
    )

    protected fun irConst(value: Long): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.longType,
        IrConstKind.Long,
        value
    )

    protected fun irConst(value: String): IrConst = IrConstImpl(
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

    protected fun irForLoop(
        elementType: IrType,
        subject: IrExpression,
        loopBody: (IrValueDeclaration) -> IrExpression,
    ): IrStatement {
        val getIteratorFunction = subject.type.classOrNull!!.owner.functions
            .single { it.name.asString() == "iterator" }

        val iteratorSymbol = getIteratorFunction.returnType.classOrNull!!
        val iteratorType = if (iteratorSymbol.owner.typeParameters.isNotEmpty()) {
            iteratorSymbol.typeWith(elementType)
        } else {
            iteratorSymbol.defaultType
        }

        val nextSymbol = iteratorSymbol.owner.functions
            .single { it.name.asString() == "next" }
        val hasNextSymbol = iteratorSymbol.owner.functions
            .single { it.name.asString() == "hasNext" }

        val call = irCall(
            symbol = getIteratorFunction.symbol,
            origin = IrStatementOrigin.FOR_LOOP_ITERATOR,
            returnType = iteratorType,
            dispatchReceiver = subject
        )

        val iteratorVar = irTemporary(
            value = call,
            isVar = false,
            name = "tmp0_iterator",
            irType = iteratorType,
            origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
        )
        return irBlock(
            type = builtIns.unitType,
            origin = IrStatementOrigin.FOR_LOOP,
            statements = listOf(
                iteratorVar,
                IrWhileLoopImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    builtIns.unitType,
                    IrStatementOrigin.FOR_LOOP_INNER_WHILE
                ).apply {
                    val loopVar = irTemporary(
                        value = irCall(
                            symbol = nextSymbol.symbol,
                            origin = IrStatementOrigin.FOR_LOOP_NEXT,
                            returnType = elementType,
                            dispatchReceiver = irGet(iteratorVar)
                        ),
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
                        type = builtIns.unitType,
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

    protected fun irTemporary(
        value: IrExpression,
        name: String,
        irType: IrType = value.type,
        isVar: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    ): IrVariableImpl {
        return IrVariableImpl(
            value.startOffset,
            value.endOffset,
            origin,
            IrVariableSymbolImpl(),
            Name.identifier(name),
            irType,
            isVar,
            isConst = false,
            isLateinit = false
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

    protected fun irGetField(field: IrField): IrGetField {
        return IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            field.symbol,
            field.type
        )
    }

    protected fun irIf(condition: IrExpression, body: IrExpression): IrExpression {
        return IrWhenImpl(
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
        endOffset: Int = UNDEFINED_OFFSET,
    ) =
        IrWhenImpl(startOffset, endOffset, type, IrStatementOrigin.IF).apply {
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
        branches: List<IrBranch>,
    ) = IrWhenImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        origin,
        branches
    )

    protected fun irBranch(
        condition: IrExpression,
        result: IrExpression,
    ): IrBranch {
        return IrBranchImpl(condition, result)
    }

    protected fun irElseBranch(
        expression: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ) = IrElseBranchImpl(startOffset, endOffset, irConst(true), expression)

    protected fun irBlock(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        statements: List<IrStatement>,
    ): IrBlock {
        return IrBlockImpl(
            startOffset,
            endOffset,
            type,
            origin,
            statements
        )
    }

    protected fun irComposite(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        statements: List<IrStatement>,
    ): IrExpression {
        return IrCompositeImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            origin,
            statements
        )
    }

    protected fun irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        returnType: IrType,
        body: (IrSimpleFunction) -> Unit,
    ): IrExpression {
        val function = context.irFactory.buildFun {
            this.startOffset = SYNTHETIC_OFFSET
            this.endOffset = SYNTHETIC_OFFSET
            this.returnType = returnType
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
        }.also(body)

        return IrFunctionExpressionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = context.irBuiltIns.functionN(function.parameters.size).typeWith(
                function.parameters.map { it.type } + listOf(function.returnType)
            ),
            origin = IrStatementOrigin.LAMBDA,
            function = function
        )
    }

    private fun IrClass.uniqueStabilityFieldName(): Name = Name.identifier(
        kotlinFqName.asString().replace(".", "_") + ComposeNames.StabilityFlag
    )

    private fun IrClass.uniqueStabilityPropertyName(): Name = Name.identifier(
        kotlinFqName.asString().replace(".", "_") + ComposeNames.StabilityFlagProperty
    )

    private fun IrClass.uniqueStabilityGetterName(): Name = Name.identifier(
        kotlinFqName.asString().replace(".", "_") + ComposeNames.StabilityFlagPropertyGetter
    )

    private fun IrClass.getMetadataStabilityGetterFun(): IrSimpleFunctionSymbol? {
        val suitableFunctions = finderForBuiltins.findFunctions(CallableId(this.packageFqName!!, uniqueStabilityGetterName()))
        return suitableFunctions.firstOrNull()
    }

    private fun IrClass.getRuntimeStabilityValue(): IrExpression? {
        if (context.platform.isJvm()) {
            val stableField = this.makeStabilityFieldJvm()
            return irGetField(stableField)
        } else {
            // since k2.0.10 compiler plugin adds special getter function that should be visible in metadata declarations
            val stabilityGetter = getMetadataStabilityGetterFun()
            if (stabilityGetter != null) {
                return irCall(stabilityGetter)
            }

            // in case we have not found getter function and dependency was compiled with k1.9, we can rely on
            // IrGetField over property backing field because it was generated with `isConst = true` and should be initialized
            val classKotlinVersion =
                ((this as? Fir2IrLazyClass)?.fir?.klibSourceFile as? DeserializedSourceFile)?.library?.versions?.compilerVersion
            if (classKotlinVersion != null && classKotlinVersion.startsWith("1.9")) {
                val stableField = this.buildStabilityProp(false)
                val backingField = stableField.backingField!!

                return irGetField(backingField)
            }

            // if we can not find stability getter function in metadata, dependency was compiled with older version of compiler plugin,
            // so we can not trust value produced from `irGetField` because it may contain uninitialized data on native targets
            //   (there is no guarantees that any of static/toplevel functions were called at this point,
            //    so no guarantees that package initializer was called and field value was initialized)
            // we treat those classes as `Unstable` and produce compilation warning that user may observe additional recompositions
            // and may need to update dependencies to version compiled with newer compiler plugin to get rid of them
            return null
        }
    }

    internal fun IrClass.makeStabilityField(): IrField {
        return if (context.platform.isJvm()) {
            makeStabilityFieldJvm()
        } else {
            makeStabilityFieldNonJvm()
        }
    }

    private fun IrClass.makeStabilityFieldJvm(): IrField {
        return buildStabilityField(ComposeNames.StabilityFlag).also { stabilityField ->
            stabilityField.parent = this@makeStabilityFieldJvm
            declarations += stabilityField
        }
    }

    private fun IrClass.makeStabilityFieldNonJvm(): IrField {
        val prop = this.buildStabilityProp(true)
        return prop.backingField!!
    }

    private fun buildStabilityField(fieldName: Name): IrField {
        return context.irFactory.buildField {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = fieldName
            isStatic = true
            isFinal = true
            type = context.irBuiltIns.intType
            visibility = if (context.platform.isJvm()) DescriptorVisibilities.PUBLIC else DescriptorVisibilities.PRIVATE
        }
    }

    private fun IrClass.buildStabilityProp(buildGetter: Boolean): IrProperty {
        val parent = this.getPackageFragment()

        val propName = this.uniqueStabilityPropertyName()
        val existingProp = parent.declarations.firstOrNull {
            it is IrProperty && it.name == propName
        } as? IrProperty
        if (existingProp != null) {
            return existingProp
        }

        val stabilityField = buildStabilityField(uniqueStabilityFieldName()).also {
            it.parent = parent
        }

        val property = context.irFactory.buildProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = propName
            visibility = this@buildStabilityProp.visibility
        }.also { property ->
            property.parent = parent
            stabilityField.correspondingPropertySymbol = property.symbol
            property.backingField = stabilityField
            parent.addChild(property)
        }

        if (buildGetter) {
            this.buildStabilityGetter(property, parent)
        }

        return property
    }

    private fun IrClass.buildStabilityGetter(stabilityProp: IrProperty, parent: IrPackageFragment) {
        val getterName = uniqueStabilityGetterName()

        val stabilityField = stabilityProp.backingField!!

        // we could have created getter instead of separate function,
        // but `registerFunctionAsMetadataVisible` is not working for field getter for some reason
        // and there is no api to register properties as metadata-visible
        val stabilityGetter = context.irFactory.buildFun {
            startOffset = this@buildStabilityGetter.startOffset
            endOffset = this@buildStabilityGetter.endOffset
            name = getterName
            returnType = stabilityField.type
            visibility = this@buildStabilityGetter.visibility
            origin = IrDeclarationOrigin.GeneratedByPlugin(ComposeCompilerKey)
        }.also { fn ->
            fn.parent = parent
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                +irReturn(irGetField(stabilityField))
            }
            parent.addChild(fn)
            val hiddenDeprecatedAnnotation = hiddenDeprecated("Synthetic declaration generated by the Compose compiler. Please do not use.")
            fn.annotations = if (context.platform.isNative()) {
                listOf(
                    hiddenFromObjC() ?: error("Expected @HiddenFromObjC annotation to be present."),
                    hiddenDeprecatedAnnotation
                )
            } else {
                listOf(hiddenDeprecatedAnnotation)
            }
        }

        context.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(stabilityGetter)
    }

    fun IrExpression.isStatic(): Boolean {
        return when (this) {
            // A constant by definition is static
            is IrConst -> true
            // We want to consider all enum values as static
            is IrGetEnumValue -> true
            // Getting a companion object or top level object can be considered static if the
            // type of that object is Stable. (`Modifier` for instance is a common example)
            is IrGetObjectValue -> {
                if (symbol.owner.isCompanion) true
                else stabilityInferencer.stabilityOf(type).knownStable()
            }

            is IrConstructorCall -> isStatic()
            is IrCall -> isStatic()
            is IrGetValue -> {
                when (val owner = symbol.owner) {
                    is IrVariable -> {
                        // If we have an immutable variable whose initializer is also static,
                        // then we can determine that the variable reference is also static.
                        !owner.isVar && owner.initializer?.isStatic() == true
                    }

                    else -> false
                }
            }

            is IrFunctionExpression,
            is IrTypeOperatorCall ->
                context.irTrace[ComposeWritableSlices.IS_STATIC_FUNCTION_EXPRESSION, this] ?: false

            is IrGetField ->
                // K2 sometimes produces `IrGetField` for reads from constant properties
                symbol.owner.correspondingPropertySymbol?.owner?.isConst == true

            is IrBlock -> {
                // Check the slice in case the block was generated as expression
                // (e.g. inlined intrinsic remember call)
                context.irTrace[ComposeWritableSlices.IS_STATIC_EXPRESSION, this] ?: false
            }
            else -> false
        }
    }

    private fun IrConstructorCall.isStatic(): Boolean {
        // special case constructors of inline classes as static if their underlying
        // value is static.
        if (type.isInlineClassType()) {
            return stabilityInferencer.stabilityOf(type.unboxInlineClass()).knownStable() &&
                    arguments[0]?.isStatic() == true
        }

        // If a type is immutable, then calls to its constructor are static if all of
        // the provided arguments are static.
        if (symbol.owner.parentAsClass.hasAnnotationSafe(ComposeFqNames.Immutable)) {
            return areAllArgumentsStatic()
        }
        return false
    }

    private fun IrStatementOrigin?.isGetProperty() = this == IrStatementOrigin.GET_PROPERTY
    private fun IrStatementOrigin?.isSpecialCaseMathOp() =
        this in setOf(
            IrStatementOrigin.PLUS,
            IrStatementOrigin.MUL,
            IrStatementOrigin.MINUS,
            IrStatementOrigin.ANDAND,
            IrStatementOrigin.OROR,
            IrStatementOrigin.DIV,
            IrStatementOrigin.EQ,
            IrStatementOrigin.EQEQ,
            IrStatementOrigin.EQEQEQ,
            IrStatementOrigin.GT,
            IrStatementOrigin.GTEQ,
            IrStatementOrigin.LT,
            IrStatementOrigin.LTEQ
        )

    private fun IrCall.isStatic(): Boolean {
        val function = symbol.owner
        val fqName = function.kotlinFqName
        return when {
            origin.isGetProperty() -> {
                // If we are in a GET_PROPERTY call, then this should usually resolve to
                // non-null, but in case it doesn't, just return false
                val prop = function.correspondingPropertySymbol?.owner ?: return false

                // if the property is a top level constant, then it is static.
                if (prop.isConst) return true

                val typeIsStable = stabilityInferencer.stabilityOf(type).knownStable()
                val receiversAreStatic = arguments.all { it?.isStatic() != false }

                // if we see that the property is read-only with a default getter and a
                // stable return type , then reading the property can also be considered
                // static if this is a top level property or the subject is also static.
                if (!prop.isVar &&
                    prop.getter?.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                    typeIsStable &&
                    receiversAreStatic
                ) {
                    return true
                }

                val getterIsStable = prop.hasAnnotation(ComposeFqNames.Stable) ||
                        function.hasAnnotation(ComposeFqNames.Stable)

                if (
                    getterIsStable &&
                    typeIsStable &&
                    receiversAreStatic
                ) {
                    return true
                }

                false
            }

            origin.isSpecialCaseMathOp() -> {
                // special case mathematical operators that are in the stdlib. These are
                // immutable operations so the overall result is static if the operands are
                // also static
                val isStableOperator = fqName.topLevelName() == "kotlin" ||
                        function.hasAnnotation(ComposeFqNames.Stable)

                val typeIsStable = stabilityInferencer.stabilityOf(type).knownStable()
                if (!typeIsStable) return false

                if (!isStableOperator) {
                    return false
                }

                getArgumentsWithIr().all { it.second.isStatic() }
            }

            origin == null -> {
                if (fqName == ComposeFqNames.remember) {
                    // if it is a call to remember with 0 input arguments, then we can
                    // consider the value static if the result type of the lambda is stable
                    val syntheticRememberParams = 1 + // composer param
                            1 // changed param
                    val expectedArgumentsCount = 1 + syntheticRememberParams // 1 for lambda
                    if (
                        arguments.size == expectedArgumentsCount &&
                        stabilityInferencer.stabilityOf(type).knownStable()
                    ) {
                        return true
                    }
                } else if (
                    fqName == ComposeFqNames.composableLambda ||
                    fqName == ComposeFqNames.rememberComposableLambda
                ) {
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
                // For functions that we have an exception for, skip these checks. We've already
                // assumed the stability here and can go straight to checking their arguments.
                if (fqName.asString() !in KnownStableConstructs.stableFunctions) {
                    val isStable = symbol.owner.hasAnnotation(ComposeFqNames.Stable)
                    if (!isStable) return false

                    val typeIsStable = stabilityInferencer.stabilityOf(type).knownStable()
                    if (!typeIsStable) return false
                }

                areAllArgumentsStatic()
            }

            else -> false
        }
    }

    private fun IrMemberAccessExpression<*>.areAllArgumentsStatic(): Boolean {
        // getArguments includes the receivers!
        return getArgumentsWithIr().all { (_, argExpression) ->
            when (argExpression) {
                // In a vacuum, we can't assume varargs are static because they're backed by
                // arrays. Arrays aren't stable types due to their implicit mutability and
                // lack of built-in equality checks. But in this context, because the static-ness of
                // an argument is meaningless unless the function call that owns the argument is
                // stable and capable of being static. So in this case, we're able to ignore the
                // array implementation detail and check whether all of the parameters sent in the
                // varargs are static on their own.
                is IrVararg -> argExpression.elements.all { varargElement ->
                    (varargElement as? IrExpression)?.isStatic() ?: false
                }

                else -> argExpression.isStatic()
            }
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
            typeArguments[0] = from
            typeArguments[1] = to
            arguments[0] = argument
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
                @Suppress("DEPRECATION")
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

    /*
     * Delegated accessors are generated with IrReturn(IrCall(<delegated function>)) structure.
     * To verify the delegated function is composable, this function is unpacking it and
     * checks annotation on the symbol owner of the call.
     */
    fun IrFunction.isComposableDelegatedAccessor(): Boolean =
        origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR &&
                body?.let {
                    val returnStatement = it.statements.singleOrNull() as? IrReturn
                    val callStatement = returnStatement?.value as? IrCall
                    val target = callStatement?.symbol?.owner
                    target?.hasComposableAnnotation()
                } == true

    private val cacheFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.cache).first {
            it.owner.parameters.let {
                it.size == 3 &&
                        it[0].kind == IrParameterKind.ExtensionReceiver &&
                        it[1].kind == IrParameterKind.Regular &&
                        it[2].kind == IrParameterKind.Regular
            }
        }.owner
    }

    fun irCache(
        currentComposer: IrExpression,
        startOffset: Int,
        endOffset: Int,
        returnType: IrType,
        invalid: IrExpression,
        calculation: IrExpression,
    ): IrCall {
        val symbol = cacheFunction.symbol
        return IrCallImpl(
            startOffset,
            endOffset,
            returnType,
            symbol,
            symbol.owner.typeParameters.size
        ).apply {
            arguments[0] = currentComposer
            arguments[1] = invalid
            arguments[2] = calculation
            typeArguments[0] = returnType
        }
    }

    fun irChanged(
        currentComposer: IrExpression,
        value: IrExpression,
        inferredStable: Boolean,
        compareInstanceForFunctionTypes: Boolean,
        compareInstanceForUnstableValues: Boolean,
    ): IrExpression {
        // compose has a unique opportunity to avoid inline class boxing for changed calls, since
        // we know that the only thing that we are detecting here is "changed or not", we can
        // just as easily pass in the underlying value, which will avoid boxing to check for
        // equality on recompositions. As a result here we want to pass in the underlying
        // property value for inline classes, not the instance itself. The inline class lowering
        // will turn this into just passing the wrapped value later on. If the type is already
        // boxed, then we don't want to unnecessarily _unbox_ it. Note that if Kotlin allows for
        // an overridden equals method of inline classes in the future, we may have to avoid the
        // boxing in a different way.
        val expr = value.unboxValueIfInline().ordinalIfEnum()
        val type = expr.type
        val stability = stabilityInferencer.stabilityOf(value)

        val primitiveDescriptor = type.toPrimitiveType()
            .let { changedPrimitiveFunctions[it] }

        return if (!compareInstanceForUnstableValues) {
            val descriptor = primitiveDescriptor
                ?: if (type.isFunction() && compareInstanceForFunctionTypes) {
                    changedInstanceFunction
                } else {
                    changedFunction
                }
            irMethodCall(currentComposer, descriptor).also {
                // 0th argument is the composer
                it.arguments[1] = expr
            }
        } else {
            val descriptor = when {
                primitiveDescriptor != null -> primitiveDescriptor
                compareInstanceForFunctionTypes && type.isFunction() -> changedInstanceFunction
                stability.knownStable() -> changedFunction
                inferredStable -> changedFunction
                stability.knownUnstable() -> changedInstanceFunction
                stability.isUncertain() -> changedInstanceFunction
                else -> error("Cannot determine descriptor for irChanged")
            }
            irMethodCall(currentComposer, descriptor).also {
                // 0th argument is the composer
                it.arguments[1] = expr
            }
        }
    }

    private val irEnumOrdinal =
        context.irBuiltIns.enumClass.owner.properties.single { it.name.asString() == "ordinal" }.getter!!

    private val protobufEnumClassId = ClassId.fromString("com/google/protobuf/Internal/EnumLite")

    private fun IrExpression.ordinalIfEnum(): IrExpression {
        val cls = type.classOrNull?.owner
        return when (cls?.kind) {
            ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY -> {
                val function = if (cls.isSubclassOf(protobufEnumClassId)) {
                    // For protobuf enums, we need to use the `getNumber` method instead of `ordinal`
                    cls.functions
                        .single {
                            it.name.asString() == "getNumber" &&
                                    it.parameters.size == 1 &&
                                    it.parameters[0].kind == IrParameterKind.DispatchReceiver
                        }
                } else {
                    irEnumOrdinal
                }
                if (type.isNullable()) {
                    val enumValue = irTemporary(this, "tmpEnum")
                    irBlock(
                        context.irBuiltIns.intType,
                        statements = listOf(
                            enumValue,
                            irIfThenElse(
                                type = context.irBuiltIns.intType,
                                condition = irEqual(irGet(enumValue), irNull()),
                                thenPart = irConst(-1),
                                elsePart = irCall(function.symbol, dispatchReceiver = irGet(enumValue))
                            )
                        )
                    )
                } else {
                    irCall(function.symbol, dispatchReceiver = this)
                }
            }
            else -> {
                this
            }
        }
    }

    fun irStartReplaceGroup(
        currentComposer: IrExpression,
        key: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrExpression {
        return irMethodCall(
            currentComposer,
            startReplaceFunction,
            startOffset,
            endOffset
        ).also {
            // 0th argument is the composer
            it.arguments[1] = key
        }
    }

    fun irEndReplaceGroup(
        currentComposer: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrExpression {
        return irMethodCall(
            currentComposer,
            endReplaceFunction,
            startOffset,
            endOffset
        )
    }

    fun IrStatement.wrap(
        startOffset: Int = this.startOffset,
        endOffset: Int = this.endOffset,
        type: IrType,
        before: List<IrStatement> = emptyList(),
        after: List<IrStatement> = emptyList(),
    ): IrContainerExpression {
        return IrBlockImpl(
            startOffset,
            endOffset,
            type,
            null,
            before + this + after
        )
    }

    private val changedFunction = composerIrClass.functions
        .first {
            it.name.identifier == "changed" &&
                    it.parameters.first { it.kind == IrParameterKind.Regular }.type.isNullableAny()
        }

    private val changedInstanceFunction = composerIrClass.functions
        .firstOrNull {
            it.name.identifier == "changedInstance" &&
                    it.parameters.first { it.kind == IrParameterKind.Regular }.type.isNullableAny()
        } ?: changedFunction

    private val startReplaceFunction by guardedLazy {
        composerIrClass.functions.firstOrNull {
            it.name.identifier == "startReplaceGroup" && it.parameters.count { it.kind == IrParameterKind.Regular } == 1
        } ?: composerIrClass.functions
            .first {
                it.name.identifier == "startReplaceableGroup" && it.parameters.count { it.kind == IrParameterKind.Regular } == 1
            }
    }

    private val endReplaceFunction by guardedLazy {
        composerIrClass.functions.firstOrNull {
            it.name.identifier == "endReplaceGroup" && it.parameters.none { it.kind == IrParameterKind.Regular }
        } ?: composerIrClass.functions
            .first {
                it.name.identifier == "endReplaceableGroup" && it.parameters.none { it.kind == IrParameterKind.Regular }
            }
    }

    private fun IrType.toPrimitiveType(): PrimitiveType? = when {
        isInt() -> PrimitiveType.INT
        isBoolean() -> PrimitiveType.BOOLEAN
        isFloat() -> PrimitiveType.FLOAT
        isLong() -> PrimitiveType.LONG
        isDouble() -> PrimitiveType.DOUBLE
        isByte() -> PrimitiveType.BYTE
        isChar() -> PrimitiveType.CHAR
        isShort() -> PrimitiveType.SHORT
        else -> null
    }

    private val changedPrimitiveFunctions by guardedLazy {
        composerIrClass
            .functions
            .filter { it.name.identifier == "changed" }
            .mapNotNull { f ->
                f.parameters.first { it.kind == IrParameterKind.Regular }.type.toPrimitiveType()?.let { primitive ->
                    primitive to f
                }
            }
            .toMap()
    }

    fun irMethodCall(
        target: IrExpression,
        function: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrCall {
        return irCall(function, startOffset, endOffset).apply {
            arguments[0] = target
        }
    }

    fun irCall(
        function: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrCall {
        val type = function.returnType
        val symbol = function.symbol
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size
        )
    }

    internal fun IrFunction.copyParametersFrom(original: IrFunction, copyDefaultValues: Boolean = true) {
        val newFunction = this
        // here generic value parameters will be applied
        newFunction.copyTypeParametersFrom(original)

        // ..but we need to remap the return type as well
        newFunction.returnType = newFunction.returnType.remapTypeParameters(
            source = original,
            target = newFunction
        )
        newFunction.parameters = original.parameters.map {
            when (it.kind) {
                IrParameterKind.ExtensionReceiver,
                IrParameterKind.DispatchReceiver -> {
                    it.copyWithNewTypeParams(original, newFunction)
                }
                IrParameterKind.Context,
                IrParameterKind.Regular -> {
                    val name = dexSafeName(it.name)
                    it.copyTo(
                        newFunction,
                        name = name,
                        type = it.type.remapTypeParameters(original, newFunction),
                        // remapping the type parameters explicitly
                        defaultValue = if (copyDefaultValues) {
                            it.defaultValue?.copyWithNewTypeParams(original, newFunction)
                        } else {
                            null
                        }
                    )
                }
            }
        }

        if (copyDefaultValues) {
            newFunction.parameters.forEach {
                it.defaultValue?.transformDefaultValue(
                    originalFunction = original,
                    newFunction = newFunction
                )
            }
        }
    }

    /**
     *  Expressions for default values can use other parameters.
     *  In such cases we need to ensure that default values expressions use parameters of the new
     *  function (new/copied value parameters).
     *
     *  Example:
     *  fun Foo(a: String, b: String = a) {...}
     */
    private fun IrExpressionBody.transformDefaultValue(
        originalFunction: IrFunction,
        newFunction: IrFunction,
    ) {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val original = super.visitGetValue(expression)
                val parameter =
                    (expression.symbol.owner as? IrValueParameter) ?: return original

                val parameterIndex = parameter.indexInParameters
                if (parameter.parent != originalFunction) {
                    return super.visitGetValue(expression)
                }
                return IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    newFunction.parameters[parameterIndex].symbol,
                    expression.origin
                )
            }
        })
    }

    protected var IrDeclaration.composeMetadata: ComposeMetadata?
        get() = context.metadataDeclarationRegistrar.getCustomMetadataExtension(this, COMPOSE_PLUGIN_ID)
            ?.let { ComposeMetadata(it) }
        set(value) {
            if (value != null && this.hasFirDeclaration()) {
                context.metadataDeclarationRegistrar.addCustomMetadataExtension(this, COMPOSE_PLUGIN_ID, value.data)
            }
        }

    protected val IrFunction.hasNonRestartableAnnotation: Boolean
        get() = hasAnnotation(ComposeFqNames.NonRestartableComposable)

    protected val IrFunction.hasReadOnlyAnnotation: Boolean
        get() = hasAnnotation(ComposeFqNames.ReadOnlyComposable)

    protected val IrFunction.hasExplicitGroups: Boolean
        get() = hasAnnotation(ComposeFqNames.ExplicitGroupsComposable)

    protected val IrFunction.hasNonSkippableAnnotation: Boolean
        get() = hasAnnotation(ComposeFqNames.NonSkippableComposable)

    private val jvmSyntheticIrClass =
        if (context.platform.isJvm()) {
            getTopLevelClass(
                ClassId(StandardClassIds.BASE_JVM_PACKAGE, Name.identifier("JvmSynthetic"))
            ).owner
        } else {
            null
        }

    private val hiddenFromObjCIrClass: IrClass? =
        if (context.platform.isNative()) {
            getTopLevelClass(hiddenFromObjCClassId).owner
        } else {
            null
        }

    private val deprecationLevelIrClass = getTopLevelClass(ClassId.fromString("kotlin/DeprecationLevel")).owner
    private val deprecatedIrClass = getTopLevelClass(ClassId.fromString("kotlin/Deprecated"))
    private val hiddenDeprecationLevel = deprecationLevelIrClass.declarations.filterIsInstance<IrEnumEntry>()
        .single { it.name.toString() == "HIDDEN" }.symbol

    private fun jvmSynthetic() = jvmSyntheticIrClass?.let {
        IrAnnotationImpl.fromSymbolOwner(
            type = it.defaultType,
            constructorSymbol = it.constructors.first().symbol
        )
    }

    private fun hiddenFromObjC() = hiddenFromObjCIrClass?.let {
        IrAnnotationImpl.fromSymbolOwner(
            type = it.defaultType,
            constructorSymbol = it.constructors.first().symbol
        )
    }

    private fun hiddenDeprecated(message: String) = IrAnnotationImpl.fromSymbolOwner(
        type = deprecatedIrClass.defaultType,
        constructorSymbol = deprecatedIrClass.constructors.first { it.owner.isPrimary }
    ).also {
        it.arguments[0] = IrConstImpl.string(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            context.irBuiltIns.stringType,
            message
        )
        it.arguments[2] = IrGetEnumValueImpl(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            deprecationLevelIrClass.defaultType,
            hiddenDeprecationLevel
        )
    }

    protected fun IrSimpleFunction.makeStub(): IrSimpleFunction {
        val source = this
        val copy = source.deepCopyWithSymbols(parent)
        copy.attributeOwnerId = copy
        copy.isDefaultParamStub = true
        val newAnnotations = listOfNotNull(
            jvmSynthetic(),
            hiddenFromObjC(),
            hiddenDeprecated("Binary compatibility stub for default parameters")
        )
        // Remove existing annotations that are overridden by the new ones
        copy.annotations = copy.annotations.filterNot { annotation ->
            newAnnotations.any { it.annotationClass?.owner?.classId == annotation.annotationClass?.owner?.classId }
        }
        copy.annotations += newAnnotations
        copy.body = null
        return copy
    }

    protected fun IrFunction.shouldBeRestartable(): Boolean {
        // Only insert observe scopes in non-empty composable function
        if (body == null || this !is IrSimpleFunction)
            return false

        if (isLocal && parentClassOrNull?.origin != JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
            return false
        }

        // Do not insert observe scope in an inline function
        if (isInline)
            return false

        if (hasNonRestartableAnnotation)
            return false

        if (hasExplicitGroups)
            return false

        // Do not insert an observe scope if the function has a return result
        if (!returnType.isUnit())
            return false

        if (isComposableDelegatedAccessor())
            return false

        // Virtual functions with default params are called through wrapper generated in
        // ComposableDefaultParamLowering. The restartable group is moved to the wrapper, while
        // the function itself is no longer restartable.
        if (isVirtualFunctionWithDefaultParam()) {
            return false
        }

        // Open functions cannot be restartable since restart logic makes a virtual call (todo: b/329477544)
        if (modality == Modality.OPEN && parentClassOrNull?.isFinalClass != true) {
            return false
        }

        // Check if the descriptor has restart scope calls resolved
        // Lambdas should be ignored. All composable lambdas are wrapped by a restartable
        // function wrapper by ComposerLambdaMemoization which supplies the startRestartGroup/
        // endRestartGroup pair on behalf of the lambda.
        return origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    }

    protected fun IrFunction.isVirtualFunctionWithDefaultParam(): Boolean =
        this is IrSimpleFunction &&
                (isVirtualFunctionWithDefaultParam == true ||
                        overriddenSymbols.any { it.owner.isVirtualFunctionWithDefaultParam() })
}

private val unsafeSymbolsRegex = "[ <>]".toRegex()

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrAnnotationContainer.hasAnnotationSafe(fqName: FqName): Boolean =
    annotations.any {
        // compiler helper getAnnotation fails during remapping in [ComposableTypeRemapper], so we
        // use this impl
        fqName == it.annotationClass?.descriptor?.fqNameSafe
    }

// workaround for KT-45361
val IrConstructorCall.annotationClass
    get() = type.classOrNull

fun IrDeclaration.hasFirDeclaration(): Boolean = ((this as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource)?.fir != null

inline fun <T> includeFileNameInExceptionTrace(file: IrFile, body: () -> T): T {
    try {
        return body()
    } catch (e: Exception) {
        rethrowIntellijPlatformExceptionIfNeeded(e)
        throw Exception("IR lowering failed at: ${file.name}", e)
    }
}

fun FqName.topLevelName() =
    asString().substringBefore(".")

private fun IrClass.isSubclassOf(classId: ClassId) =
    superTypes.any { it.classOrNull?.owner?.classId == classId }

internal inline fun <reified T : IrElement> T.copyWithNewTypeParams(
    source: IrFunction,
    target: IrFunction,
): T {
    val typeParamsAwareSymbolRemapper = object : DeepCopySymbolRemapper() {
        init {
            for ((orig, new) in source.typeParameters.zip(target.typeParameters)) {
                typeParameters[orig.symbol] = new.symbol
            }
        }
    }
    val typeRemapper = DeepCopyTypeRemapper(typeParamsAwareSymbolRemapper)
    val typeParamRemapper = object : TypeRemapper by typeRemapper {
        override fun remapType(type: IrType): IrType {
            return typeRemapper.remapType(type.remapTypeParameters(source, target))
        }
    }

    val deepCopy = DeepCopyPreservingMetadata(typeParamsAwareSymbolRemapper, typeParamRemapper)
    typeRemapper.deepCopy = deepCopy

    acceptVoid(typeParamsAwareSymbolRemapper)
    return transform(deepCopy, null).patchDeclarationParents(target) as T
}

fun IrType.isSyntheticComposableFunction() =
    classOrNull?.owner?.let {
        it.name.asString().startsWith("ComposableFunction") &&
                it.packageFqName == InternalPackage
    } ?: false

fun IrType.isKComposableFunction() =
    classOrNull?.owner?.let {
        it.name.asString().startsWith("KComposableFunction") &&
                it.packageFqName == InternalPackage
    } ?: false

fun IrFunction.firstParameterOfKind(kind: IrParameterKind) =
    parameters.firstOrNull { it.kind == kind }

val IrFunction.namedParameters
    get() = parameters.filter { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }

val IrValueParameter.isReceiver
    get() = kind == IrParameterKind.ExtensionReceiver || kind == IrParameterKind.DispatchReceiver
