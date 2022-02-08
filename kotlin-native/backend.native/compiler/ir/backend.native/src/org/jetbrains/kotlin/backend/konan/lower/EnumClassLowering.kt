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
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_ENUM
import org.jetbrains.kotlin.backend.konan.MemoryModel
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

private class EnumSyntheticFunctionsBuilder(val context: Context) {
    fun IrBuilderWithScope.enumValues(enumClass: IrClass): IrExpression {
        val loweredEnum = this@EnumSyntheticFunctionsBuilder.context.specialDeclarationsFactory.getLoweredEnum(enumClass)
        return irCall(genericValuesSymbol, listOf(enumClass.defaultType)).apply {
            putValueArgument(0, loweredEnum.getValuesField(startOffset, endOffset))
        }
    }

    fun IrBuilderWithScope.enumValueOf(enumClass: IrClass, value: IrExpression): IrExpression {
        val loweredEnum = this@EnumSyntheticFunctionsBuilder.context.specialDeclarationsFactory.getLoweredEnum(enumClass)
        return irCall(genericValueOfSymbol, listOf(enumClass.defaultType)).apply {
            putValueArgument(0, value)
            putValueArgument(1, loweredEnum.getValuesField(startOffset, endOffset))
        }
    }

    private val genericValueOfSymbol = context.ir.symbols.valueOfForEnum

    private val genericValuesSymbol = context.ir.symbols.valuesForEnum
}

internal class NativeEnumWhenLowering constructor(context: Context) : EnumWhenLowering(context) {
    override fun mapConstEnumEntry(entry: IrEnumEntry): Int {
        val parent = entry.parentAsClass
        val loweredEnum = (context as Context).specialDeclarationsFactory.getLoweredEnumOrNull(parent)
                ?: return super.mapConstEnumEntry(entry)
        return loweredEnum.entriesMap[entry.name]!!.ordinal
    }
}

internal class EnumUsageLowering(val context: Context)
    : IrElementTransformer<IrBuilderWithScope?>, FileLoweringPass {

    private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

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

        return with(enumSyntheticFunctionsBuilder) {
            if (intrinsicType == IntrinsicType.ENUM_VALUES)
                data.enumValues(irClass)
            else
                data.enumValueOf(irClass, expression.getValueArgument(0)!!)
        }
    }

    private fun IrBuilderWithScope.loadEnumEntry(enumClass: IrClass, name: Name): IrExpression {
        val loweredEnum = this@EnumUsageLowering.context.specialDeclarationsFactory.getLoweredEnum(enumClass)
        val getterId = loweredEnum.entriesMap.getValue(name).getterId
        return irCall(loweredEnum.itemGetterSymbol, enumClass.defaultType).apply {
            dispatchReceiver = irCall(loweredEnum.valuesGetter)
            putValueArgument(0, irInt(getterId))
        }
    }
}

internal class EnumClassLowering(val context: Context) : FileLoweringPass {

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
        private val loweredEnum = context.specialDeclarationsFactory.getInternalLoweredEnum(irClass)
        private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

        fun run() {
            pullUpEnumEntriesClasses()
            createImplObject()
        }

        private fun pullUpEnumEntriesClasses() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    val correspondingClass = declaration.correspondingClass
                    declaration.correspondingClass = null
                    listOfNotNull(declaration, correspondingClass)
                } else null
            }
        }

        private fun createImplObject() {
            val implObject = loweredEnum.implObject

            val enumEntries = mutableListOf<IrEnumEntry>()
            var i = 0
            while (i < irClass.declarations.size) {
                val declaration = irClass.declarations[i]
                var delete = false
                when (declaration) {
                    is IrEnumEntry -> {
                        enumEntries.add(declaration)
                        delete = true
                    }
                    is IrFunction -> {
                        val body = declaration.body
                        if (body is IrSyntheticBody) {
                            when (body.kind) {
                                IrSyntheticBodyKind.ENUM_VALUEOF ->
                                    declaration.body = createSyntheticValueOfMethodBody(declaration)
                                IrSyntheticBodyKind.ENUM_VALUES ->
                                    declaration.body = createSyntheticValuesMethodBody(declaration)
                            }
                        }
                    }
                }
                if (delete)
                    irClass.declarations.removeAt(i)
                else
                    ++i
            }

            implObject.declarations += createSyntheticValuesPropertyDeclaration(enumEntries)

            val companion = irClass.companionObject()
            if (companion != null) {
                // The initialization order is the following:
                // - first all enum entries in their declaration order
                // - then companion object if it exists
                val constructor = implObject.constructors.single()
                context.createIrBuilder(constructor.symbol).run {
                    (constructor.body as IrBlockBody).statements += irGetObject(companion.symbol)
                }
            }

            irClass.declarations += implObject
        }

        private val createUninitializedInstance = context.ir.symbols.createUninitializedInstance.owner

        private fun createSyntheticValuesPropertyDeclaration(enumEntries: List<IrEnumEntry>): IrProperty {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            val implObject = loweredEnum.implObject
            val constructor = implObject.constructors.single()

            val irValuesInitializer = context.createArrayOfExpression(
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

                                irCall(startOffset, endOffset,
                                        createUninitializedInstance,
                                        listOf(entryClass.defaultType)
                                )

                            }
            )
            val irField = loweredEnum.valuesField
            context.createIrBuilder(constructor.symbol).run {
                (constructor.body as IrBlockBody).statements +=
                        irSetField(irGet(implObject.thisReceiver!!), irField, irValuesInitializer)
            }

            val getter = loweredEnum.valuesGetter
            context.createIrBuilder(getter.symbol).run {
                getter.body = irBlockBody(irClass) { +irReturn(irGetField(irGetObject(implObject.symbol), irField)) }
            }

            createValuesPropertyInitializer(enumEntries)

            return with(loweredEnum.valuesField.descriptor) {
                IrPropertyImpl(
                    startOffset, endOffset, DECLARATION_ORIGIN_ENUM, IrPropertySymbolImpl(this),
                    name, visibility, modality, isVar, isConst, isLateInit, isDelegated, isExternal
                ).apply {
                    this.backingField = irField
                    this.getter = getter
                    this.parent = implObject
                }
            }
        }

        private val initInstanceSymbol = context.ir.symbols.initInstance

        private val arrayGetSymbol = context.ir.symbols.array.functions.single { it.owner.name == Name.identifier("get") }

        private val arrayType = context.ir.symbols.array.typeWith(irClass.defaultType)

        private fun createValuesPropertyInitializer(enumEntries: List<IrEnumEntry>) {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            fun IrBlockBuilder.initInstanceCall(instance: IrCall, constructor: IrConstructorCall): IrCall =
                    irCall(initInstanceSymbol).apply {
                        putValueArgument(0, instance)
                        putValueArgument(1, constructor)
                    }

            val implObject = loweredEnum.implObject
            val constructor = implObject.constructors.single()
            val irBuilder = context.createIrBuilder(constructor.symbol, startOffset, endOffset)
            val valuesInitializer = irBuilder.irBlock(startOffset, endOffset) {
                val receiver = implObject.thisReceiver!!
                val instances = irTemporary(irGetField(irGet(receiver), loweredEnum.valuesField))
                val enumIndices = enumEntries.sortedBy { it.name }.withIndex().associate { it.value to it.index }
                enumEntries.forEach {
                    val instance = irCall(arrayGetSymbol).apply {
                        dispatchReceiver = irGet(instances)
                        putValueArgument(0, irInt(enumIndices[it]!!))
                    }
                    val initializer = it.initializerExpression!!.expression
                    initializer.setDeclarationsParent(constructor)
                    when {
                        initializer is IrConstructorCall -> +initInstanceCall(instance, initializer)

                        initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL -> {
                            val statements = initializer.statements
                            val constructorCall = statements.last() as IrConstructorCall
                            statements[statements.lastIndex] = initInstanceCall(instance, constructorCall)
                            +initializer
                        }

                        else -> error("Unexpected initializer: $initializer")
                    }
                }
                // Needed for legacy MM targets that do not support threads.
                if (this@EnumClassLowering.context.memoryModel != MemoryModel.EXPERIMENTAL) {
                    +irCall(this@EnumClassLowering.context.ir.symbols.freeze, listOf(arrayType)).apply {
                        extensionReceiver = irGet(receiver)
                    }
                }
            }
            (constructor.body as IrBlockBody).statements += valuesInitializer
        }

        private fun createSyntheticValuesMethodBody(declaration: IrFunction) =
                context.createIrBuilder(declaration.symbol, irClass.startOffset, irClass.endOffset).irBlockBody {
                    +irReturn(with(enumSyntheticFunctionsBuilder) { enumValues(irClass) })
                }

        private fun createSyntheticValueOfMethodBody(declaration: IrFunction) =
                context.createIrBuilder(declaration.symbol, irClass.startOffset, irClass.endOffset).irBlockBody {
                    +irReturn(with(enumSyntheticFunctionsBuilder) { enumValueOf(irClass, irGet(declaration.valueParameters[0])) })
                }
    }
}
