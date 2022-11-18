/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.lower.EnumWhenLowering
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.serialization.unlinked.isPartiallyLinked
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeMapping
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.KonanNameConventions
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal data class LoweredEnumEntryDescription(val ordinal: Int, val getterId: Int)

internal class EnumsSupport(
        mapping: NativeMapping,
        private val irBuiltIns: IrBuiltIns,
        private val irFactory: IrFactory,
) {
    private val enumValueGetters = mapping.enumValueGetters
    private val enumEntriesMaps = mapping.enumEntriesMaps

    fun enumEntriesMap(enumClass: IrClass): Map<Name, LoweredEnumEntryDescription> {
        require(enumClass.isEnumClass) { "Expected enum class but was: ${enumClass.render()}" }
        return enumEntriesMaps.getOrPut(enumClass) {
            data class NameWithOrdinal(val name: Name, val ordinal: Int)
            enumClass.declarations.asSequence()
                    .filterIsInstance<IrEnumEntry>()
                    .mapIndexed { index, it -> NameWithOrdinal(it.name, index) }
                    .sortedBy { it.name }
                    .withIndex()
                    .associate { it.value.name to LoweredEnumEntryDescription(it.value.ordinal, it.index) }
                    .toMap()
        }
    }

    fun getValueGetter(enumClass: IrClass): IrFunction {
        require(enumClass.isEnumClass) { "Expected enum class but was: ${enumClass.render()}" }
        return enumValueGetters.getOrPut(enumClass) {
            irFactory.buildFun {
                startOffset = enumClass.startOffset
                endOffset = enumClass.endOffset
                origin = DECLARATION_ORIGIN_ENUM
                name = "getEnumAt".synthesizedName
                returnType = enumClass.defaultType
            }.apply {
                parent = enumClass

                addValueParameter {
                    name = Name.identifier("getterId")
                    origin = DECLARATION_ORIGIN_ENUM
                    type = irBuiltIns.intType
                }
            }
        }
    }
}

internal object DECLARATION_ORIGIN_ENUM : IrDeclarationOriginImpl("ENUM")

internal class NativeEnumWhenLowering constructor(context: Context) : EnumWhenLowering(context) {
    override fun mapConstEnumEntry(entry: IrEnumEntry): Int {
        val enumEntriesMap = (context as Context).enumsSupport.enumEntriesMap(entry.parentAsClass)
        return enumEntriesMap[entry.name]!!.ordinal
    }
}

internal class EnumUsageLowering(val context: Context) : IrElementTransformer<IrBuilderWithScope?>, FileLoweringPass {
    private val enumsSupport = context.enumsSupport

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

        return when (intrinsicType) {
            IntrinsicType.ENUM_VALUES -> {
                val function = irClass.simpleFunctions().single {
                    it.name == Name.identifier("values") && it.dispatchReceiverParameter == null
                }
                data.irCall(function)
            }
            IntrinsicType.ENUM_VALUE_OF -> {
                val function = irClass.simpleFunctions().single {
                    it.name == Name.identifier("valueOf") && it.dispatchReceiverParameter == null
                }
                data.irCall(function).apply {
                    putValueArgument(0, expression.getValueArgument(0)!!)
                }
            }
            else -> TODO("Unsupported intrinsic type ${intrinsicType}")
        }
    }

    private fun IrBuilderWithScope.loadEnumEntry(enumClass: IrClass, name: Name) = with (enumsSupport) {
        irCall(getValueGetter(enumClass).symbol, enumClass.defaultType).apply {
            putValueArgument(0, irInt(enumEntriesMap(enumClass).getValue(name).getterId))
        }
    }

}

internal class EnumClassLowering(val context: Context) : FileLoweringPass {
    private val enumsSupport = context.enumsSupport
    private val symbols = context.ir.symbols
    private val createUninitializedInstance = symbols.createUninitializedInstance
    private val createEnumEntries = symbols.createEnumEntries
    private val initInstance = symbols.initInstance
    private val arrayGet = symbols.array.owner.functions.single { it.name == KonanNameConventions.getWithoutBoundCheck }.symbol

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid()
                if (declaration.kind == ClassKind.ENUM_CLASS && !declaration.isPartiallyLinked())
                    EnumClassTransformer(declaration).run()
                return declaration
            }
        })
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val valuesField = context.irFactory.buildField {
            startOffset = irClass.startOffset
            endOffset = irClass.endOffset
            origin = DECLARATION_ORIGIN_ENUM
            name = "VALUES".synthesizedName
            type = context.irBuiltIns.arrayClass.typeWith(irClass.defaultType)
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isStatic = true
        }
        private val entriesField = context.irFactory.buildField {
            startOffset = irClass.startOffset
            endOffset = irClass.endOffset
            origin = DECLARATION_ORIGIN_ENUM
            name = "ENTRIES".synthesizedName
            type = symbols.enumEntriesInterface.typeWith(irClass.defaultType)
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isStatic = true
        }

        // also saves this in enumSupport before removing them from list
        private val enumEntriesMap = enumsSupport.enumEntriesMap(irClass)


        fun run() {
            val enumEntries = transformEnumBody()
            // These fields are inserted into begining to be initialized before companion object
            // The values field should be initialized first, so we need to insert entries field first
            defineEntriesField()
            defineValuesField(enumEntries)
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

                    else -> null
                }
            }
            irClass.simpleFunctions().forEach { declaration ->
                val body = declaration.body
                if (body is IrSyntheticBody) {
                    declaration.body = when (body.kind) {
                        IrSyntheticBodyKind.ENUM_VALUEOF -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                            +irReturn(irCall(symbols.valueOfForEnum, listOf(irClass.defaultType)).apply {
                                putValueArgument(0, irGet(declaration.valueParameters[0]))
                                putValueArgument(1, irGetField(null, valuesField))
                            })
                        }

                        IrSyntheticBodyKind.ENUM_VALUES -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                            +irReturn(irCall(symbols.valuesForEnum, listOf(irClass.defaultType)).apply {
                                putValueArgument(0, irGetField(null, valuesField))
                            })
                        }

                        IrSyntheticBodyKind.ENUM_ENTRIES -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                            +irReturn(irGetField(null, entriesField))
                        }
                    }
                }
            }
            return enumEntries
        }

        private fun defineValueGetter() {
            val valueGetter = enumsSupport.getValueGetter(irClass)
            context.createIrBuilder(valueGetter.symbol).run {
                valueGetter.body = irBlockBody(valueGetter) {
                    +irReturn(irCall(arrayGet, irClass.defaultType).apply {
                        dispatchReceiver = irGetField(null, valuesField)
                        putValueArgument(0, irGet(valueGetter.valueParameters[0]))
                    })
                }
            }
            irClass.declarations.add(valueGetter)
        }

        private fun IrBlockBuilder.irInitInstanceCall(instance: IrCall, constructor: IrConstructorCall): IrCall =
                irCall(initInstance).apply {
                    putValueArgument(0, instance)
                    putValueArgument(1, constructor)
                }

        private fun defineValuesField(enumEntries: List<IrEnumEntry>) {
            irClass.declarations.add(0, valuesField)
            valuesField.parent = irClass
            val irBuilder = context.createIrBuilder(valuesField.symbol, irClass.startOffset, irClass.endOffset)

            valuesField.initializer = irBuilder.irExprBody(irBuilder.irBlock {
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
                val instances = irTemporary(irValuesInitializer)
                +irSetField(null, valuesField, irGet(instances), origin = ObjectClassLowering.IrStatementOriginFieldPreInit)
                callEnumEntriesConstructors(instances, enumEntries)
                +irGet(instances)
            }).also {
                it.setDeclarationsParent(valuesField)
            }
            valuesField.annotations += buildSimpleAnnotation(context.irBuiltIns, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.ir.symbols.sharedImmutable.owner)
        }

        fun defineEntriesField() {
            irClass.declarations.add(0, entriesField)
            entriesField.parent = irClass
            val irBuilder = context.createIrBuilder(entriesField.symbol, irClass.startOffset, irClass.endOffset)
            entriesField.initializer = irBuilder.irExprBody(irBuilder.irBlock {
                val irValuesArray = irTemporary(irGetField(null, valuesField))
                val irEntriesArray = this@EnumClassLowering.context.createArrayOfExpression(
                        startOffset, endOffset,
                        irClass.defaultType,
                        enumEntriesMap.values
                                .sortedBy { it.ordinal }
                                .map {
                                    irCall(arrayGet, irClass.defaultType).apply {
                                        dispatchReceiver = irGet(irValuesArray)
                                        putValueArgument(0, irInt(it.getterId))
                                    }
                                }
                )
                +irCall(createEnumEntries, listOf(irClass.defaultType)).apply {
                    putValueArgument(0, irEntriesArray)
                }
            })
        }

        private fun IrBlockBuilder.callEnumEntriesConstructors(instances: IrVariable, enumEntries: List<IrEnumEntry>) {
            enumEntries.forEach {
                val instance = irCall(arrayGet).apply {
                    dispatchReceiver = irGet(instances)
                    putValueArgument(0, irInt(enumEntriesMap[it.name]!!.getterId))
                }
                val initializer = it.initializerExpression!!.expression
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
        }
    }
}
