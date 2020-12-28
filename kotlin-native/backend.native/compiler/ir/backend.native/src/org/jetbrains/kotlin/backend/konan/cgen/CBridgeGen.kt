package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.common.ir.addFakeOverrides
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.getObjCMethodInfo
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.ir.getAnnotationArgumentValue
import org.jetbrains.kotlin.backend.konan.ir.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.isObjCMetaClass
import org.jetbrains.kotlin.backend.konan.lower.FunctionReferenceLowering
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

internal interface KotlinStubs {
    val irBuiltIns: IrBuiltIns
    val symbols: KonanSymbols
    val target: KonanTarget
    fun addKotlin(declaration: IrDeclaration)
    fun addC(lines: List<String>)
    fun getUniqueCName(prefix: String): String
    fun getUniqueKotlinFunctionReferenceClassName(prefix: String): String

    fun throwCompilerError(element: IrElement?, message: String): Nothing
    fun renderCompilerError(element: IrElement?, message: String = "Failed requirement."): String
}

private class KotlinToCCallBuilder(
        val irBuilder: IrBuilderWithScope,
        val stubs: KotlinStubs,
        val isObjCMethod: Boolean,
        foreignExceptionMode: ForeignExceptionMode.Mode
) {

    val cBridgeName = stubs.getUniqueCName("knbridge")

    val symbols: KonanSymbols get() = stubs.symbols

    val bridgeCallBuilder = KotlinCallBuilder(irBuilder, symbols)
    val bridgeBuilder = KotlinCBridgeBuilder(irBuilder.startOffset, irBuilder.endOffset, cBridgeName, stubs, isKotlinToC = true, foreignExceptionMode)
    val cBridgeBodyLines = mutableListOf<String>()
    val cCallBuilder = CCallBuilder()
    val cFunctionBuilder = CFunctionBuilder()

}

private fun KotlinToCCallBuilder.passThroughBridge(argument: IrExpression, kotlinType: IrType, cType: CType): CVariable {
    bridgeCallBuilder.arguments += argument
    return bridgeBuilder.addParameter(kotlinType, cType).second
}

private fun KotlinToCCallBuilder.addArgument(
        argument: IrExpression,
        type: IrType,
        variadic: Boolean,
        parameter: IrValueParameter?
) {
    val argumentPassing = mapCalleeFunctionParameter(type, variadic, parameter, argument)
    addArgument(argument, argumentPassing, variadic)
}

private fun KotlinToCCallBuilder.addArgument(
        argument: IrExpression,
        argumentPassing: KotlinToCArgumentPassing,
        variadic: Boolean
) {
    val cArgument = with(argumentPassing) { passValue(argument) } ?: return
    cCallBuilder.arguments += cArgument.expression
    if (!variadic) cFunctionBuilder.addParameter(cArgument.type)
}

private fun KotlinToCCallBuilder.buildKotlinBridgeCall(transformCall: (IrMemberAccessExpression<*>) -> IrExpression = { it }): IrExpression =
        bridgeCallBuilder.build(
                bridgeBuilder.buildKotlinBridge().also {
                    this.stubs.addKotlin(it)
                },
                transformCall
        )

internal fun KotlinStubs.generateCCall(expression: IrCall, builder: IrBuilderWithScope, isInvoke: Boolean,
                                       foreignExceptionMode: ForeignExceptionMode.Mode = ForeignExceptionMode.default): IrExpression {
    require(expression.dispatchReceiver == null) { renderCompilerError(expression) }

    val callBuilder = KotlinToCCallBuilder(builder, this, isObjCMethod = false, foreignExceptionMode)

    val callee = expression.symbol.owner

    // TODO: consider computing all arguments before converting.

    val targetPtrParameter: String?
    val targetFunctionName: String

    if (isInvoke) {
        targetPtrParameter = callBuilder.passThroughBridge(
                expression.extensionReceiver!!,
                symbols.interopCPointer.typeWithStarProjections,
                CTypes.voidPtr
        ).name
        targetFunctionName = "targetPtr"

        (0 until expression.valueArgumentsCount).forEach {
            callBuilder.addArgument(
                    expression.getValueArgument(it)!!,
                    type = expression.getTypeArgument(it)!!,
                    variadic = false,
                    parameter = null
            )
        }
    } else {
        require(expression.extensionReceiver == null) { renderCompilerError(expression) }
        targetPtrParameter = null
        targetFunctionName = this.getUniqueCName("target")

        val arguments = (0 until expression.valueArgumentsCount).map {
            expression.getValueArgument(it)
        }
        callBuilder.addArguments(arguments, callee)
    }

    val returnValuePassing = if (isInvoke) {
        val returnType = expression.getTypeArgument(expression.typeArgumentsCount - 1)!!
        mapReturnType(returnType, expression, signature = null)
    } else {
        mapReturnType(callee.returnType, expression, signature = callee)
    }

    val result = callBuilder.buildCall(targetFunctionName, returnValuePassing)

    val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)

    if (isInvoke) {
        callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = ${targetPtrParameter!!};")
    } else {
        val cCallSymbolName = callee.getAnnotationArgumentValue<String>(RuntimeNames.cCall, "id")!!
        this.addC(listOf("extern const $targetFunctionVariable __asm(\"$cCallSymbolName\");")) // Exported from cinterop stubs.
    }

    callBuilder.emitCBridge()

    return result
}

private fun KotlinToCCallBuilder.addArguments(arguments: List<IrExpression?>, callee: IrFunction) {
    arguments.forEachIndexed { index, argument ->
        val parameter = callee.valueParameters[index]
        if (parameter.isVararg) {
            require(index == arguments.lastIndex) { stubs.renderCompilerError(argument) }
            addVariadicArguments(argument)
            cFunctionBuilder.variadic = true
        } else {
            addArgument(argument!!, parameter.type, variadic = false, parameter = parameter)
        }
    }
}

private fun KotlinToCCallBuilder.addVariadicArguments(
        argumentForVarargParameter: IrExpression?
) = handleArgumentForVarargParameter(argumentForVarargParameter) { variable, elements ->
    if (variable == null) {
        unwrapVariadicArguments(elements).forEach {
            addArgument(it, it.type, variadic = true, parameter = null)
        }
    } else {
        // See comment in [handleArgumentForVarargParameter].
        // Array for this vararg parameter is already computed before the call,
        // so query statically known typed arguments from this array.

        with(irBuilder) {
            val argumentTypes = unwrapVariadicArguments(elements).map { it.type }
            argumentTypes.forEachIndexed { index, type ->
                val untypedArgument = irCall(symbols.arrayGet[symbols.array]!!.owner).apply {
                    dispatchReceiver = irGet(variable)
                    putValueArgument(0, irInt(index))
                }
                val argument = irAs(untypedArgument, type) // Note: this cast always succeeds.
                addArgument(argument, type, variadic = true, parameter = null)
            }
        }
    }
}

private fun KotlinToCCallBuilder.unwrapVariadicArguments(
        elements: List<IrVarargElement>
): List<IrExpression> = elements.flatMap {
    when (it) {
        is IrExpression -> listOf(it)
        is IrSpreadElement -> {
            val expression = it.expression
            require(expression is IrCall && expression.symbol == symbols.arrayOf) { stubs.renderCompilerError(it) }
            handleArgumentForVarargParameter(expression.getValueArgument(0)) { _, elements ->
                unwrapVariadicArguments(elements)
            }
        }
        else -> stubs.throwCompilerError(it, "unexpected IrVarargElement")
    }
}

private fun <R> KotlinToCCallBuilder.handleArgumentForVarargParameter(
        argument: IrExpression?,
        block: (variable: IrVariable?, elements: List<IrVarargElement>) -> R
): R = when (argument) {

    null -> block(null, emptyList())

    is IrVararg -> block(null, argument.elements)

    is IrGetValue -> {
        /* This is possible when using named arguments with reordering, i.e.
         *
         *   foo(second = *arrayOf(...), first = ...)
         *
         * psi2ir generates as
         *
         *   val secondTmp = *arrayOf(...)
         *   val firstTmp = ...
         *   foo(firstTmp, secondTmp)
         *
         *
         **/

        val variable = argument.symbol.owner
        if (variable is IrVariable && variable.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE && !variable.isVar) {
            val initializer = variable.initializer
            require(initializer is IrVararg) { stubs.renderCompilerError(initializer) }
            block(variable, initializer.elements)
        } else if (variable is IrValueParameter && FunctionReferenceLowering.isLoweredFunctionReference(variable)) {
            val location = variable.parent // Parameter itself has incorrect location.
            val kind = if (this.isObjCMethod) "Objective-C methods" else "C functions"
            stubs.throwCompilerError(location, "callable references to variadic $kind are not supported")
        } else {
            stubs.throwCompilerError(variable, "unexpected value declaration")
        }
    }

    else -> stubs.throwCompilerError(argument, "unexpected vararg")
}

private fun KotlinToCCallBuilder.emitCBridge() {
    val cLines = mutableListOf<String>()

    cLines += "${bridgeBuilder.buildCSignature(cBridgeName)} {"
    cLines += cBridgeBodyLines
    cLines += "}"

    stubs.addC(cLines)
}

private fun KotlinToCCallBuilder.buildCall(
        targetFunctionName: String,
        returnValuePassing: ValueReturning
): IrExpression = with(returnValuePassing) {
    returnValue(cCallBuilder.build(targetFunctionName))
}

internal sealed class ObjCCallReceiver {
    class Regular(val rawPtr: IrExpression) : ObjCCallReceiver()
    class Retained(val rawPtr: IrExpression) : ObjCCallReceiver()
}

internal fun KotlinStubs.generateObjCCall(
        builder: IrBuilderWithScope,
        method: IrSimpleFunction,
        isStret: Boolean,
        selector: String,
        call: IrFunctionAccessExpression,
        superQualifier: IrClassSymbol?,
        receiver: ObjCCallReceiver,
        arguments: List<IrExpression?>
) = builder.irBlock {
    val resolved = method.resolveFakeOverride(allowAbstract = true)?: method
    val exceptionMode = ForeignExceptionMode.byValue(
            resolved.konanLibrary?.manifestProperties
                    ?.getProperty(ForeignExceptionMode.manifestKey)
    )

    val callBuilder = KotlinToCCallBuilder(builder, this@generateObjCCall, isObjCMethod = true, exceptionMode)

    val superClass = irTemporary(
            superQualifier?.let { getObjCClass(symbols, it) } ?: irNullNativePtr(symbols),
            isMutable = true
    )

    val messenger = irCall(if (isStret) {
        symbols.interopGetMessengerStret
    } else {
        symbols.interopGetMessenger
    }.owner).apply {
        putValueArgument(0, irGet(superClass)) // TODO: check superClass statically.
    }

    val targetPtrParameter = callBuilder.passThroughBridge(
            messenger,
            symbols.interopCPointer.typeWithStarProjections,
            CTypes.voidPtr
    ).name
    val targetFunctionName = "targetPtr"

    val preparedReceiver = if (method.objCConsumesReceiver()) {
        when (receiver) {
            is ObjCCallReceiver.Regular -> irCall(symbols.interopObjCRetain.owner).apply {
                putValueArgument(0, receiver.rawPtr)
            }

            is ObjCCallReceiver.Retained -> receiver.rawPtr
        }
    } else {
        when (receiver) {
            is ObjCCallReceiver.Regular -> receiver.rawPtr

            is ObjCCallReceiver.Retained -> {
                // Note: shall not happen: Retained is used only for alloc result currently,
                // which is used only as receiver for init methods, which are always receiver-consuming.
                // Can't even add a test for the code below.
                val rawPtrVar = scope.createTemporaryVariable(receiver.rawPtr)
                callBuilder.bridgeCallBuilder.prepare += rawPtrVar
                callBuilder.bridgeCallBuilder.cleanup += {
                    irCall(symbols.interopObjCRelease).apply {
                        putValueArgument(0, irGet(rawPtrVar)) // Balance retained pointer.
                    }
                }
                irGet(rawPtrVar)
            }
        }
    }

    val receiverOrSuper = if (superQualifier != null) {
        irCall(symbols.interopCreateObjCSuperStruct.owner).apply {
            putValueArgument(0, preparedReceiver)
            putValueArgument(1, irGet(superClass))
        }
    } else {
        preparedReceiver
    }

    callBuilder.cCallBuilder.arguments += callBuilder.passThroughBridge(
            receiverOrSuper, symbols.nativePtrType, CTypes.voidPtr).name
    callBuilder.cFunctionBuilder.addParameter(CTypes.voidPtr)

    callBuilder.cCallBuilder.arguments += "@selector($selector)"
    callBuilder.cFunctionBuilder.addParameter(CTypes.voidPtr)

    callBuilder.addArguments(arguments, method)

    val returnValuePassing = mapReturnType(method.returnType, call, signature = method)

    val result = callBuilder.buildCall(targetFunctionName, returnValuePassing)

    val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)
    callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = $targetPtrParameter;")

    callBuilder.emitCBridge()

    +result
}

internal fun IrBuilderWithScope.getObjCClass(symbols: KonanSymbols, symbol: IrClassSymbol): IrExpression {
    val classDescriptor = symbol.descriptor
    require(!classDescriptor.isObjCMetaClass())
    return irCall(symbols.interopGetObjCClass, symbols.nativePtrType, listOf(symbol.typeWithStarProjections))
}

private fun IrBuilderWithScope.irNullNativePtr(symbols: KonanSymbols) = irCall(symbols.getNativeNullPtr.owner)

private class CCallbackBuilder(
        val stubs: KotlinStubs,
        val location: IrElement,
        val isObjCMethod: Boolean
) {

    val irBuiltIns: IrBuiltIns get() = stubs.irBuiltIns
    val symbols: KonanSymbols get() = stubs.symbols

    private val cBridgeName = stubs.getUniqueCName("knbridge")

    fun buildCBridgeCall(): String = cBridgeCallBuilder.build(cBridgeName)
    fun buildCBridge(): String = bridgeBuilder.buildCSignature(cBridgeName)

    val bridgeBuilder = KotlinCBridgeBuilder(location.startOffset, location.endOffset, cBridgeName, stubs, isKotlinToC = false)
    val kotlinCallBuilder = KotlinCallBuilder(bridgeBuilder.kotlinIrBuilder, symbols)
    val kotlinBridgeStatements = mutableListOf<IrStatement>()
    val cBridgeCallBuilder = CCallBuilder()
    val cBodyLines = mutableListOf<String>()
    val cFunctionBuilder = CFunctionBuilder()

}

private fun CCallbackBuilder.passThroughBridge(
        cBridgeArgument: String,
        cBridgeParameterType: CType,
        kotlinBridgeParameterType: IrType
): IrValueParameter {
    cBridgeCallBuilder.arguments += cBridgeArgument
    return bridgeBuilder.addParameter(kotlinBridgeParameterType, cBridgeParameterType).first
}

private fun CCallbackBuilder.addParameter(it: IrValueParameter, functionParameter: IrValueParameter) {
    val location = if (isObjCMethod) functionParameter else location
    require(!functionParameter.isVararg) { stubs.renderCompilerError(location) }

    val valuePassing = stubs.mapFunctionParameterType(
            it.type,
            retained = it.isObjCConsumed(),
            variadic = false,
            location = location
    )

    val kotlinArgument = with(valuePassing) { receiveValue() }
    kotlinCallBuilder.arguments += kotlinArgument
}

private fun CCallbackBuilder.build(function: IrSimpleFunction, signature: IrSimpleFunction): String {
    val valueReturning = stubs.mapReturnType(
            signature.returnType,
            location = if (isObjCMethod) function else location,
            signature = signature
    )
    buildValueReturn(function, valueReturning)
    return buildCFunction()
}

private fun CCallbackBuilder.buildValueReturn(function: IrSimpleFunction, valueReturning: ValueReturning) {
    val kotlinCall = kotlinCallBuilder.build(function)
    with(valueReturning) {
        returnValue(kotlinCall)
    }

    val kotlinBridge = bridgeBuilder.buildKotlinBridge()
    kotlinBridge.body = bridgeBuilder.kotlinIrBuilder.irBlockBody {
        kotlinBridgeStatements.forEach { +it }
    }
    stubs.addKotlin(kotlinBridge)

    stubs.addC(listOf("${buildCBridge()};"))
}

private fun CCallbackBuilder.buildCFunction(): String {
    val result = stubs.getUniqueCName("kncfun")

    val cLines = mutableListOf<String>()

    cLines += "${cFunctionBuilder.buildSignature(result)} {"
    cLines += cBodyLines
    cLines += "}"

    stubs.addC(cLines)

    return result
}

private fun KotlinStubs.generateCFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
): String {
    val callbackBuilder = CCallbackBuilder(this, location, isObjCMethod)

    if (isObjCMethod) {
        val receiver = signature.dispatchReceiverParameter!!
        require(receiver.type.isObjCReferenceType(target, irBuiltIns)) { renderCompilerError(signature) }
        val valuePassing = ObjCReferenceValuePassing(symbols, receiver.type, retained = signature.objCConsumesReceiver())
        val kotlinArgument = with(valuePassing) { callbackBuilder.receiveValue() }
        callbackBuilder.kotlinCallBuilder.arguments += kotlinArgument

        // Selector is ignored:
        with(TrivialValuePassing(symbols.nativePtrType, CTypes.voidPtr)) { callbackBuilder.receiveValue() }
    } else {
        require(signature.dispatchReceiverParameter == null) { renderCompilerError(signature) }
    }

    signature.extensionReceiverParameter?.let { callbackBuilder.addParameter(it, function.extensionReceiverParameter!!) }

    signature.valueParameters.forEach {
        callbackBuilder.addParameter(it, function.valueParameters[it.index])
    }

    return callbackBuilder.build(function, signature)
}

internal fun KotlinStubs.generateCFunctionPointer(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        expression: IrExpression
): IrExpression {
    val fakeFunction = generateCFunctionAndFakeKotlinExternalFunction(
            function,
            signature,
            isObjCMethod = false,
            location = expression
    )
    addKotlin(fakeFunction)

    return IrFunctionReferenceImpl.fromSymbolDescriptor(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            fakeFunction.symbol,
            typeArgumentsCount = 0,
            reflectionTarget = null
    )
}

internal fun KotlinStubs.generateCFunctionAndFakeKotlinExternalFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
): IrSimpleFunction {
    val cFunction = generateCFunction(function, signature, isObjCMethod, location)
    return createFakeKotlinExternalFunction(signature, cFunction, isObjCMethod)
}

private fun KotlinStubs.createFakeKotlinExternalFunction(
        signature: IrSimpleFunction,
        cFunctionName: String,
        isObjCMethod: Boolean
): IrSimpleFunction {
    val bridge = IrFunctionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(),
            Name.identifier(cFunctionName),
            DescriptorVisibilities.PRIVATE,
            Modality.FINAL,
            signature.returnType,
            isInline = false,
            isExternal = true,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false
    )

    bridge.annotations += buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            symbols.symbolName.owner, cFunctionName)

    if (isObjCMethod) {
        val methodInfo = signature.getObjCMethodInfo()!!
        bridge.annotations += buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                symbols.objCMethodImp.owner, methodInfo.selector, methodInfo.encoding)
    }

    return bridge
}

private fun getCStructType(kotlinClass: IrClass): CType? =
        kotlinClass.getCStructSpelling()?.let { CTypes.simple(it) }

private fun KotlinStubs.getNamedCStructType(kotlinClass: IrClass): CType? {
    val cStructType = getCStructType(kotlinClass) ?: return null
    val name = getUniqueCName("struct")
    addC(listOf("typedef ${cStructType.render(name)};"))
    return CTypes.simple(name)
}

// TODO: rework Boolean support.
// TODO: What should be used on watchOS?
internal fun cBoolType(target: KonanTarget): CType? = when (target.family) {
    Family.IOS, Family.TVOS, Family.WATCHOS -> CTypes.C99Bool
    else -> CTypes.signedChar
}

private fun KotlinToCCallBuilder.mapCalleeFunctionParameter(
        type: IrType,
        variadic: Boolean,
        parameter: IrValueParameter?,
        argument: IrExpression
): KotlinToCArgumentPassing {
    val classifier = type.classifierOrNull
    return when {
        classifier == symbols.interopCValues || // Note: this should not be accepted, but is required for compatibility
                classifier == symbols.interopCValuesRef -> CValuesRefArgumentPassing

        classifier == symbols.string && (variadic || parameter?.isCStringParameter() == true) -> {
            require(!variadic || !isObjCMethod) { stubs.renderCompilerError(argument) }
            CStringArgumentPassing()
        }

        classifier == symbols.string && parameter?.isWCStringParameter() == true ->
            WCStringArgumentPassing()

        else -> stubs.mapFunctionParameterType(
                type,
                retained = parameter?.isObjCConsumed() ?: false,
                variadic = variadic,
                location = argument
        )
    }
}

private fun KotlinStubs.mapFunctionParameterType(
        type: IrType,
        retained: Boolean,
        variadic: Boolean,
        location: IrElement
): ArgumentPassing = when {
    type.isUnit() && !variadic -> IgnoredUnitArgumentPassing
    else -> mapType(type, retained = retained, variadic = variadic, location = location)
}

private fun KotlinStubs.mapReturnType(
        type: IrType,
        location: IrElement,
        signature: IrSimpleFunction?
): ValueReturning = when {
    type.isUnit() -> VoidReturning
    else -> mapType(type, retained = signature?.objCReturnsRetained() ?: false, variadic = false, location = location)
}

private fun KotlinStubs.mapBlockType(
        type: IrType,
        retained: Boolean,
        location: IrElement
): ObjCBlockPointerValuePassing {
    require(type is IrSimpleType) { renderCompilerError(location) }
    require(type.classifier == symbols.functionN(type.arguments.size - 1)) { renderCompilerError(location) }

    val returnTypeArgument = type.arguments.last()
    require(returnTypeArgument is IrTypeProjection) { renderCompilerError(location) }
    require(returnTypeArgument.variance == Variance.INVARIANT) { renderCompilerError(location) }
    val valueReturning = mapReturnType(returnTypeArgument.type, location, null)

    val parameterValuePassings = type.arguments.dropLast(1).map { argument ->
        require(argument is IrTypeProjection) { renderCompilerError(location) }
        require(argument.variance == Variance.INVARIANT) { renderCompilerError(location) }
        mapType(
                argument.type,
                retained = false,
                variadic = false,
                location = location
        )
    }
    return ObjCBlockPointerValuePassing(
            this,
            location,
            type,
            valueReturning,
            parameterValuePassings,
            retained
    )
}

private fun KotlinStubs.mapType(
        type: IrType,
        retained: Boolean,
        variadic: Boolean,
        location: IrElement
): ValuePassing = when {
    type.isBoolean() -> {
        val cBoolType = cBoolType(target)
        require(cBoolType != null) { renderCompilerError(location) }
        BooleanValuePassing(cBoolType, irBuiltIns)
    }

    type.isByte() -> TrivialValuePassing(irBuiltIns.byteType, CTypes.signedChar)
    type.isShort() -> TrivialValuePassing(irBuiltIns.shortType, CTypes.short)
    type.isInt() -> TrivialValuePassing(irBuiltIns.intType, CTypes.int)
    type.isLong() -> TrivialValuePassing(irBuiltIns.longType, CTypes.longLong)
    type.isFloat() -> TrivialValuePassing(irBuiltIns.floatType, CTypes.float)
    type.isDouble() -> TrivialValuePassing(irBuiltIns.doubleType, CTypes.double)
    type.isCPointer(symbols) -> TrivialValuePassing(type, CTypes.voidPtr)
    type.isTypeOfNullLiteral() && variadic  -> TrivialValuePassing(symbols.interopCPointer.typeWithStarProjections.makeNullable(), CTypes.voidPtr)
    type.isUByte() -> UnsignedValuePassing(type, CTypes.signedChar, CTypes.unsignedChar)
    type.isUShort() -> UnsignedValuePassing(type, CTypes.short, CTypes.unsignedShort)
    type.isUInt() -> UnsignedValuePassing(type, CTypes.int, CTypes.unsignedInt)
    type.isULong() -> UnsignedValuePassing(type, CTypes.longLong, CTypes.unsignedLongLong)

    type.isVector() -> TrivialValuePassing(type, CTypes.vector128)

    type.isCEnumType() -> {
        val enumClass = type.getClass()!!
        val value = enumClass.declarations
            .filterIsInstance<IrProperty>()
            .single { it.name.asString() == "value" }

        CEnumValuePassing(
                enumClass,
                value,
                mapType(value.getter!!.returnType, retained, variadic, location) as SimpleValuePassing
        )
    }

    type.isCValue(symbols) -> {
        require(!type.isNullable()) { renderCompilerError(location) }
        val kotlinClass = (type as IrSimpleType).arguments.singleOrNull()?.typeOrNull?.getClass()
        require(kotlinClass != null) { renderCompilerError(location) }
        val cStructType = getNamedCStructType(kotlinClass)
        require(cStructType != null) { renderCompilerError(location) }

        StructValuePassing(kotlinClass, cStructType)
    }

    type.classOrNull?.isSubtypeOfClass(symbols.nativePointed) == true -> {
        TrivialValuePassing(type, CTypes.voidPtr)
    }

    type.isFunction() -> {
        require(!variadic) { renderCompilerError(location) }
        mapBlockType(type, retained = retained, location = location)
    }

    type.isObjCReferenceType(target, irBuiltIns) -> ObjCReferenceValuePassing(symbols, type, retained = retained)

    else -> throwCompilerError(location, "doesn't correspond to any C type")
}

private class CExpression(val expression: String, val type: CType)

private interface KotlinToCArgumentPassing {
    fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression?
}

private interface ValueReturning {
    val cType: CType

    fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression
    fun CCallbackBuilder.returnValue(expression: IrExpression)
}

private interface ArgumentPassing : KotlinToCArgumentPassing {
    fun CCallbackBuilder.receiveValue(): IrExpression
}

private interface ValuePassing : ArgumentPassing, ValueReturning

private abstract class SimpleValuePassing : ValuePassing {
    abstract val kotlinBridgeType: IrType
    abstract val cBridgeType: CType
    override abstract val cType: CType
    open val callbackParameterCType get() = cType

    abstract fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression
    open fun IrBuilderWithScope.kotlinCallbackResultToBridged(expression: IrExpression): IrExpression =
            kotlinToBridged(expression)

    abstract fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression
    abstract fun bridgedToC(expression: String): String
    abstract fun cToBridged(expression: String): String

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val bridgeArgument = irBuilder.kotlinToBridged(expression)
        val cBridgeValue = passThroughBridge(bridgeArgument, kotlinBridgeType, cBridgeType).name
        return CExpression(bridgedToC(cBridgeValue), cType)
    }

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(kotlinBridgeType, cBridgeType)
        cBridgeBodyLines.add("return ${cToBridged(expression)};")
        val kotlinBridgeCall = buildKotlinBridgeCall()
        return irBuilder.bridgedToKotlin(kotlinBridgeCall, symbols)
    }

    override fun CCallbackBuilder.receiveValue(): IrExpression {
        val cParameter = cFunctionBuilder.addParameter(callbackParameterCType)
        val cBridgeArgument = cToBridged(cParameter.name)
        val kotlinParameter = passThroughBridge(cBridgeArgument, cBridgeType, kotlinBridgeType)
        return with(bridgeBuilder.kotlinIrBuilder) {
            bridgedToKotlin(irGet(kotlinParameter), symbols)
        }
    }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(kotlinBridgeType, cBridgeType)

        kotlinBridgeStatements += with(bridgeBuilder.kotlinIrBuilder) {
            irReturn(kotlinCallbackResultToBridged(expression))
        }
        val cBridgeCall = buildCBridgeCall()
        cBodyLines += "return ${bridgedToC(cBridgeCall)};"
    }
}

private class TrivialValuePassing(val kotlinType: IrType, override val cType: CType) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = kotlinType
    override val cBridgeType: CType
        get() = cType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression = expression
    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression = expression
    override fun bridgedToC(expression: String): String = expression
    override fun cToBridged(expression: String): String = expression
}

private class UnsignedValuePassing(val kotlinType: IrType, val cSignedType: CType, override val cType: CType) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = kotlinType
    override val cBridgeType: CType
        get() = cSignedType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression = expression

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression = expression

    override fun bridgedToC(expression: String): String = cType.cast(expression)

    override fun cToBridged(expression: String): String = cBridgeType.cast(expression)
}

private class BooleanValuePassing(override val cType: CType, private val irBuiltIns: IrBuiltIns) : SimpleValuePassing() {
    override val cBridgeType: CType get() = CTypes.signedChar
    override val kotlinBridgeType: IrType get() = irBuiltIns.byteType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression = irIfThenElse(
            irBuiltIns.byteType,
            condition = expression,
            thenPart = IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 1),
            elsePart = IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 0)
    )

    override fun IrBuilderWithScope.bridgedToKotlin(
            expression: IrExpression,
            symbols: KonanSymbols
    ): IrExpression = irNot(irCall(symbols.areEqualByValue[PrimitiveBinaryType.BYTE]!!.owner).apply {
        putValueArgument(0, expression)
        putValueArgument(1, IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 0))
    })

    override fun bridgedToC(expression: String): String = cType.cast(expression)

    override fun cToBridged(expression: String): String = cBridgeType.cast(expression)
}

private class StructValuePassing(private val kotlinClass: IrClass, override val cType: CType) : ValuePassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cBridgeValue = passThroughBridge(
                cValuesRefToPointer(expression),
                symbols.interopCPointer.typeWithStarProjections,
                CTypes.pointer(cType)
        ).name

        return CExpression("*$cBridgeValue", cType)
    }

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression = with(irBuilder) {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(context.irBuiltIns.unitType, CTypes.void)

        val kotlinPointed = scope.createTemporaryVariable(irCall(symbols.interopAllocType.owner).apply {
            extensionReceiver = bridgeCallBuilder.getMemScope()
            putValueArgument(0, getTypeObject())
        })

        bridgeCallBuilder.prepare += kotlinPointed

        val cPointer = passThroughBridge(irGet(kotlinPointed), kotlinPointedType, CTypes.pointer(cType))
        cBridgeBodyLines += "*${cPointer.name} = $expression;"

        buildKotlinBridgeCall {
            irBlock {
                at(it)
                +it
                +readCValue(irGet(kotlinPointed), symbols)
            }
        }
    }

    override fun CCallbackBuilder.receiveValue(): IrExpression = with(bridgeBuilder.kotlinIrBuilder) {
        val cParameter = cFunctionBuilder.addParameter(cType)
        val kotlinPointed = passThroughBridge("&${cParameter.name}", CTypes.voidPtr, kotlinPointedType)

        readCValue(irGet(kotlinPointed), symbols)
    }

    private fun IrBuilderWithScope.readCValue(kotlinPointed: IrExpression, symbols: KonanSymbols): IrExpression =
        irCall(symbols.interopCValueRead.owner).apply {
            extensionReceiver = kotlinPointed
            putValueArgument(0, getTypeObject())
        }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) = with(bridgeBuilder.kotlinIrBuilder) {
        bridgeBuilder.setReturnType(irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(cType)

        val result = "callbackResult"
        val cReturnValue = CVariable(cType, result)
        cBodyLines += "$cReturnValue;"
        val kotlinPtr = passThroughBridge("&$result", CTypes.voidPtr, symbols.nativePtrType)

        kotlinBridgeStatements += irCall(symbols.interopCValueWrite.owner).apply {
            extensionReceiver = expression
            putValueArgument(0, irGet(kotlinPtr))
        }
        val cBridgeCall = buildCBridgeCall()
        cBodyLines += "$cBridgeCall;"
        cBodyLines += "return $result;"
    }

    private val kotlinPointedType: IrType get() = kotlinClass.defaultType

    private fun IrBuilderWithScope.getTypeObject() =
            irGetObject(
                    kotlinClass.declarations.filterIsInstance<IrClass>()
                            .single { it.isCompanion }.symbol
            )

}

private class CEnumValuePassing(
        val enumClass: IrClass,
        val value: IrProperty,
        val baseValuePassing: SimpleValuePassing
) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = baseValuePassing.kotlinBridgeType
    override val cBridgeType: CType
        get() = baseValuePassing.cBridgeType
    override val cType: CType
        get() = baseValuePassing.cType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression {
        val value = irCall(value.getter!!).apply {
            dispatchReceiver = expression
        }

        return with(baseValuePassing) { kotlinToBridged(value) }
    }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression {
        val companionClass = enumClass.declarations.filterIsInstance<IrClass>().single { it.isCompanion }
        val byValue = companionClass.simpleFunctions().single { it.name.asString() == "byValue" }

        return irCall(byValue).apply {
            dispatchReceiver = irGetObject(companionClass.symbol)
            putValueArgument(0, expression)
        }
    }

    override fun bridgedToC(expression: String): String = with(baseValuePassing) { bridgedToC(expression) }
    override fun cToBridged(expression: String): String = with(baseValuePassing) { cToBridged(expression) }
}

private class ObjCReferenceValuePassing(
        private val symbols: KonanSymbols,
        private val type: IrType,
        private val retained: Boolean
) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = symbols.nativePtrType
    override val cBridgeType: CType
        get() = CTypes.voidPtr
    override val cType: CType
        get() = CTypes.voidPtr

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression {
        val ptr = irCall(symbols.interopObjCObjectRawValueGetter.owner).apply {
            extensionReceiver = expression
        }
        return if (retained) {
            irCall(symbols.interopObjCRetain).apply {
                putValueArgument(0, ptr)
            }
        } else {
            ptr
        }
    }

    override fun IrBuilderWithScope.kotlinCallbackResultToBridged(expression: IrExpression): IrExpression {
        if (retained) return kotlinToBridged(expression) // Optimization.
        // Kotlin code may loose the ownership on this pointer after returning from the bridge,
        // so retain the pointer and autorelease it:
        return irCall(symbols.interopObjcRetainAutoreleaseReturnValue.owner).apply {
            putValueArgument(0, kotlinToBridged(expression))
        }
        // TODO: optimize by using specialized Kotlin-to-ObjC converter.
    }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            convertPossiblyRetainedObjCPointer(symbols, retained, expression) {
                irCall(symbols.interopInterpretObjCPointerOrNull, listOf(type)).apply {
                    putValueArgument(0, it)
                }
            }

    override fun bridgedToC(expression: String): String = expression
    override fun cToBridged(expression: String): String = expression

}

private fun IrBuilderWithScope.convertPossiblyRetainedObjCPointer(
        symbols: KonanSymbols,
        retained: Boolean,
        pointer: IrExpression,
        convert: (IrExpression) -> IrExpression
): IrExpression = if (retained) {
    irBlock(startOffset, endOffset) {
        val ptrVar = irTemporary(pointer)
        val resultVar = irTemporary(convert(irGet(ptrVar)))
        +irCall(symbols.interopObjCRelease.owner).apply {
            putValueArgument(0, irGet(ptrVar))
        }
        +irGet(resultVar)
    }
} else {
    convert(pointer)
}

private class ObjCBlockPointerValuePassing(
        val stubs: KotlinStubs,
        private val location: IrElement,
        private val functionType: IrSimpleType,
        private val valueReturning: ValueReturning,
        private val parameterValuePassings: List<ValuePassing>,
        private val retained: Boolean
) : SimpleValuePassing() {
    val symbols get() = stubs.symbols

    override val kotlinBridgeType: IrType
        get() = symbols.nativePtrType
    override val cBridgeType: CType
        get() = CTypes.id
    override val cType: CType
        get() = CTypes.id

    /**
     * Callback can receive stack-allocated block. Using block type for parameter and passing it as `id` to the bridge
     * makes Objective-C compiler generate proper copying to heap.
     */
    override val callbackParameterCType: CType
        get() = CTypes.blockPointer(
                CTypes.function(
                        valueReturning.cType,
                        parameterValuePassings.map { it.cType },
                        variadic = false
                ))

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression =
            irCall(symbols.interopCreateKotlinObjectHolder.owner).apply {
                putValueArgument(0, expression)
            }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            irLetS(expression) { blockPointerVarSymbol ->
                val blockPointerVar = blockPointerVarSymbol.owner
                irIfThenElse(
                        functionType.makeNullable(),
                        condition = irCall(symbols.areEqualByValue.getValue(PrimitiveBinaryType.POINTER).owner).apply {
                            putValueArgument(0, irGet(blockPointerVar))
                            putValueArgument(1, irNullNativePtr(symbols))
                        },
                        thenPart = irNull(),
                        elsePart = convertPossiblyRetainedObjCPointer(symbols, retained, irGet(blockPointerVar)) {
                            createKotlinFunctionObject(it)
                        }
                )
            }

    private object OBJC_BLOCK_FUNCTION_IMPL : IrDeclarationOriginImpl("OBJC_BLOCK_FUNCTION_IMPL")

    private fun IrBuilderWithScope.createKotlinFunctionObject(blockPointer: IrExpression): IrExpression {
        val constructor = generateKotlinFunctionClass()
        return irCall(constructor).apply {
            putValueArgument(0, blockPointer)
        }
    }

    private fun IrBuilderWithScope.generateKotlinFunctionClass(): IrConstructor {
        val symbols = stubs.symbols

        val irClass = IrClassImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL, IrClassSymbolImpl(),
                Name.identifier(stubs.getUniqueKotlinFunctionReferenceClassName("BlockFunctionImpl")),
                ClassKind.CLASS, DescriptorVisibilities.PRIVATE, Modality.FINAL,
                isCompanion = false, isInner = false, isData = false, isExternal = false,
                isInline = false, isExpect = false, isFun = false
        )
        irClass.createParameterDeclarations()

        irClass.superTypes += stubs.irBuiltIns.anyType
        irClass.superTypes += functionType.makeNotNull()

        val blockHolderField = createField(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                stubs.irBuiltIns.anyType,
                Name.identifier("blockHolder"),
                isMutable = false, owner = irClass
        )

        val constructor = IrConstructorImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                IrConstructorSymbolImpl(),
                Name.special("<init>"),
                DescriptorVisibilities.PUBLIC,
                irClass.defaultType,
                isInline = false, isExternal = false, isPrimary = true, isExpect = false
        )
        irClass.addChild(constructor)

        val constructorParameter = IrValueParameterImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                IrValueParameterSymbolImpl(),
                Name.identifier("blockPointer"),
                0,
                symbols.nativePtrType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false,
                isAssignable = false
        )
        constructor.valueParameters += constructorParameter
        constructorParameter.parent = constructor

        constructor.body = irBuilder(stubs.irBuiltIns, constructor.symbol).irBlockBody(startOffset, endOffset) {
            +irDelegatingConstructorCall(symbols.any.owner.constructors.single())
            +irSetField(irGet(irClass.thisReceiver!!), blockHolderField,
                    irCall(symbols.interopCreateObjCObjectHolder.owner).apply {
                        putValueArgument(0, irGet(constructorParameter))
                    })
        }

        val parameterCount = parameterValuePassings.size
        require(functionType.arguments.size == parameterCount + 1) { stubs.renderCompilerError(location) }

        val overriddenInvokeMethod = (functionType.classifier.owner as IrClass).simpleFunctions()
                .single { it.name == OperatorNameConventions.INVOKE }

        val invokeMethod = IrFunctionImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                IrSimpleFunctionSymbolImpl(),
                overriddenInvokeMethod.name,
                DescriptorVisibilities.PUBLIC, Modality.FINAL,
                returnType = functionType.arguments.last().typeOrNull!!,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isExpect = false,
                isFakeOverride = false, isOperator = false, isInfix = false
        )
        invokeMethod.overriddenSymbols += overriddenInvokeMethod.symbol
        irClass.addChild(invokeMethod)
        invokeMethod.createDispatchReceiverParameter()

        invokeMethod.valueParameters += (0 until parameterCount).map { index ->
            val parameter = IrValueParameterImpl(
                    startOffset, endOffset,
                    OBJC_BLOCK_FUNCTION_IMPL,
                    IrValueParameterSymbolImpl(),
                    Name.identifier("p$index"),
                    index,
                    functionType.arguments[index].typeOrNull!!,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
                    isAssignable = false
            )
            parameter.parent = invokeMethod
            parameter
        }

        invokeMethod.body = irBuilder(stubs.irBuiltIns, invokeMethod.symbol).irBlockBody(startOffset, endOffset) {
            val blockPointer = irCall(symbols.interopObjCObjectRawValueGetter.owner).apply {
                extensionReceiver = irGetField(irGet(invokeMethod.dispatchReceiverParameter!!), blockHolderField)
            }

            val arguments = (0 until parameterCount).map { index ->
                irGet(invokeMethod.valueParameters[index])
            }

            +irReturn(callBlock(blockPointer, arguments))
        }

        irClass.addFakeOverrides(stubs.irBuiltIns)

        stubs.addKotlin(irClass)
        return constructor
    }

    private fun IrBuilderWithScope.callBlock(blockPtr: IrExpression, arguments: List<IrExpression>): IrExpression {
        val callBuilder = KotlinToCCallBuilder(this, stubs, isObjCMethod = false, ForeignExceptionMode.default)

        val rawBlockPointerParameter =  callBuilder.passThroughBridge(blockPtr, blockPtr.type, CTypes.id)
        val blockVariableName = "block"

        arguments.forEachIndexed { index, argument ->
            callBuilder.addArgument(argument, parameterValuePassings[index], variadic = false)
        }

        val result = callBuilder.buildCall(blockVariableName, valueReturning)

        val blockVariableType = CTypes.blockPointer(callBuilder.cFunctionBuilder.getType())
        val blockVariable = CVariable(blockVariableType, blockVariableName)
        callBuilder.cBridgeBodyLines.add(0, "$blockVariable = ${rawBlockPointerParameter.name};")

        callBuilder.emitCBridge()

        return result
    }

    override fun bridgedToC(expression: String): String {
        val callbackBuilder = CCallbackBuilder(stubs, location, isObjCMethod = false)
        val kotlinFunctionHolder = "kotlinFunctionHolder"

        callbackBuilder.cBridgeCallBuilder.arguments += kotlinFunctionHolder
        val (kotlinFunctionHolderParameter, _) =
                callbackBuilder.bridgeBuilder.addParameter(symbols.nativePtrType, CTypes.id)

        callbackBuilder.kotlinCallBuilder.arguments += with(callbackBuilder.bridgeBuilder.kotlinIrBuilder) {
            // TODO: consider casting to [functionType].
            irCall(symbols.interopUnwrapKotlinObjectHolderImpl.owner).apply {
                putValueArgument(0, irGet(kotlinFunctionHolderParameter) )
            }
        }

        parameterValuePassings.forEach {
            callbackBuilder.kotlinCallBuilder.arguments += with(it) {
                callbackBuilder.receiveValue()
            }
        }

        require(functionType.isFunction()) { stubs.renderCompilerError(location) }
        val invokeFunction = (functionType.classifier.owner as IrClass)
                .simpleFunctions().single { it.name == OperatorNameConventions.INVOKE }

        callbackBuilder.buildValueReturn(invokeFunction, valueReturning)

        val block = buildString {
            append('^')
            append(callbackBuilder.cFunctionBuilder.buildSignature(""))
            append(" { ")
            callbackBuilder.cBodyLines.forEach {
                append(it)
                append(' ')
            }
            append(" }")
        }
        val blockAsId = if (retained) {
            "(__bridge id)(__bridge_retained void*)$block" // Retain and convert to id.
        } else {
            "(id)$block"
        }

        return "({ id $kotlinFunctionHolder = $expression; $kotlinFunctionHolder ? $blockAsId : (id)0; })"
    }

    override fun cToBridged(expression: String) = expression

}

private class WCStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val wcstr = irBuilder.irSafeTransform(expression) {
            irCall(symbols.interopWcstr.owner).apply {
                extensionReceiver = it
            }
        }
        return with(CValuesRefArgumentPassing) { passValue(wcstr) }
    }

}

private class CStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cstr = irBuilder.irSafeTransform(expression) {
            irCall(symbols.interopCstr.owner).apply {
                extensionReceiver = it
            }
        }
        return with(CValuesRefArgumentPassing) { passValue(cstr) }
    }

}

private object CValuesRefArgumentPassing : KotlinToCArgumentPassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val bridgeArgument = cValuesRefToPointer(expression)
        val cBridgeValue = passThroughBridge(
                bridgeArgument,
                symbols.interopCPointer.typeWithStarProjections.makeNullable(),
                CTypes.voidPtr
        )
        return CExpression(cBridgeValue.name, cBridgeValue.type)
    }
}

private fun KotlinToCCallBuilder.cValuesRefToPointer(
        value: IrExpression
): IrExpression = if (value.type.classifierOrNull == symbols.interopCPointer) {
    value // Optimization
} else {
    val getPointerFunction = symbols.interopCValuesRef.owner
            .simpleFunctions()
            .single { it.name.asString() == "getPointer" }

    irBuilder.irSafeTransform(value) {
        irCall(getPointerFunction).apply {
            dispatchReceiver = it
            putValueArgument(0, bridgeCallBuilder.getMemScope())
        }
    }
}

private fun IrBuilderWithScope.irSafeTransform(
        value: IrExpression,
        block: IrBuilderWithScope.(IrExpression) -> IrExpression
): IrExpression = if (!value.type.isNullable()) {
    block(value) // Optimization
} else {
    irLetS(value) { valueVarSymbol ->
        val valueVar = valueVarSymbol.owner
        val transformed = block(irGet(valueVar))
        irIfThenElse(
                type = transformed.type.makeNullable(),
                condition = irEqeqeq(irGet(valueVar), irNull()),
                thenPart = irNull(),
                elsePart = transformed
        )
    }
}

private object VoidReturning : ValueReturning {
    override val cType: CType
        get() = CTypes.void

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression {
        bridgeBuilder.setReturnType(irBuilder.context.irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(CTypes.void)
        cBridgeBodyLines += "$expression;"
        return buildKotlinBridgeCall()
    }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) {
        bridgeBuilder.setReturnType(irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(CTypes.void)
        kotlinBridgeStatements += bridgeBuilder.kotlinIrBuilder.irReturn(expression)
        cBodyLines += "${buildCBridgeCall()};"
    }
}

private object IgnoredUnitArgumentPassing : ArgumentPassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression? {
        // Note: it is not correct to just drop the expression (due to possible side effects),
        // so (in lack of other options) evaluate the expression and pass ignored value to the bridge:
        val bridgeArgument = irBuilder.irBlock {
            +expression
            +irInt(0)
        }
        passThroughBridge(bridgeArgument, irBuilder.context.irBuiltIns.intType, CTypes.int).name
        return null
    }

    override fun CCallbackBuilder.receiveValue(): IrExpression {
        return bridgeBuilder.kotlinIrBuilder.irGetObject(irBuiltIns.unitClass)
    }
}

internal fun CType.cast(expression: String): String = "((${this.render("")})$expression)"
