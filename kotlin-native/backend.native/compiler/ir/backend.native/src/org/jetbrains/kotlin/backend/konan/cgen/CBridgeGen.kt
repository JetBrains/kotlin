/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.objcinterop.getObjCMethodInfo
import org.jetbrains.kotlin.ir.objcinterop.isObjCMetaClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

internal interface KotlinStubs {
    val irBuiltIns: IrBuiltIns
    val typeSystem: IrTypeSystemContext
    val symbols: KonanSymbols
    val target: KonanTarget
    val language: String

    val isSwiftExportEnabled: Boolean

    fun addKotlin(declaration: IrDeclaration)
    fun getUniqueCName(prefix: String): String
    fun getUniqueKotlinFunctionReferenceClassName(prefix: String): String

    fun throwCompilerError(element: IrElement?, message: String): Nothing
    fun renderCompilerError(element: IrElement?, message: String = "Failed requirement."): String
}

internal class CBridgeGenState(val stubs: KotlinStubs) {
    private val cLines = mutableListOf<String>()

    fun addC(lines: List<String>) {
        cLines.addAll(lines)
    }

    fun getC(): List<String> = cLines
}

private class KotlinToCCallBuilder(
        val irBuilder: IrBuilderWithScope,
        val stubs: KotlinStubs,
        val isObjCMethod: Boolean,
        foreignExceptionMode: ForeignExceptionMode.Mode
) {

    val state = CBridgeGenState(stubs)

    val cBridgeName = stubs.getUniqueCName("knbridge")

    val symbols: KonanSymbols get() = stubs.symbols
    val irBuiltIns: IrBuiltIns get() = stubs.irBuiltIns

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
                bridgeBuilder.getKotlinBridge().also {
                    this.stubs.addKotlin(it)
                },
                transformCall
        )

internal fun KotlinStubs.generateCCall(
        expression: IrCall, builder: IrBuilderWithScope, isInvoke: Boolean,
        foreignExceptionMode: ForeignExceptionMode.Mode = ForeignExceptionMode.default,
        direct: Boolean
): IrExpression {
    val callBuilder = KotlinToCCallBuilder(builder, this, isObjCMethod = false, foreignExceptionMode)

    val callee = expression.symbol.owner

    // TODO: consider computing all arguments before converting.

    val targetPtrParameter: String?
    val targetFunctionName: String

    if (isInvoke) {
        require(expression.dispatchReceiver == null) { renderCompilerError(expression) }
        targetPtrParameter = callBuilder.passThroughBridge(
                expression.arguments[0]!!,
                symbols.interopCPointer.starProjectedType,
                CTypes.voidPtr
        ).name
        targetFunctionName = "targetPtr"

        for (index in 1..<expression.arguments.size) {
            callBuilder.addArgument(
                    expression.arguments[index]!!,
                    type = expression.typeArguments[index - 1]!!,
                    variadic = false,
                    parameter = null
            )
        }
    } else {
        require(expression.symbol.owner.parameters.all {
            it.kind != IrParameterKind.ExtensionReceiver && it.kind != IrParameterKind.Context
        }) {
            renderCompilerError(expression)
        }
        targetPtrParameter = null
        targetFunctionName = this.getUniqueCName("target")

        val receiverParameter = expression.symbol.owner.dispatchReceiverParameter
        val arguments: List<IrExpression?> = when {
            receiverParameter?.type?.classOrNull?.owner?.isCompanion == true -> expression.arguments.drop(1)
            else -> expression.arguments
        }
        callBuilder.addArguments(arguments, callee)
    }

    val returnValuePassing = if (isInvoke) {
        val returnType = expression.typeArguments.last()!!
        callBuilder.state.mapReturnType(returnType, expression, signature = null)
    } else {
        callBuilder.state.mapReturnType(callee.returnType, expression, signature = callee)
    }

    val result = callBuilder.buildCall(targetFunctionName, returnValuePassing)

    if (isInvoke) {
        val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)
        callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = ${targetPtrParameter!!};")
    } else if (!direct) {
        val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)
        val cCallSymbolName = callee.getAnnotationArgumentValue<String>(RuntimeNames.cCall, "id")!!
        callBuilder.state.addC(listOf("extern const $targetFunctionVariable __asm(\"$cCallSymbolName\");")) // Exported from cinterop stubs.
    } else {
        /*
        Basically, we need to make the target C function accessible here.

        We could declare the function with its original name without the `__asm` attribute.
        But then we could have ended up with the "same" function declared multiple times incompatibly across different
        generated stubs.
        See e.g. the `sameFunctionWithDifferentSignatures.kt` test.
        Declaring them in the C bridge bodies wouldn't help either -- Clang still prohibits incompatible redeclarations
        even when they are "local".

        So, instead we stick to the unique name for the declaration (`targetFunctionName`)
        and specify the actual symbol name with the `__asm` attribute.
        Having multiple function declarations with the same `__asm` is perfectly fine.

        This approach has a downside: it is incompatible with `-Xcompile-source` in cinterop.
        Explanation: imagine there is a function definition `foo` on an Apple platform, compiled to bitcode
        with `-Xcompile-source`.
        As any other C function on Apple platforms, it gets a global symbol name prefix, so
        `symbolName` is "_foo".

        Using `__asm("_foo")` makes Clang refer to this function as "\01_foo" in the resulting LLVM IR,
        where `\01` instructs LLVM that it doesn't need to add the global symbol name prefix
        when generating machine code.
        But the function is defined as "foo" in LLVM IR of the source code compiled with `-Xcompile-source`.

        All LLVM modules (including those definition and reference) are linked and then optimized
        using LLVM optimization passes.
        One of them, globaldce, won't understand that "foo" and "\01_foo" denote the same function
        and will just delete "foo", leading to an unresolved symbol error at the linkage stage.

        There seems to be no alternative way to specify the symbol name without making Clang add `\01`.

        To work around this problem, cinterop marks `-Xcompile-source` incompatible with direct CCall.
        */
        val symbolName = callee.getAnnotationArgumentValue<String>(RuntimeNames.cCallDirect, "name")!!
        val signature = callBuilder.cFunctionBuilder.buildSignature(targetFunctionName, language)

        val symbolNameLiteral = quoteAsCStringLiteral(symbolName)
        callBuilder.state.addC(listOf("$signature __asm($symbolNameLiteral);"))
    }

    val libraryName = if (isInvoke) "" else callee.getPackageFragment().konanLibrary.let {
        require(it?.isCInteropLibrary() == true) { "Expected a function from a cinterop library: ${callee.render()}" }
        it.uniqueName
    }

    callBuilder.finishBuilding(libraryName)

    return result
}

internal fun KotlinStubs.generateCGlobalDirectAccess(
        expression: IrCall, builder: IrBuilderWithScope,
        foreignExceptionMode: ForeignExceptionMode.Mode = ForeignExceptionMode.default
): IrExpression {
    val callBuilder = KotlinToCCallBuilder(builder, this, isObjCMethod = false, foreignExceptionMode)

    val callee = expression.symbol.owner
    check(callee.isAccessor) { callee.render() }

    // Indicates that this getter returns not a value of the global, but a pointer to the global.
    val isPointer = callee.hasAnnotation(RuntimeNames.cGlobalAccessPointer)

    // Describes how to pass the getter result or the setter argument between C and Kotlin.
    val valuePassing = if (isPointer) {
        check(callee.isGetter) { callee.render() }
        // The generated stub body is going to declare the global type as `char`,
        // so we need to return `char*` to Kotlin.
        val cType = CTypes.pointer(CTypes.char)
        TrivialValuePassing(callee.returnType, cType)
    } else {
        // Extract the Kotlin property type:
        val type = callee.correspondingPropertySymbol?.owner?.getter?.returnType ?: error(callee.render())

        callBuilder.state.mapType(
                type,
                retained = false,
                variadic = false,
                location = expression,
        )
    }

    // Now we declare the C global and generate the access.

    val globalCType = if (isPointer) {
        // We are going to use only the pointer to the global, so we can declare the global with any type,
        // not necessarily matching its original type as parsed in cinterop.
        // `char` is fine and simple:
        CTypes.char
    } else {
        valuePassing.cType
    }

    // Now we declare the actual global using a generated unique name and specifying the real binary name with the
    // `__asm` attribute.
    // The reasoning and consequences are basically the same as in `generateCCall(direct = true)`.
    val globalName = this.getUniqueCName("targetGlobal")
    val globalSymbolName = callee.getAnnotationArgumentValue<String>(RuntimeNames.cGlobalAccess, "name")!!
    val globalSymbolNameLiteral = quoteAsCStringLiteral(globalSymbolName)
    callBuilder.state.addC(listOf("extern ${globalCType.render(globalName)} __asm($globalSymbolNameLiteral);"))

    // And now generate the actual access and pass the value between C and Kotlin:
    val result: IrExpression = when {
        isPointer -> with(valuePassing) {
            check(expression.arguments.isEmpty()) { expression.dump() }
            callBuilder.returnValue("&$globalName")
        }
        callee.isGetter -> with(valuePassing) {
            check(expression.arguments.isEmpty()) { expression.dump() }
            callBuilder.returnValue(globalName)
            // A great hack is hidden here.
            // For Objective-C references, `valuePassing` is an `ObjCReferenceValuePassing`.
            // It uses `void*` as its `cType`, and not an `id`.
            // It means that the stub has a read of a `void*` global and the Obj-C compiler doesn't involve any ARC.
            // Luckily, we don't need it here: the Kotlin code calling the C stub is going to immediately do proper
            // retaining (see `ObjCReferenceValuePassing.bridgedToKotlin`).
            //
            // We could theoretically rework ObjCReferenceValuePassing instead and make it use `id`.
            // But that's undesirable, because:
            // 1. The change would affect other (non-experimental) usages of it, prompting additional verification.
            // 2. For all usages no ARC is actually needed, so we would need to rely on Clang optimizations to keep
            //    the compiled stubs ARC-free.
            //
            // TODO: KT-82011 a global with Objective-C reference type may have a non-strong ownership,
            //  which requires different handling here.
        }
        callee.isSetter -> {
            val kotlinValue = expression.arguments.singleOrNull() ?: error(expression.dump())
            val cValue = with(valuePassing) {
                callBuilder.passValue(kotlinValue) ?: error(expression.dump())
            }
            val assignment = if (valuePassing is ObjCReferenceValuePassing) {
                // Now things are getting tricky.
                // For handling Obj-C reference getters above, we didn't need any ARC,
                // so we could continue using `void*` as is.
                // But the setter writes the value into the global, which does require ARC:
                // we need to release the old value and retain the new value.
                //
                // Of all different ways to achieve that, this one seems the easiest:
                // cast the global pointer to `__strong id*`, cast the value to `id`, do the store.
                // That way we delegate all the necessary ARC to Clang without affecting other usages of
                // `ObjCReferenceValuePassing`.
                //
                // When casting the global pointer, we need to cast it to `void*` first,
                // since Clang prohibits casting `void**` to `id*`.
                check(globalCType == CTypes.voidPtr && cValue.type == CTypes.voidPtr) { callee.render() }
                " *(__strong id *)(void*)&$globalName = (__bridge id)(${cValue.expression});"
                // TODO: KT-82011 a global with Objective-C reference type may have a non-strong ownership,
                //  which requires different handling here.
            } else {
                "$globalName = ${cValue.expression};"
            }
            with(VoidReturning) {
                callBuilder.returnValue(assignment)
            }
        }
        else -> {
            error(callee.render())
        }
    }

    val libraryName = callee.getPackageFragment().konanLibrary.let {
        require(it?.isCInteropLibrary() == true) { "Expected a function from a cinterop library: ${callee.render()}" }
        it.uniqueName
    }

    callBuilder.finishBuilding(libraryName)

    return result
}

private fun KotlinToCCallBuilder.addArguments(arguments: List<IrExpression?>, callee: IrFunction) {
    val parameters = callee.parameters.filter { it.kind == IrParameterKind.Regular }
    arguments.forEachIndexed { index, argument ->
        val parameter = parameters[index]
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
                val argument = irCall(symbols.arrayGet[symbols.array]!!, irBuiltIns.anyClass.defaultType.makeNullable()).apply {
                    arguments[0] = irGet(variable)
                    arguments[1] = irInt(index)
                }.implicitCastIfNeededTo(type)
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
            handleArgumentForVarargParameter(expression.arguments[0]) { _, elements ->
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

    state.addC(cLines)
}

private fun KotlinToCCallBuilder.finishBuilding(libraryName: String): IrSimpleFunction {
    emitCBridge()

    val bridge = bridgeBuilder.getKotlinBridge()

    val allC = state.getC()

    bridge.annotations += buildSimpleAnnotation(
            stubs.irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.kotlinToCBridge.owner,
            stubs.language, allC.joinToString("\n") { it }, libraryName
    )

    return bridge
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
        directSymbolName: String?,
        call: IrFunctionAccessExpression,
        superQualifier: IrClassSymbol?,
        receiver: ObjCCallReceiver,
        arguments: List<IrExpression?>
) = builder.irBlock {
    val resolved = method.resolveFakeOverrideMaybeAbstract() ?: method
    val isDirect = directSymbolName != null

    val exceptionMode = ForeignExceptionMode.byValue(
            resolved.konanLibrary?.manifestProperties
                    ?.getProperty(ForeignExceptionMode.manifestKey)
    )

    val callBuilder = KotlinToCCallBuilder(builder, this@generateObjCCall, isObjCMethod = true, exceptionMode)

    val superClass = irTemporary(
            superQualifier?.let { getObjCClass(symbols, it) } ?: irNullNativePtr(symbols),
            isMutable = true
    )

    val targetPtrParameter = if (!isDirect) {
        val messenger = irCall(if (isStret) {
            symbols.interopGetMessengerStret
        } else {
            symbols.interopGetMessenger
        }.owner).apply {
            this.arguments[0] = irGet(superClass) // TODO: check superClass statically.
        }

        callBuilder.passThroughBridge(
                messenger,
                symbols.interopCPointer.starProjectedType,
                CTypes.voidPtr
        ).name
    } else {
        null
    }

    val preparedReceiver = if (method.objCConsumesReceiver()) {
        when (receiver) {
            is ObjCCallReceiver.Regular -> irCall(symbols.interopObjCRetain.owner).apply {
                this.arguments[0] = receiver.rawPtr
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
                        this.arguments[0] = irGet(rawPtrVar) // Balance retained pointer.
                    }
                }
                irGet(rawPtrVar)
            }
        }
    }

    val receiverOrSuper = if (superQualifier != null) {
        irCall(symbols.interopCreateObjCSuperStruct.owner).apply {
            this.arguments[0] = preparedReceiver
            this.arguments[1] = irGet(superClass)
        }
    } else {
        preparedReceiver
    }

    callBuilder.cCallBuilder.arguments += callBuilder.passThroughBridge(
            receiverOrSuper, symbols.nativePtrType, CTypes.voidPtr).name
    callBuilder.cFunctionBuilder.addParameter(CTypes.voidPtr)

    if (!isDirect) {
        callBuilder.cCallBuilder.arguments += "@selector($selector)"
        callBuilder.cFunctionBuilder.addParameter(CTypes.voidPtr)
    }

    callBuilder.addArguments(arguments, method)

    val returnValuePassing = callBuilder.state.mapReturnType(method.returnType, call, signature = method)

    val targetFunctionName = getUniqueCName("knbridge_targetPtr")

    val result = callBuilder.buildCall(targetFunctionName, returnValuePassing)

    if (isDirect) {
        // This declares a function
        val targetFunctionVariable = CVariable(callBuilder.cFunctionBuilder.getType(), targetFunctionName)
        callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable __asm(\"$directSymbolName\");")

    } else {
        val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)
        callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = $targetPtrParameter;")
    }

    // The library is saved to not lose an implicit edge in the dependencies graph (see DependenciesTracker.kt)
    val libraryName = if (method.parent is IrClass) {
        // Obj-C class/protocol. No need in saving the library, as to get an instance,
        // an explicit call must have been executed and no edge would be lost.
        ""
    } else { // Category-provided.
        method.getPackageFragment().konanLibrary.let {
            require(it?.isCInteropLibrary() == true) { "Expected a function from a cinterop library: ${method.render()}" }
            it.uniqueName
        }
    }

    callBuilder.finishBuilding(libraryName)

    +result
}

internal fun IrBuilderWithScope.getObjCClass(symbols: KonanSymbols, symbol: IrClassSymbol): IrExpression {
    require(!symbol.owner.isObjCMetaClass())
    return irCall(symbols.interopGetObjCClass, symbols.nativePtrType, listOf(symbol.starProjectedType))
}

private fun IrBuilderWithScope.irNullNativePtr(symbols: KonanSymbols) = irCall(symbols.getNativeNullPtr.owner)

private class CCallbackBuilder(
        val state: CBridgeGenState,
        val location: IrElement,
        val isObjCMethod: Boolean
) {

    val stubs: KotlinStubs get() = state.stubs
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

    val valuePassing = state.mapFunctionParameterType(
            it.type,
            retained = it.isObjCConsumed(),
            variadic = false,
            location = location
    )

    val kotlinArgument = with(valuePassing) { receiveValue() }
    kotlinCallBuilder.arguments += kotlinArgument
}

private fun CCallbackBuilder.build(function: IrSimpleFunction, signature: IrSimpleFunction): String {
    val valueReturning = state.mapReturnType(
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

    val kotlinBridge = bridgeBuilder.getKotlinBridge()
    kotlinBridge.body = bridgeBuilder.kotlinIrBuilder.irBlockBody {
        kotlinBridgeStatements.forEach { +it }
    }
    val cBridgeDeclaration = "${buildCBridge()};"
    kotlinBridge.annotations += listOf(
            buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.cToKotlinBridge.owner,
                    stubs.language, cBridgeDeclaration)
    )
    stubs.addKotlin(kotlinBridge)
}

private fun CCallbackBuilder.buildCFunction(): String {
    val result = stubs.getUniqueCName("kncfun")

    val cLines = mutableListOf<String>()

    cLines += "${cFunctionBuilder.buildSignature(result, stubs.language)} {"
    cLines += cBodyLines
    cLines += "}"

    state.addC(cLines)

    return result
}

private fun CBridgeGenState.generateCFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
) = with(stubs) {
    val callbackBuilder = CCallbackBuilder(this@generateCFunction, location, isObjCMethod)

    if (isObjCMethod) {
        val receiver = function.dispatchReceiverParameter!!
        require(signature.dispatchReceiverParameter!!.type.isObjCReferenceType(target, irBuiltIns)) { renderCompilerError(signature) }
        require(receiver.type.isObjCReferenceType(target, irBuiltIns)) { renderCompilerError(signature) }
        val valuePassing = ObjCReferenceValuePassing(symbols, receiver.type, retained = signature.objCConsumesReceiver())
        val kotlinArgument = with(valuePassing) { callbackBuilder.receiveValue() }
        callbackBuilder.kotlinCallBuilder.arguments += kotlinArgument

        // Selector is ignored:
        with(TrivialValuePassing(symbols.nativePtrType, CTypes.voidPtr)) { callbackBuilder.receiveValue() }
    } else {
        require(signature.dispatchReceiverParameter == null) { renderCompilerError(signature) }
    }

    signature.parameters.forEach {
        if (it.kind != IrParameterKind.DispatchReceiver) {
            callbackBuilder.addParameter(it, function.parameters[it.indexInParameters])
        }
    }

    callbackBuilder.build(function, signature)
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

    return IrRawFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            fakeFunction.symbol
    )
}

internal fun KotlinStubs.generateCFunctionAndFakeKotlinExternalFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
): IrSimpleFunction {
    val state = CBridgeGenState(this)
    val cFunction = state.generateCFunction(function, signature, isObjCMethod, location)
    return state.createFakeKotlinExternalFunction(signature, cFunction, isObjCMethod)
}

private fun CBridgeGenState.createFakeKotlinExternalFunction(
        signature: IrSimpleFunction,
        cFunctionName: String,
        isObjCMethod: Boolean
): IrSimpleFunction {
    val symbols = stubs.symbols
    val irBuiltIns = stubs.irBuiltIns

    val bridge = irBuiltIns.irFactory.createSimpleFunction(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            Name.identifier(cFunctionName),
            DescriptorVisibilities.PRIVATE,
            isInline = false,
            isExpect = false,
            signature.returnType,
            Modality.FINAL,
            IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = true,
    )

    if (isObjCMethod) {
        val methodInfo = signature.getObjCMethodInfo()!!
        bridge.annotations += buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                symbols.objCMethodImp.owner, methodInfo.selector, methodInfo.encoding)
    }

    val allC = getC()

    bridge.annotations += buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            symbols.kotlinToCBridge.owner, stubs.language, allC.joinToString("\n") { it }, "")

    return bridge
}

private fun getCStructType(kotlinClass: IrClass): CType? =
        kotlinClass.getCStructSpelling()?.let { CTypes.simple(it) }

private fun CBridgeGenState.getNamedCStructType(kotlinClass: IrClass): CType? {
    val cStructType = getCStructType(kotlinClass) ?: return null
    val name = stubs.getUniqueCName("struct")
    addC(listOf("typedef ${cStructType.render(name)};"))
    return CTypes.simple(name)
}

private fun KotlinToCCallBuilder.mapCalleeFunctionParameter(
        type: IrType,
        variadic: Boolean,
        parameter: IrValueParameter?,
        argument: IrExpression
): KotlinToCArgumentPassing {
    val classifier = type.classifierOrNull
    return when {
        classifier?.isClassWithFqName(InteropFqNames.cValues.toUnsafe()) == true || // Note: this should not be accepted, but is required for compatibility
                classifier?.isClassWithFqName(InteropFqNames.cValuesRef.toUnsafe()) == true -> CValuesRefArgumentPassing(type)

        classifier == symbols.string && (variadic || parameter?.isCStringParameter() == true) -> {
            require(!variadic || !isObjCMethod) { stubs.renderCompilerError(argument) }
            CStringArgumentPassing()
        }

        classifier == symbols.string && parameter?.isWCStringParameter() == true ->
            WCStringArgumentPassing()

        else -> state.mapFunctionParameterType(
                type,
                retained = parameter?.isObjCConsumed() ?: false,
                variadic = variadic,
                location = argument
        )
    }
}

private fun CBridgeGenState.mapFunctionParameterType(
        type: IrType,
        retained: Boolean,
        variadic: Boolean,
        location: IrElement
): ArgumentPassing = when {
    type.isUnit() && !variadic -> IgnoredUnitArgumentPassing
    else -> mapType(type, retained = retained, variadic = variadic, location = location)
}

private fun CBridgeGenState.mapReturnType(
        type: IrType,
        location: IrElement,
        signature: IrSimpleFunction?
): ValueReturning = when {
    type.isUnit() -> VoidReturning
    else -> mapType(type, retained = signature?.objCReturnsRetained() ?: false, variadic = false, location = location)
}

private fun CBridgeGenState.mapBlockType(
        type: IrType,
        retained: Boolean,
        location: IrElement
): ObjCBlockPointerValuePassing = with(stubs) {
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

    ObjCBlockPointerValuePassing(
            this@mapBlockType,
            location,
            type,
            valueReturning,
            parameterValuePassings,
            retained
    )
}

private fun CBridgeGenState.mapType(
        type: IrType,
        retained: Boolean,
        variadic: Boolean,
        location: IrElement
): ValuePassing = with(stubs) {
    when {
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
        (isSwiftExportEnabled && type == symbols.nativePtrType) -> TrivialValuePassing(type, CTypes.voidPtr)
        type.isTypeOfNullLiteral() && variadic -> TrivialValuePassing(symbols.interopCPointer.starProjectedType.makeNullable(), CTypes.voidPtr)
        type.isUByte() -> TrivialValuePassing(type, CTypes.unsignedChar)
        type.isUShort() -> TrivialValuePassing(type, CTypes.unsignedShort)
        type.isUInt() -> TrivialValuePassing(type, CTypes.unsignedInt)
        type.isULong() -> TrivialValuePassing(type, CTypes.unsignedLongLong)

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

        type.classOrNull?.isSubtypeOfClass(symbols.nativePointed) == true ->
            TrivialValuePassing(type, CTypes.voidPtr)

        type.isFunction() -> {
            require(!variadic) { renderCompilerError(location) }
            mapBlockType(type, retained = retained, location = location)
        }

        type.isObjCReferenceType(target, irBuiltIns) -> ObjCReferenceValuePassing(symbols, type, retained = retained)

        else -> stubs.throwCompilerError(location, "doesn't correspond to any C type: ${type.render()}")
    }
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
        arguments[0] = expression
        arguments[1] = IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 0)
    })

    override fun bridgedToC(expression: String): String = cType.cast(expression)

    override fun cToBridged(expression: String): String = cBridgeType.cast(expression)
}

private class StructValuePassing(private val kotlinClass: IrClass, override val cType: CType) : ValuePassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cBridgeValue = passThroughBridge(
                cValuesRefToPointer(expression, kotlinClass.defaultType),
                symbols.interopCPointer.starProjectedType,
                CTypes.pointer(cType)
        ).name

        return CExpression("*$cBridgeValue", cType)
    }

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression = with(irBuilder) {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(context.irBuiltIns.unitType, CTypes.void)

        val kotlinPointed = scope.createTemporaryVariable(irCall(symbols.interopAllocType.owner).apply {
            arguments[0] = bridgeCallBuilder.getMemScope()
            arguments[1] = getTypeObject()
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
        irCallWithSubstitutedType(symbols.interopCValueRead.owner, listOf(kotlinPointedType)).apply {
            arguments[0] = kotlinPointed
            arguments[1] = getTypeObject()
        }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) = with(bridgeBuilder.kotlinIrBuilder) {
        bridgeBuilder.setReturnType(irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(cType)

        val result = "callbackResult"
        val cReturnValue = CVariable(cType, result)
        cBodyLines += "$cReturnValue;"
        val kotlinPtr = passThroughBridge("&$result", CTypes.voidPtr, symbols.nativePtrType)

        kotlinBridgeStatements += irCallWithSubstitutedType(
                symbols.interopCValueWrite.owner, listOf(kotlinPointedType)
        ).apply {
            arguments[0] = expression
            arguments[1] = irGet(kotlinPtr)
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
            arguments[0] = expression
        }

        return with(baseValuePassing) { kotlinToBridged(value) }
    }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression {
        val companionClass = enumClass.declarations.filterIsInstance<IrClass>().single { it.isCompanion }
        val byValue = companionClass.simpleFunctions().single { it.name.asString() == "byValue" }

        return irCall(byValue).apply {
            arguments[0] = irGetObject(companionClass.symbol)
            arguments[1] = expression
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
            arguments[0] = expression
        }
        return if (retained) {
            irCall(symbols.interopObjCRetain).apply {
                arguments[0] = ptr
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
            arguments[0] = kotlinToBridged(expression)
        }
        // TODO: optimize by using specialized Kotlin-to-ObjC converter.
    }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            convertPossiblyRetainedObjCPointer(symbols, retained, expression) {
                irCallWithSubstitutedType(symbols.interopInterpretObjCPointerOrNull, listOf(type)).apply {
                    arguments[0] = it
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
            arguments[0] = irGet(ptrVar)
        }
        +irGet(resultVar)
    }
} else {
    convert(pointer)
}

internal fun CBridgeGenState.convertBlockPtrToKotlinFunction(builder: IrBuilderWithScope, blockPtr: IrExpression, functionType: IrType): IrExpression {
    // blockPtr can be stack-allocated, so copy it first.
    val copiedBlockPtr = builder.irCall(stubs.symbols.interopBlockCopy).apply {
        arguments[0] = blockPtr
    }
    val valuePassing = mapBlockType(
            type = functionType,
            retained = true,
            location = blockPtr
    )

    /*
    Note: the code below checks that `valuePassing.cToBridged` simply returns its argument; this is a hack.
    By design, using [valuePassing] to convert from C to Kotlin consists of two parts:
    * `cToBridged` in the Obj-C code,
    * `bridgedToKotlin` in the Kotlin code.

    Generally, we could do it that way.
    There are a couple of problems, though:
    * That would require us to generate and call a full-blown C function, which is suboptimal.
      Consider in particular thread state switches usually required to call a C function from Kotlin.
      Improving it would require more changes.
      Considering that the whole intrinsic is a temporary solution, this seems unnecessary.
    * A straightforward black-box implementation would involve an autorelease operation.
      To handle the `cToBridged` part properly, it would generate a function like
          id foo(id p) { return p; }
      which has to autorelease `p` when returning. Which is suboptimal.
      Fixing this would anyway require breaking encapsulation of the [valuePassing],
      since `id` return type here is a `cBridgeType`, right in the middle of the two-step conversion.
      In other words, getting rid of the redundant autorelease would basically require us to rely
      on the `cToBridged` implementation as well.

    So, this hack is an optimization as well as a simplification at the same time.
    Win-win.
    */
    val cBlockPtr = "blockPtr"
    val cBridged = valuePassing.cToBridged(cBlockPtr)
    check(cBridged == cBlockPtr) { "Unexpected cToBridged implementation: expected $cBlockPtr, got $cBridged" }

    with(valuePassing) {
        return builder.bridgedToKotlin(copiedBlockPtr, symbols)
    }
}

private class ObjCBlockPointerValuePassing(
        val state: CBridgeGenState,
        private val location: IrElement,
        private val functionType: IrSimpleType,
        private val valueReturning: ValueReturning,
        private val parameterValuePassings: List<ValuePassing>,
        private val retained: Boolean
) : SimpleValuePassing() {
    val symbols get() = state.stubs.symbols
    val irBuiltIns get() = state.stubs.irBuiltIns

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
                arguments[0] = expression
            }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            irLetS(expression) { blockPointerVarSymbol ->
                val blockPointerVar = blockPointerVarSymbol.owner
                irIfThenElse(
                        functionType.makeNullable(),
                        condition = irCall(symbols.areEqualByValue.getValue(PrimitiveBinaryType.POINTER).owner).apply {
                            arguments[0] = irGet(blockPointerVar)
                            arguments[1] = irNullNativePtr(symbols)
                        },
                        thenPart = irNull(),
                        elsePart = convertPossiblyRetainedObjCPointer(symbols, retained, irGet(blockPointerVar)) {
                            createKotlinFunctionObject(it)
                        }
                )
            }

    private companion object {
        private val OBJC_BLOCK_FUNCTION_IMPL by IrDeclarationOriginImpl.Regular
    }

    private fun IrBuilderWithScope.createKotlinFunctionObject(blockPointer: IrExpression): IrExpression {
        val constructor = generateKotlinFunctionClass()
        return irCall(constructor).apply {
            arguments[0] = blockPointer
        }
    }

    private fun IrBuilderWithScope.generateKotlinFunctionClass(): IrConstructor {
        val irClass = context.irFactory.createClass(
                startOffset,
                endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                Name.identifier(state.stubs.getUniqueKotlinFunctionReferenceClassName("BlockFunctionImpl")),
                DescriptorVisibilities.PRIVATE,
                IrClassSymbolImpl(),
                ClassKind.CLASS,
                Modality.FINAL,
        )
        irClass.createThisReceiverParameter()

        irClass.superTypes += irBuiltIns.anyType
        irClass.superTypes += functionType.makeNotNull()

        val blockHolderField = context.irFactory.createField(
                startOffset,
                endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                Name.identifier("blockHolder"),
                DescriptorVisibilities.PRIVATE,
                IrFieldSymbolImpl(),
                irBuiltIns.anyType,
                isFinal = true,
                isStatic = false,
        )
        irClass.addChild(blockHolderField)

        val constructor = context.irFactory.createConstructor(
                startOffset,
                endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                Name.special("<init>"),
                DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                irClass.defaultType,
                IrConstructorSymbolImpl(),
                isPrimary = true,
        )
        irClass.addChild(constructor)

        val constructorParameter = context.irFactory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = OBJC_BLOCK_FUNCTION_IMPL,
                kind = IrParameterKind.Regular,
                name = Name.identifier("blockPointer"),
                type = symbols.nativePtrType,
                isAssignable = false,
                symbol = IrValueParameterSymbolImpl(),
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false,
        )
        constructor.parameters = listOf(constructorParameter)
        constructorParameter.parent = constructor

        constructor.body = irBuiltIns.createIrBuilder(constructor.symbol).irBlockBody(startOffset, endOffset) {
            +irDelegatingConstructorCall(irBuiltIns.anyClass.owner.constructors.single())
            +irSetField(irGet(irClass.thisReceiver!!), blockHolderField,
                    irCall(symbols.interopCreateObjCObjectHolder.owner).apply {
                        arguments[0] = irGet(constructorParameter)
                    })
        }

        val parameterCount = parameterValuePassings.size
        require(functionType.arguments.size == parameterCount + 1) { state.stubs.renderCompilerError(location) }

        val overriddenInvokeMethod = (functionType.classifier.owner as IrClass).simpleFunctions()
                .single { it.name == OperatorNameConventions.INVOKE }

        val invokeMethod = context.irFactory.createSimpleFunction(
                startOffset,
                endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                overriddenInvokeMethod.name,
                DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                returnType = functionType.arguments.last().typeOrNull!!,
                Modality.FINAL,
                IrSimpleFunctionSymbolImpl(),
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
                isInfix = false,
        )
        invokeMethod.overriddenSymbols += overriddenInvokeMethod.symbol
        irClass.addChild(invokeMethod)
        invokeMethod.parameters += invokeMethod.createDispatchReceiverParameterWithClassParent()

        invokeMethod.parameters += (0 until parameterCount).map { index ->
            val parameter = context.irFactory.createValueParameter(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = OBJC_BLOCK_FUNCTION_IMPL,
                    kind = IrParameterKind.Regular,
                    name = Name.identifier("p$index"),
                    type = functionType.arguments[index].typeOrNull!!,
                    isAssignable = false,
                    symbol = IrValueParameterSymbolImpl(),
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
            )
            parameter.parent = invokeMethod
            parameter
        }

        invokeMethod.body = irBuiltIns.createIrBuilder(invokeMethod.symbol).irBlockBody(startOffset, endOffset) {
            val blockPointer = irCall(symbols.interopObjCObjectRawValueGetter.owner).apply {
                arguments[0] = irGetField(irGet(invokeMethod.parameters[0]), blockHolderField)
            }

            val arguments = (0 until parameterCount).map { index ->
                irGet(invokeMethod.parameters[index + 1])
            }

            +irReturn(callBlock(blockPointer, arguments))
        }

        state.stubs.addKotlin(irClass)
        // we need to add class to stubs first, because it will implicitly initialize class parent.
        irClass.addFakeOverrides(state.stubs.typeSystem)

        return constructor
    }

    private fun IrBuilderWithScope.callBlock(blockPtr: IrExpression, arguments: List<IrExpression>): IrExpression {
        val callBuilder = KotlinToCCallBuilder(this, state.stubs, isObjCMethod = false, ForeignExceptionMode.default)

        val rawBlockPointerParameter =  callBuilder.passThroughBridge(blockPtr, blockPtr.type, CTypes.id)
        val blockVariableName = "block"

        arguments.forEachIndexed { index, argument ->
            callBuilder.addArgument(argument, parameterValuePassings[index], variadic = false)
        }

        val result = callBuilder.buildCall(blockVariableName, valueReturning)

        val blockVariableType = CTypes.blockPointer(callBuilder.cFunctionBuilder.getType())
        val blockVariable = CVariable(blockVariableType, blockVariableName)
        callBuilder.cBridgeBodyLines.add(0, "$blockVariable = ${rawBlockPointerParameter.name};")

        callBuilder.finishBuilding("")

        return result
    }

    override fun bridgedToC(expression: String): String {
        val callbackBuilder = CCallbackBuilder(state, location, isObjCMethod = false)
        val kotlinFunctionHolder = "kotlinFunctionHolder"

        callbackBuilder.cBridgeCallBuilder.arguments += kotlinFunctionHolder
        val (kotlinFunctionHolderParameter, _) =
                callbackBuilder.bridgeBuilder.addParameter(symbols.nativePtrType, CTypes.id)

        callbackBuilder.kotlinCallBuilder.arguments += with(callbackBuilder.bridgeBuilder.kotlinIrBuilder) {
            irCall(symbols.interopUnwrapKotlinObjectHolderImpl.owner).apply {
                arguments[0] = irGet(kotlinFunctionHolderParameter)
            }.implicitCastTo(functionType)
        }

        parameterValuePassings.forEach {
            callbackBuilder.kotlinCallBuilder.arguments += with(it) {
                callbackBuilder.receiveValue()
            }
        }

        require(functionType.isFunction()) { state.stubs.renderCompilerError(location) }
        val invokeFunction = (functionType.classifier.owner as IrClass)
                .simpleFunctions().single { it.name == OperatorNameConventions.INVOKE }

        callbackBuilder.buildValueReturn(invokeFunction, valueReturning)

        val block = buildString {
            append('^')
            append(callbackBuilder.cFunctionBuilder.buildSignature("", state.stubs.language))
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

    /**
     * Note: [convertBlockPtrToKotlinFunction] relies on the fact that the implementation simply returns the argument.
     * See the detailed comment inside that function.
     */
    override fun cToBridged(expression: String) = expression

}

private class WCStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val wcstr = irBuilder.irSafeTransform(expression) {
            irCall(symbols.interopWcstr.owner).apply {
                arguments[0] = it
            }
        }
        return with(CValuesRefArgumentPassing(wcstr.type)) { passValue(wcstr) }
    }

}

private class CStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cstr = irBuilder.irSafeTransform(expression) {
            irCall(symbols.interopCstr.owner).apply {
                arguments[0] = it
            }
        }
        return with(CValuesRefArgumentPassing(cstr.type)) { passValue(cstr) }
    }

}

private class CValuesRefArgumentPassing(type: IrType) : KotlinToCArgumentPassing {
    init {
        require(type.classOrNull?.owner?.let {
            it.hasEqualFqName(InteropFqNames.cValues) || it.hasEqualFqName(InteropFqNames.cValuesRef)
        } == true) { "Expected either ${InteropFqNames.cValues} or ${InteropFqNames.cValuesRef} but was: ${type.render()}" }
    }

    private val pointedType = (type as IrSimpleType).arguments.single()

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val bridgeArgument = cValuesRefToPointer(expression, pointedType)
        val cBridgeValue = passThroughBridge(
                bridgeArgument,
                symbols.interopCPointer.starProjectedType.makeNullable(),
                CTypes.voidPtr
        )
        return CExpression(cBridgeValue.name, cBridgeValue.type)
    }
}

private fun KotlinToCCallBuilder.cValuesRefToPointer(
        value: IrExpression,
        pointedType: IrTypeArgument,
): IrExpression = if (value.type.classifierOrNull == symbols.interopCPointer) {
    value // Optimization
} else {
    val getPointerFunction = symbols.interopCValuesRef.owner
            .simpleFunctions()
            .single { it.name.asString() == "getPointer" }

    irBuilder.irSafeTransform(value) {
        irCall(getPointerFunction.symbol, symbols.interopCPointer.typeWithArguments(listOf(pointedType))).apply {
            arguments[0] = it
            arguments[1] = bridgeCallBuilder.getMemScope()
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
        return bridgeBuilder.kotlinIrBuilder.irCall(symbols.theUnitInstance)
    }
}

internal fun CType.cast(expression: String): String = "((${this.render("")})$expression)"
