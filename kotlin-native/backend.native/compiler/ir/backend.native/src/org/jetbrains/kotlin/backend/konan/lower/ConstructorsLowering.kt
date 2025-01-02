/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createExtensionReceiver
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.ir.isArray
import org.jetbrains.kotlin.backend.konan.isInlined
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.ir.objcinterop.isObjCConstructor
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

internal var IrConstructor.loweredConstructorFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)
internal var IrSimpleFunction.originalConstructor: IrConstructor? by irAttribute(copyByDefault = false)

internal fun Context.getLoweredConstructorFunction(irConstructor: IrConstructor): IrSimpleFunction =
        irConstructor::loweredConstructorFunction.getOrSetIfNull {
            irFactory.buildFun {
                name = irConstructor.name
                startOffset = irConstructor.startOffset
                endOffset = irConstructor.endOffset
                visibility = irConstructor.visibility
                isExternal = irConstructor.isExternal
                returnType = irBuiltIns.unitType
            }.apply {
                val function = this
                val parentClass = irConstructor.parentAsClass
                parent = parentClass
                originalConstructor = irConstructor

                parameters += buildReceiverParameter {
                    type = parentClass.defaultType
                }

                require(irConstructor.parameters.none { it.kind == IrParameterKind.ExtensionReceiver }) { "A constructor with an extension receiver: ${irConstructor.render()}" }

                irConstructor.parameters.forEach {
                    if (it.kind == IrParameterKind.DispatchReceiver) {
                        parameters += createExtensionReceiver(it.type)
                    } else {
                        parameters += it.copyTo(function, type = it.type)
                    }
                }

                annotations = irConstructor.annotations
            }
        }

internal val LOWERED_DELEGATING_CONSTRUCTOR_CALL by IrStatementOriginImpl

/**
 * Replaces constructor calls by (alloc + static call).
 */
internal class ConstructorsLowering(private val context: Context) : FileLoweringPass, IrTransformer<IrDeclaration?>() {
    private val createUninitializedInstance = context.symbols.createUninitializedInstance
    private val createUninitializedArray = context.symbols.createUninitializedArray
    private val createEmptyString = context.symbols.createEmptyString
    private val initInstance = context.symbols.initInstance

    override fun lower(irFile: IrFile) {
        irFile.transform(this, data = null)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?): IrStatement {
        return super.visitDeclaration(declaration, declaration)
    }

    override fun visitClass(declaration: IrClass, data: IrDeclaration?): IrStatement {
        declaration.transformChildren(this, declaration)

        declaration.declarations.transformFlat {
            (it as? IrConstructor)?.let { constructor ->
                buildLoweredConstructorFunction(constructor)?.let { loweredConstructorFunction ->
                    listOf(constructor, loweredConstructorFunction)
                }
            }
        }

        return declaration
    }

    private fun buildLoweredConstructorFunction(constructor: IrConstructor): IrSimpleFunction? {
        require(!constructor.isObjCConstructor) { "No Obj-C constructor is expected here: ${constructor.render()}" }
        val body = constructor.body
        constructor.body = null
        val constructedClass = constructor.constructedClass
        // Inline classes constructors are intrinsified, but the constructors themselves are used
        // in [IrTypeInlineClassesSupport.getInlinedClassUnderlyingType], so they can't be removed.
        if (constructedClass.isInlined())
            return null

        val loweredConstructorFunction = context.getLoweredConstructorFunction(constructor)
        if (body != null) {
            loweredConstructorFunction.body = body as IrBlockBody
            body.setDeclarationsParent(loweredConstructorFunction)
            val irBuilder = context.createIrBuilder(loweredConstructorFunction.symbol)
            body.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitReturn(expression: IrReturn): IrExpression {
                    expression.transformChildrenVoid()

                    return if (expression.returnTargetSymbol == constructor.symbol)
                        irBuilder.at(expression).irReturn(expression.value)
                    else expression
                }

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid()

                    return when (val value = expression.symbol.owner) {
                        constructedClass.thisReceiver ->
                            irBuilder.at(expression).irGet(loweredConstructorFunction.dispatchReceiverParameter!!)
                        constructor.dispatchReceiverParameter -> {
                            require(constructedClass.isInner) { "Expected an inner class: ${constructedClass.render()}" }
                            irBuilder.at(expression).irGet(loweredConstructorFunction.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }!!)
                        }
                        is IrValueParameter -> {
                            irBuilder.at(expression).irGet(loweredConstructorFunction.parameters[value.indexInParameters + 1])
                        }
                        else -> expression
                    }
                }

                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                    expression.transformChildrenVoid()

                    return irBuilder.at(expression).run {
                        val callee = expression.symbol.owner
                        if (callee.constructedClass.isAny() || callee.constructedClass.isExternalObjCClass())
                            irComposite { }
                        else irCall(this@ConstructorsLowering.context.getLoweredConstructorFunction(callee),
                                origin = LOWERED_DELEGATING_CONSTRUCTOR_CALL
                        ).apply {
                            dispatchReceiver = irGet(loweredConstructorFunction.dispatchReceiverParameter!!)
                            fillArgumentsFrom(expression)
                        }
                    }
                }
            })
        }
        return loweredConstructorFunction
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrDeclaration?): IrExpression {
        expression.transformChildren(this, data)

        val constructor = expression.symbol.owner
        require(constructor.parameters.none { it.kind == IrParameterKind.ExtensionReceiver }) { "A constructor call cannot have the extension receiver: ${expression.render()}" }
        val constructedType = constructor.constructedClassType
        val loweredConstructorFunction = context.getLoweredConstructorFunction(constructor)
        val irBuilder = context.createIrBuilder(data!!.symbol, expression.startOffset, expression.endOffset)
        return when {
            constructor.constructedClass.isObjCClass() -> {
                error("A call to an Obj-C class constructor should've been lowered: ${expression.render()}")
            }
            constructor.constructedClass.isArray -> {
                require(expression.dispatchReceiver == null) { "An array constructor call cannot have the dispatch receiver: ${expression.render()}" }
                require(expression.arguments.size == 1) { "Expected a call to the array constructor with a single argument: ${expression.render()}" }
                irBuilder.irCall(createUninitializedArray, constructedType, listOf(constructedType)).apply {
                    arguments[0] = expression.arguments[0]!!
                }
            }
            constructedType.isString() -> irBuilder.run {
                require(expression.dispatchReceiver == null) { "A string constructor call cannot have the dispatch receiver: ${expression.render()}" }
                require(expression.arguments.isEmpty()) { "Expected a call to the string constructor with no arguments: ${expression.render()}" }
                irBuilder.irCall(createEmptyString, constructedType)
            }
            constructedType.isAny() -> {
                require(expression.dispatchReceiver == null) { "A kotlin.Any constructor call cannot have the dispatch receiver: ${expression.render()}" }
                require(expression.arguments.isEmpty()) { "Expected a call to the kotlin.Any constructor with no arguments: ${expression.render()}" }
                irBuilder.irCall(createUninitializedInstance, constructedType, listOf(constructedType))
            }
            else -> irBuilder.irBlock {
                val instance = irTemporary(irCall(createUninitializedInstance, constructedType, listOf(constructedType)), "inst")
                +irCall(loweredConstructorFunction).apply {
                    dispatchReceiver = irGet(instance)
                    fillArgumentsFrom(expression)
                }
                +irGet(instance)
            }
        }
    }

    override fun visitCall(expression: IrCall, data: IrDeclaration?): IrElement {
        val callee = expression.symbol.owner
        if (callee.symbol != initInstance)
            return super.visitCall(expression, data)

        val instance = expression.arguments[0]
        val constructorCall = expression.arguments[1] as IrConstructorCall
        val loweredConstructorFunction = context.getLoweredConstructorFunction(constructorCall.symbol.owner)
        val irBuilder = context.createIrBuilder(data!!.symbol, expression.startOffset, expression.endOffset)
        return irBuilder.irCall(loweredConstructorFunction).apply {
            dispatchReceiver = instance
            fillArgumentsFrom(constructorCall)

            transformChildren(this@ConstructorsLowering, data)
        }
    }

    private fun IrFunctionAccessExpression.fillArgumentsFrom(callSite: IrFunctionAccessExpression) {
        val constructedClass = callSite.symbol.owner.parentAsClass
        val outerReceiver = callSite.dispatchReceiver
        if (outerReceiver == null && constructedClass.isInner)
            error("No outer receiver is supplied for an inner class constructor call: ${callSite.render()}")
        if (outerReceiver != null && !constructedClass.isInner)
            error("The outer receiver is supplied for a non-inner class constructor call: ${callSite.render()}")
        require(callSite.symbol.owner.parameters.none { it.kind == IrParameterKind.ExtensionReceiver}) { "Unexpected extension receiver: ${callSite.render()}" }
        (0..<callSite.arguments.size).forEach {
            arguments[it + 1] = callSite.arguments[it]
        }
    }
}