/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.EnumWhenLowering
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.MemoryModel
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class NativeEnumWhenLowering constructor(context: Context) : EnumWhenLowering(context) {
    override fun mapConstEnumEntry(entry: IrEnumEntry): Int {
        val enumEntriesMap = (context as Context).enumsSupport.enumEntriesMap(entry.parentAsClass)
        return enumEntriesMap[entry.name]!!.ordinal
    }
}

internal class EnumUsageLowering(val context: Context) : IrElementTransformer<IrBuilderWithScope?>, FileLoweringPass {
    private val enumsSupport = context.enumsSupport
    private val symbols = context.ir.symbols
    private val arrayGet = symbols.arrayGet[symbols.array]!!

    override fun lower(irFile: IrFile) {
        visitFile(irFile, data = null)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrBuilderWithScope?): IrStatement {
        return super.visitDeclaration(declaration, context.createIrBuilder(declaration.symbol))
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: IrBuilderWithScope?): IrExpression {
        val entry = expression.symbol.owner
        return data!!.at(expression).loadEnumEntry(entry.parentAsClass, entry.name)
    }

    override fun visitCall(expression: IrCall, data: IrBuilderWithScope?): IrExpression {
        expression.transformChildren(this, data)

        val intrinsicType = tryGetIntrinsicType(expression)
        if (intrinsicType != IntrinsicType.ENUM_VALUES && intrinsicType != IntrinsicType.ENUM_VALUE_OF)
            return expression

        data!!.at(expression)

        val irClassSymbol = expression.getTypeArgument(0)!!.classifierOrNull as? IrClassSymbol

        if (irClassSymbol == null || irClassSymbol == context.ir.symbols.enum) {
            // Either a type parameter or a type parameter erased to 'Enum'.
            return data.irCall(context.ir.symbols.throwIllegalStateException)
        }

        val irClass = irClassSymbol.owner

        require(irClass.kind == ClassKind.ENUM_CLASS)

        return with(enumsSupport) {
            if (intrinsicType == IntrinsicType.ENUM_VALUES)
                data.irEnumValues(irClass)
            else
                data.irEnumValueOf(irClass, expression.getValueArgument(0)!!)
        }
    }

    private fun IrBuilderWithScope.loadEnumEntry(enumClass: IrClass, name: Name) =
            irCall(arrayGet, enumClass.defaultType).apply {
                dispatchReceiver = with(enumsSupport) { irGetValuesField(enumClass) }
                putValueArgument(0, irInt(enumsSupport.enumEntriesMap(enumClass).getValue(name).getterId))
            }
}

internal class EnumClassLowering(val context: Context) : FileLoweringPass {
    private val enumsSupport = context.enumsSupport
    private val symbols = context.ir.symbols
    private val createUninitializedInstance = symbols.createUninitializedInstance
    private val initInstance = symbols.initInstance
    private val arrayGet = symbols.arrayGet[symbols.array]!!
    private val constructorOfAny = context.irBuiltIns.anyClass.owner.constructors.first()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid()
                if (declaration.kind == ClassKind.ENUM_CLASS)
                    EnumClassTransformer(declaration).run()
                return declaration
            }
        })
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val implObject = enumsSupport.getImplObject(irClass)
        private val valuesField = enumsSupport.getValuesField(implObject)
        private val enumEntriesMap = enumsSupport.enumEntriesMap(irClass)

        fun run() {
            val enumEntries = transformEnumBody()
            defineImplObject(enumEntries)
            defineValueGetter()
        }

        private fun transformEnumBody(): List<IrEnumEntry> {
            val enumEntries = mutableListOf<IrEnumEntry>()
            irClass.declarations.transformFlat { declaration ->
                when (declaration) {
                    is IrEnumEntry -> {
                        enumEntries.add(declaration)
                        val correspondingClass = declaration.correspondingClass
                        declaration.correspondingClass = null
                        listOfNotNull(correspondingClass)
                    }

                    is IrFunction -> {
                        val body = declaration.body
                        if (body is IrSyntheticBody) {
                            declaration.body = when (body.kind) {
                                IrSyntheticBodyKind.ENUM_VALUEOF -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                                    +irReturn(with(enumsSupport) { irEnumValueOf(irClass, irGet(declaration.valueParameters[0])) })
                                }

                                IrSyntheticBodyKind.ENUM_VALUES -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                                    +irReturn(with(enumsSupport) { irEnumValues(irClass) })
                                }

                                IrSyntheticBodyKind.ENUM_ENTRIES -> TODO("KT-48872 is not yet supported")
                            }
                        }
                        null
                    }

                    else -> null
                }
            }
            return enumEntries
        }

        private fun defineValueGetter() {
            val valueGetter = enumsSupport.getValueGetter(irClass)
            context.createIrBuilder(valueGetter.symbol).run {
                valueGetter.body = irBlockBody(valueGetter) {
                    +irReturn(irCall(arrayGet, irClass.defaultType).apply {
                        dispatchReceiver = with(enumsSupport) { irGetValuesField(irClass) }
                        putValueArgument(0, irGet(valueGetter.valueParameters[0]))
                    })
                }
            }
            irClass.declarations.add(valueGetter)
        }

        private fun defineImplObject(enumEntries: List<IrEnumEntry>) {
            implObject.createParameterDeclarations()

            implObject.addSimpleDelegatingConstructor(constructorOfAny, context.irBuiltIns, true /* TODO: why primary? */)
            implObject.addFakeOverrides(context.typeSystem)
            irClass.declarations.add(implObject)

            ImplConstructorBuilder(implObject.constructors.single()).build(enumEntries)
        }

        private inner class ImplConstructorBuilder(val constructor: IrConstructor) {
            private val irBuilder = context.createIrBuilder(constructor.symbol, constructor.startOffset, constructor.endOffset)

            fun build(enumEntries: List<IrEnumEntry>) {
                val statements = (constructor.body as IrBlockBody).statements

                // The initialization order is the following:
                // - first all enum entries in their declaration order
                // - then companion object if it exists
                statements += buildValuesFieldInitializer(enumEntries)
                // Split allocation and constructors calling because enum entries can reference one another.
                statements += callEnumEntriesConstructors(enumEntries)

                irClass.companionObject()?.let { statements += irBuilder.irGetObject(it.symbol) }
            }

            private fun IrBlockBuilder.irInitInstanceCall(instance: IrCall, constructor: IrConstructorCall): IrCall =
                    irCall(initInstance).apply {
                        putValueArgument(0, instance)
                        putValueArgument(1, constructor)
                    }

            private fun buildValuesFieldInitializer(enumEntries: List<IrEnumEntry>) = irBuilder.run {
                val irValuesInitializer = this@EnumClassLowering.context.createArrayOfExpression(
                        startOffset, endOffset,
                        irClass.defaultType,
                        enumEntries
                                .sortedBy { it.name }
                                .map {
                                    val initializer = it.initializerExpression?.expression
                                    val entryConstructorCall = when {
                                        initializer is IrConstructorCall -> initializer

                                        initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL ->
                                            initializer.statements.last() as IrConstructorCall

                                        else -> error("Unexpected initializer: $initializer")
                                    }
                                    val entryClass = entryConstructorCall.symbol.owner.constructedClass

                                    irCall(createUninitializedInstance, listOf(entryClass.defaultType))
                                }
                )
                irSetField(irGet(implObject.thisReceiver!!), valuesField, irValuesInitializer)
            }

            private fun callEnumEntriesConstructors(enumEntries: List<IrEnumEntry>) = irBuilder.irBlock {
                val receiver = implObject.thisReceiver!!
                val instances = irTemporary(irGetField(irGet(receiver), valuesField))
                enumEntries.forEach {
                    val instance = irCall(arrayGet).apply {
                        dispatchReceiver = irGet(instances)
                        putValueArgument(0, irInt(enumEntriesMap[it.name]!!.getterId))
                    }
                    val initializer = it.initializerExpression!!.expression
                    initializer.setDeclarationsParent(constructor)
                    when {
                        initializer is IrConstructorCall -> +irInitInstanceCall(instance, initializer)

                        initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL -> {
                            val statements = initializer.statements
                            val constructorCall = statements.last() as IrConstructorCall
                            statements[statements.lastIndex] = irInitInstanceCall(instance, constructorCall)
                            +initializer
                        }

                        else -> error("Unexpected initializer: $initializer")
                    }
                }
                // Needed for legacy MM targets that do not support threads.
                if (this@EnumClassLowering.context.memoryModel != MemoryModel.EXPERIMENTAL) {
                    +irCall(this@EnumClassLowering.context.ir.symbols.freeze, listOf(valuesField.type)).apply {
                        extensionReceiver = irGet(receiver)
                    }
                }
            }
        }
    }
}
