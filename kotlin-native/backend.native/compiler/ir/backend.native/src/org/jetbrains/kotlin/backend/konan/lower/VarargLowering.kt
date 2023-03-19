/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.ir.KonanNameConventions
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class VarargInjectionLowering constructor(val context: KonanBackendContext): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach{
            when (it) {
                is IrField    -> lower(it.symbol, it.initializer)
                is IrFunction -> lower(it.symbol, it.body)
                is IrProperty -> {
                    it.backingField?.let { field ->
                        lower(field.symbol, field)
                    }
                    it.getter?.let { getter ->
                        lower(getter.symbol, getter)
                    }
                    it.setter?.let { setter ->
                        lower(setter.symbol, setter)
                    }
                }
            }
        }
    }

    private fun lower(owner: IrSymbol, element: IrElement?) {
        element?.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                // Skip nested.
                return declaration
            }

            val transformer = this

            private fun replaceEmptyParameterWithEmptyArray(expression: IrFunctionAccessExpression) {
                val callee = expression.symbol.owner
                log { "call of: ${callee.fqNameForIrSerialization}" }
                context.createIrBuilder(owner, expression.startOffset, expression.endOffset).apply {
                    callee.valueParameters.forEach {
                        log { "varargElementType: ${it.varargElementType} expr: ${ir2string(expression.getValueArgument(it.index))}" }
                    }
                    callee.valueParameters
                        .filter { it.varargElementType != null && expression.getValueArgument(it.index) == null }
                        .forEach {
                            expression.putValueArgument(
                                it.index,
                                IrVarargImpl(
                                    startOffset = startOffset,
                                    endOffset = endOffset,
                                    type = it.type,
                                    varargElementType = it.varargElementType!!
                                )
                            )
                        }
                }
                expression.transformChildrenVoid(this)
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                replaceEmptyParameterWithEmptyArray(expression)
                return expression
            }

            override fun visitVararg(expression: IrVararg): IrExpression {
                expression.transformChildrenVoid(transformer)

                val hasSpreadElement = hasSpreadElement(expression)
                val type = expression.varargElementType
                val arrayHandle = arrayType(expression.type)
                val irBuilder = context.createIrBuilder(owner, expression.startOffset, expression.endOffset)
                val isConstantVararg = expression.elements.all {
                    it is IrConst<*> || it is IrConstantValue
                }
                if (isConstantVararg) {
                    return irBuilder.irCall(
                            arrayHandle.copyOfSymbol,
                    ).apply {
                        extensionReceiver = arrayHandle.createStatic(irBuilder, type, expression.elements.map {
                            when (it) {
                                is IrConst<*> -> irBuilder.irConstantPrimitive(it)
                                is IrConstantValue -> it
                                else -> throw IllegalStateException("Try to initialize vararg constantly, when it's impossible")
                            }
                        })
                    }
                }
                return irBuilder.irBlock(expression, null, expression.type) {
                    log { "$expression: array type:$type, is array of primitives ${!expression.type.isArray()}" }

                    val vars = expression.elements.map {
                        it to irTemporary(
                                (it as? IrSpreadElement)?.expression ?: it as IrExpression,
                                "elem".synthesizedString,
                        )
                    }.toMap()
                    val arraySize = calculateArraySize(arrayHandle, hasSpreadElement, scope, expression, vars)
                    val array = arrayHandle.createArray(this, expression.varargElementType, arraySize)

                    val arrayTmpVariable = irTemporary(array, "array".synthesizedString)
                    lateinit var indexTmpVariable: IrVariable
                    if (hasSpreadElement)
                        indexTmpVariable = irTemporary(kIntZero, "index".synthesizedString, isMutable = true)
                    expression.elements.forEachIndexed { i, element ->
                        irBuilder.at(element.startOffset, element.endOffset)
                        log { "element:$i> ${ir2string(element)}" }
                        val dst = vars[element]!!
                        if (element !is IrSpreadElement) {
                            +irCall(arrayHandle.setMethodSymbol).apply {
                                dispatchReceiver = irGet(arrayTmpVariable)
                                putValueArgument(0, if (hasSpreadElement) irGet(indexTmpVariable) else irConstInt(i))
                                putValueArgument(1, irGet(dst))
                            }
                            if (hasSpreadElement)
                                +incrementVariable(indexTmpVariable, kIntOne)
                        } else {
                            val arraySizeVariable = irTemporary(irArraySize(arrayHandle, irGet(dst)), "length".synthesizedString)
                            +irCall(arrayHandle.copyIntoSymbol.owner).apply {
                                extensionReceiver = irGet(dst)
                                putValueArgument(0, irGet(arrayTmpVariable))  /* destination */
                                putValueArgument(1, irGet(indexTmpVariable))  /* destinationOffset */
                                putValueArgument(2, kIntZero)                 /* startIndex */
                                putValueArgument(3, irGet(arraySizeVariable)) /* endIndex */
                            }
                            +incrementVariable(indexTmpVariable, irGet(arraySizeVariable))
                            log { "element:$i:spread element> ${ir2string(element.expression)}" }
                        }
                    }
                    +irGet(arrayTmpVariable)
                }
            }
        })
    }

    private val symbols = context.ir.symbols
    private val intPlusInt = symbols.getBinaryOperator(
            OperatorNameConventions.PLUS, context.irBuiltIns.intType, context.irBuiltIns.intType
    ).owner

    private fun arrayType(type: IrType): ArrayHandle {
        val arrayClass = type.classifierOrFail
        return arrayToHandle[arrayClass] ?: error(arrayClass)
    }

    private fun IrBuilderWithScope.intPlus() = irCall(intPlusInt)
    private fun IrBuilderWithScope.increment(expression: IrExpression, value: IrExpression): IrExpression {
        return intPlus().apply {
            dispatchReceiver = expression
            putValueArgument(0, value)
        }
    }

    private fun IrBuilderWithScope.incrementVariable(variable: IrVariable, value: IrExpression): IrExpression {
        return irSet(variable.symbol, intPlus().apply {
            dispatchReceiver = irGet(variable)
            putValueArgument(0, value)
        })
    }

    private fun calculateArraySize(arrayHandle: ArrayHandle, hasSpreadElement: Boolean, scope:Scope, expression: IrVararg, vars: Map<IrVarargElement, IrVariable>): IrExpression {
        context.createIrBuilder(scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
            if (!hasSpreadElement)
                return irConstInt(expression.elements.size)
            val notSpreadElementCount = expression.elements.filter { it !is IrSpreadElement}.size
            val initialValue = irConstInt(notSpreadElementCount) as IrExpression
            return vars.filter{it.key is IrSpreadElement}.toList().fold( initial = initialValue) { result, it ->
                val arraySize = irArraySize(arrayHandle, irGet(it.second))
                increment(result, arraySize)
            }
        }
    }

    private fun IrBuilderWithScope.irArraySize(arrayHandle: ArrayHandle, expression: IrExpression): IrExpression {
        val arraySize = irCall(arrayHandle.sizeGetterSymbol.owner).apply {
            dispatchReceiver = expression
        }
        return arraySize
    }


    private fun hasSpreadElement(expression: IrVararg?) = expression?.elements?.any { it is IrSpreadElement }?:false

    private fun log(msg:() -> String) {
        context.log { "VARARG-INJECTOR:    ${msg()}" }
    }

    abstract inner class ArrayHandle(val arraySymbol: IrClassSymbol) {
        val setMethodSymbol = with(arraySymbol.owner.functions) {
            // For unsigned types use set method.
            singleOrNull { it.name == KonanNameConventions.setWithoutBoundCheck } ?: single { it.name == OperatorNameConventions.SET }
        }
        val sizeGetterSymbol = arraySymbol.getPropertyGetter("size")!!
        val copyIntoSymbol = symbols.copyInto[arraySymbol]!!
        val copyOfSymbol = symbols.copyOf[arraySymbol]!!
        protected val singleParameterConstructor =
                arraySymbol.owner.constructors.find { it.valueParameters.size == 1 }!!

        abstract fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression
        abstract fun createStatic(
                builder: IrBuilderWithScope,
                elementType: IrType,
                values: List<IrConstantValue>): IrConstantValue
    }

    inner class ReferenceArrayHandle : ArrayHandle(symbols.array) {
        override fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression {
            return builder.irCall(singleParameterConstructor).apply {
                putTypeArgument(0, elementType)
                putValueArgument(0, size)
            }
        }

        override fun createStatic(
                builder: IrBuilderWithScope,
                elementType: IrType,
                values: List<IrConstantValue>): IrConstantValue {
            return builder.irConstantArray(
                    arraySymbol.typeWith(listOf(elementType)),
                    values
            )
        }
    }

    inner class PrimitiveArrayHandle(primitiveType: PrimitiveType)
        : ArrayHandle(symbols.irBuiltIns.primitiveTypesToPrimitiveArrays[primitiveType]!!) {

        override fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression {
            return builder.irCall(singleParameterConstructor).apply {
                putValueArgument(0, size)
            }
        }

        override fun createStatic(builder: IrBuilderWithScope, elementType: IrType, values: List<IrConstantValue>): IrConstantValue {
            return builder.irConstantArray(
                    arraySymbol.defaultType,
                    values
            )
        }
    }

    inner class UnsignedArrayHandle(
            arraySymbol: IrClassSymbol,
            private val wrappedArrayHandle: PrimitiveArrayHandle
    ) : ArrayHandle(arraySymbol) {

        override fun createArray(builder: IrBuilderWithScope, elementType: IrType, size: IrExpression): IrExpression {
            val wrappedArray = wrappedArrayHandle.createArray(builder, elementType, size)
            return builder.irCall(singleParameterConstructor).apply {
                putValueArgument(0, wrappedArray)
            }
        }

        override fun createStatic(builder: IrBuilderWithScope, elementType: IrType, values: List<IrConstantValue>): IrConstantValue {
            val casted = values.map {
                require(it.type == elementType)
                when (it) {
                    is IrConstantPrimitive -> {
                        val castedConst = when (it.value.kind) {
                            IrConstKind.Byte -> IrConstImpl.byte(it.startOffset, it.endOffset, symbols.byte.defaultType, it.value.value as Byte)
                            IrConstKind.Short -> IrConstImpl.short(it.startOffset, it.endOffset, symbols.short.defaultType, it.value.value as Short)
                            IrConstKind.Int -> IrConstImpl.int(it.startOffset, it.endOffset, symbols.int.defaultType, it.value.value as Int)
                            IrConstKind.Long -> IrConstImpl.long(it.startOffset, it.endOffset, symbols.long.defaultType, it.value.value as Long)
                            else -> error("Unsupported unsigned constant")
                        }
                        builder.irConstantPrimitive(castedConst)
                    }
                    is IrConstantObject -> {
                        it.valueArguments.singleOrNull()
                                ?: error("Constant of type ${it.type.render()} is expected to have exactly one field")
                    }
                    else -> error("unsigned integer can't be represented as ${it::class.qualifiedName}")
                }
            }
            return builder.irConstantObject(
                    arraySymbol.owner,
                    mapOf(
                            "storage" to wrappedArrayHandle.createStatic(builder, elementType, casted)
                    )
            )
        }
    }

    private val primitiveArrayHandles = PrimitiveType.values().associate { it to PrimitiveArrayHandle(it) }

    private val unsignedArrayHandles = UnsignedType.values().mapNotNull { unsignedType ->
        symbols.unsignedTypesToUnsignedArrays[unsignedType]?.let {
            val primitiveType = when (unsignedType) {
                UnsignedType.UBYTE -> PrimitiveType.BYTE
                UnsignedType.USHORT -> PrimitiveType.SHORT
                UnsignedType.UINT -> PrimitiveType.INT
                UnsignedType.ULONG -> PrimitiveType.LONG
            }
            UnsignedArrayHandle(it, primitiveArrayHandles[primitiveType]!!)
        }
    }


    val arrayToHandle =
        (primitiveArrayHandles.values + unsignedArrayHandles + ReferenceArrayHandle()).associateBy { it.arraySymbol }

}

private fun IrBuilderWithScope.irConstInt(value: Int): IrConst<Int> =
    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, value)
private val IrBuilderWithScope.kIntZero get() = irConstInt(0)
private val IrBuilderWithScope.kIntOne get() = irConstInt(1)
