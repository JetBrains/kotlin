/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.findDeclarationByName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Generate IR for function that returns appropriate enum entry for the provided integral value.
 */
internal class CEnumByValueFunctionGenerator(
        context: GeneratorContext,
        private val symbols: KonanSymbols
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator

    fun generateByValueFunction(
            companionIrClass: IrClass,
            valuesIrFunctionSymbol: IrSimpleFunctionSymbol
    ): IrFunction {
        val byValueFunctionDescriptor = companionIrClass.descriptor.findDeclarationByName<FunctionDescriptor>("byValue")!!
        val byValueIrFunction = createFunction(byValueFunctionDescriptor)
        val irValueParameter = byValueIrFunction.valueParameters.first()
        // val values: Array<E> = values()
        // var i: Int = 0
        // val size: Int = values.size
        // while (i < size) {
        //      val entry: E = values[i]
        //      if (entry.value == arg) {
        //          return entry
        //      }
        //      i++
        // }
        // throw NPE
        byValueIrFunction.body = irBuilder(irBuiltIns, byValueIrFunction.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +irReturn(irBlock {
                val values = irTemporary(irCall(valuesIrFunctionSymbol), isMutable = true)
                val inductionVariable = irTemporary(irInt(0), isMutable = true)
                val arrayClass = values.type.classOrNull!!
                val valuesSize = irCall(symbols.arraySize.getValue(arrayClass), irBuiltIns.intType).also { irCall ->
                    irCall.dispatchReceiver = irGet(values)
                }
                val getElementFn = symbols.arrayGet.getValue(arrayClass)
                val plusFun = symbols.getBinaryOperator(OperatorNameConventions.PLUS, irBuiltIns.intType, irBuiltIns.intType)
                val lessFunctionSymbol = irBuiltIns.lessFunByOperandType.getValue(irBuiltIns.intClass)
                +irWhile().also { loop ->
                    loop.condition = irCall(lessFunctionSymbol, irBuiltIns.booleanType).also { irCall ->
                        irCall.putValueArgument(0, irGet(inductionVariable))
                        irCall.putValueArgument(1, valuesSize)
                    }
                    loop.body = irBlock {
                        val entry = irTemporary(irCall(getElementFn, byValueIrFunction.returnType).also { irCall ->
                            irCall.dispatchReceiver = irGet(values)
                            irCall.putValueArgument(0, irGet(inductionVariable))
                        }, isMutable = true)
                        val valueGetter = entry.type.getClass()!!.getPropertyGetter("value")!!
                        val entryValue = irGet(irValueParameter.type, irGet(entry), valueGetter)
                        +irIfThenElse(
                                type = irBuiltIns.unitType,
                                condition = irEquals(entryValue, irGet(irValueParameter)),
                                thenPart = irReturn(irGet(entry)),
                                elsePart = irSetVar(
                                        inductionVariable,
                                        irCallOp(plusFun, irBuiltIns.intType,
                                                irGet(inductionVariable),
                                                irInt(1)
                                        )
                                )
                        )
                    }
                }
                +IrCallImpl.fromSymbolDescriptor(startOffset, endOffset, irBuiltIns.nothingType,
                        symbols.throwNullPointerException)
            })
        }
        return byValueIrFunction
    }
}