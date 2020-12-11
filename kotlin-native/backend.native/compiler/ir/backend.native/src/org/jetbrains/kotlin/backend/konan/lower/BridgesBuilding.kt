/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures

internal class WorkersBridgesBuilding(val context: Context) : DeclarationContainerLoweringPass, IrElementTransformerVoid() {

    val symbols = context.ir.symbols
    lateinit var runtimeJobFunction: IrSimpleFunction

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat {
            listOf(it) + buildWorkerBridges(it).also { bridges ->
                // `buildWorkerBridges` builds bridges for all declarations inside `it` and nested declarations,
                // so some bridges get incorrect parent. Fix it:
                bridges.forEach { bridge -> bridge.parent = irDeclarationContainer }
            }
        }
    }

    private fun buildWorkerBridges(declaration: IrDeclaration): List<IrFunction> {
        val bridges = mutableListOf<IrFunction>()
        declaration.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                // Skip nested.
                return declaration
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol != symbols.executeImpl)
                    return expression

                val job = expression.getValueArgument(3) as IrFunctionReference
                val jobFunction = (job.symbol as IrSimpleFunctionSymbol).owner

                if (!::runtimeJobFunction.isInitialized) {
                    val arg = jobFunction.valueParameters[0]
                    val startOffset = jobFunction.startOffset
                    val endOffset = jobFunction.endOffset
                    runtimeJobFunction =
                        IrFunctionImpl(
                                startOffset, endOffset,
                                IrDeclarationOrigin.DEFINED,
                                IrSimpleFunctionSymbolImpl(),
                                jobFunction.name,
                                jobFunction.visibility,
                                jobFunction.modality,
                                isInline = false,
                                isExternal = false,
                                isTailrec = false,
                                isSuspend = false,
                                returnType = context.irBuiltIns.anyNType,
                                isExpect = false,
                                isFakeOverride = false,
                                isOperator = false,
                                isInfix = false
                    )

                    runtimeJobFunction.valueParameters +=
                        IrValueParameterImpl(
                                startOffset, endOffset,
                                IrDeclarationOrigin.DEFINED,
                                IrValueParameterSymbolImpl(),
                                arg.name,
                                arg.index,
                                type = context.irBuiltIns.anyNType,
                                varargElementType = null,
                                isCrossinline = arg.isCrossinline,
                                isNoinline = arg.isNoinline,
                                isHidden = arg.isHidden,
                                isAssignable = arg.isAssignable
                        )
                }
                val overriddenJobDescriptor = OverriddenFunctionInfo(jobFunction, runtimeJobFunction)
                if (!overriddenJobDescriptor.needBridge) return expression

                val bridge = context.buildBridge(
                        startOffset  = job.startOffset,
                        endOffset    = job.endOffset,
                        overriddenFunction = overriddenJobDescriptor,
                        targetSymbol = jobFunction.symbol)
                bridges += bridge
                expression.putValueArgument(3, IrFunctionReferenceImpl.fromSymbolDescriptor(
                        startOffset   = job.startOffset,
                        endOffset     = job.endOffset,
                        type          = job.type,
                        symbol        = bridge.symbol,
                        typeArgumentsCount = 0,
                        reflectionTarget = null)
                )
                return expression
            }
        })
        return bridges
    }
}

internal class BridgesBuilding(val context: Context) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val builtBridges = mutableSetOf<IrSimpleFunction>()

        for (function in irClass.simpleFunctions()) {
            val set = mutableSetOf<BridgeDirections>()
            for (overriddenFunction in function.allOverriddenFunctions) {
                val overriddenFunctionInfo = OverriddenFunctionInfo(function, overriddenFunction)
                val bridgeDirections = overriddenFunctionInfo.bridgeDirections
                if (!bridgeDirections.allNotNeeded() && overriddenFunctionInfo.canBeCalledVirtually
                        && !overriddenFunctionInfo.inheritsBridge && set.add(bridgeDirections)) {
                    buildBridge(overriddenFunctionInfo, irClass)
                    builtBridges += function
                }
            }
        }
        irClass.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                // Skip nested.
                return declaration
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                declaration.transformChildrenVoid(this)

                val body = declaration.body ?: return declaration

                val descriptor = declaration.descriptor
                val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor)
                if (typeSafeBarrierDescription == null || builtBridges.contains(declaration))
                    return declaration

                val irBuilder = context.createIrBuilder(declaration.symbol, declaration.startOffset, declaration.endOffset)
                declaration.body = irBuilder.irBlockBody(declaration) {
                    buildTypeSafeBarrier(declaration, declaration, typeSafeBarrierDescription)
                    (body as IrBlockBody).statements.forEach { +it }
                }
                return declaration
            }
        })
    }

    private fun buildBridge(overriddenFunction: OverriddenFunctionInfo, irClass: IrClass) {
        irClass.declarations.add(context.buildBridge(
                startOffset          = irClass.startOffset,
                endOffset            = irClass.endOffset,
                overriddenFunction   = overriddenFunction,
                targetSymbol         = overriddenFunction.function.symbol,
                superQualifierSymbol = irClass.symbol)
        )
    }
}

internal class DECLARATION_ORIGIN_BRIDGE_METHOD(val bridgeTarget: IrFunction) : IrDeclarationOrigin {
    override fun toString(): String {
        return "BRIDGE_METHOD(target=${bridgeTarget.descriptor})"
    }
}

internal val IrFunction.bridgeTarget: IrFunction?
        get() = (origin as? DECLARATION_ORIGIN_BRIDGE_METHOD)?.bridgeTarget

private fun IrBuilderWithScope.returnIfBadType(value: IrExpression,
                                               type: IrType,
                                               returnValueOnFail: IrExpression)
        = irIfThen(irNotIs(value, type), irReturn(returnValueOnFail))

private fun IrBuilderWithScope.irConst(value: Any?) = when (value) {
    null       -> irNull()
    is Int     -> irInt(value)
    is Boolean -> if (value) irTrue() else irFalse()
    else       -> TODO()
}

private fun IrBlockBodyBuilder.buildTypeSafeBarrier(function: IrFunction,
                                                    originalFunction: IrFunction,
                                                    typeSafeBarrierDescription: SpecialGenericSignatures.TypeSafeBarrierDescription) {
    val valueParameters = function.valueParameters
    val originalValueParameters = originalFunction.valueParameters
    for (i in valueParameters.indices) {
        if (!typeSafeBarrierDescription.checkParameter(i))
            continue

        val type = originalValueParameters[i].type.erasureForTypeOperation()
        // Note: erasing to single type is not entirely correct if type parameter has multiple upper bounds.
        // In this case the compiler could generate multiple type checks, one for each upper bound.
        // But let's keep it simple here for now; JVM backend doesn't do this anyway.

        if (!type.isNullableAny()) {
            +returnIfBadType(irGet(valueParameters[i]), type,
                    if (typeSafeBarrierDescription == SpecialGenericSignatures.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT)
                        irGet(valueParameters[2])
                    else irConst(typeSafeBarrierDescription.defaultValue)
            )
        }
    }
}

private fun Context.buildBridge(startOffset: Int, endOffset: Int,
                                overriddenFunction: OverriddenFunctionInfo, targetSymbol: IrSimpleFunctionSymbol,
                                superQualifierSymbol: IrClassSymbol? = null): IrFunction {

    val bridge = specialDeclarationsFactory.getBridge(overriddenFunction)

    if (bridge.modality == Modality.ABSTRACT) {
        return bridge
    }

    val irBuilder = createIrBuilder(bridge.symbol, startOffset, endOffset)
    bridge.body = irBuilder.irBlockBody(bridge) {
        val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(overriddenFunction.overriddenFunction.descriptor)
        typeSafeBarrierDescription?.let { buildTypeSafeBarrier(bridge, overriddenFunction.function, it) }

        val delegatingCall = IrCallImpl.fromSymbolDescriptor(
                startOffset,
                endOffset,
                targetSymbol.owner.returnType,
                targetSymbol,
                typeArgumentsCount = targetSymbol.owner.typeParameters.size,
                valueArgumentsCount = targetSymbol.owner.valueParameters.size,
                superQualifierSymbol = superQualifierSymbol /* Call non-virtually */
        ).apply {
            bridge.dispatchReceiverParameter?.let {
                dispatchReceiver = irGet(it)
            }
            bridge.extensionReceiverParameter?.let {
                extensionReceiver = irGet(it)
            }
            bridge.valueParameters.forEachIndexed { index, parameter ->
                this.putValueArgument(index, irGet(parameter))
            }
        }

        +irReturn(delegatingCall)
    }
    return bridge
}