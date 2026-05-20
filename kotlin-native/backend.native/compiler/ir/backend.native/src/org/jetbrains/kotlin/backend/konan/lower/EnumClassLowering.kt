/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createArrayOfExpression
import org.jetbrains.kotlin.backend.common.lower.EnumWhenLowering
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.KonanNameConventions
import org.jetbrains.kotlin.backend.konan.IntrinsicType
import org.jetbrains.kotlin.backend.konan.ir.tryGetIntrinsicType
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
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.ARGUMENTS_REORDERING_FOR_CALL
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

private var IrClass.enumValueGetter: IrSimpleFunction? by irAttribute(copyByDefault = false)
private var IrClass.enumEntriesMap: Map<Name, LoweredEnumEntryDescription>? by irAttribute(copyByDefault = false)

internal data class LoweredEnumEntryDescription(val ordinal: Int, val getterId: Int)

private data class StaticPlainEnumEntry(val enumEntry: IrEnumEntry, val constructorCall: IrConstructorCall)

internal class EnumsSupport(
        private val irBuiltIns: IrBuiltIns,
        private val irFactory: IrFactory,
) {
    fun enumEntriesMap(enumClass: IrClass): Map<Name, LoweredEnumEntryDescription> {
        require(enumClass.isEnumClass) { "Expected enum class but was: ${enumClass.render()}" }
        return enumClass::enumEntriesMap.getOrSetIfNull {
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

    fun getValueGetter(enumClass: IrClass): IrSimpleFunction {
        require(enumClass.isEnumClass) { "Expected enum class but was: ${enumClass.render()}" }
        return enumClass::enumValueGetter.getOrSetIfNull {
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

internal val DECLARATION_ORIGIN_ENUM = IrDeclarationOriginImpl("ENUM")

internal class NativeEnumWhenLowering constructor(context: Context) : EnumWhenLowering(context) {
    override fun mapConstEnumEntry(entry: IrEnumEntry): Int {
        val enumEntriesMap = (context as Context).enumsSupport.enumEntriesMap(entry.parentAsClass)
        return enumEntriesMap[entry.name]!!.ordinal
    }
}

internal class EnumUsageLowering(val context: Context) : IrTransformer<IrBuilderWithScope?>(), FileLoweringPass {
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
        if (intrinsicType != IntrinsicType.ENUM_VALUES && intrinsicType != IntrinsicType.ENUM_VALUE_OF && intrinsicType != IntrinsicType.ENUM_ENTRIES)
            return expression

        data!!.at(expression)

        val irClassSymbol = expression.typeArguments[0]!!.classifierOrNull as? IrClassSymbol

        if (irClassSymbol == null || irClassSymbol == context.irBuiltIns.enumClass) {
            // Either a type parameter or a type parameter erased to 'Enum'.
            return data.irCall(context.symbols.throwIllegalStateException)
        }

        val irClass = irClassSymbol.owner

        require(irClass.kind == ClassKind.ENUM_CLASS)

        fun IrClass.findStaticMethod(name: Name) = simpleFunctions().single {
            it.name == name && it.dispatchReceiverParameter == null
        }

        return when (intrinsicType) {
            IntrinsicType.ENUM_VALUES -> {
                val function = irClass.findStaticMethod(Name.identifier("values"))
                data.irCall(function)
            }
            IntrinsicType.ENUM_VALUE_OF -> {
                val function = irClass.findStaticMethod(Name.identifier("valueOf"))
                data.irCall(function).apply {
                    arguments[0] = expression.arguments[0]!!
                }
            }
            IntrinsicType.ENUM_ENTRIES -> {
                val entriesProperty = irClass.properties.singleOrNull {
                    it.name == Name.identifier("entries") && it.getter != null && it.getter!!.dispatchReceiverParameter == null
                }
                if (entriesProperty != null) {
                    data.irCall(entriesProperty.getter!!)
                } else {
                    // fallback for enums from old klibs
                    val valuesFunction = irClass.findStaticMethod(Name.identifier("values"))
                    data.irCallWithSubstitutedType(context.symbols.createEnumEntries, listOf(irClass.defaultType)).apply {
                        arguments[0] = data.irCall(valuesFunction)
                    }
                }
            }
        }
    }

    private fun IrBuilderWithScope.loadEnumEntry(enumClass: IrClass, name: Name) = with(enumsSupport) {
        irCall(getValueGetter(enumClass).symbol, enumClass.defaultType).apply {
            arguments[0] = irInt(enumEntriesMap(enumClass).getValue(name).getterId)
        }
    }

}

internal class EnumClassLowering(val context: Context) : FileLoweringPass {
    private val enumsSupport = context.enumsSupport
    private val symbols = context.symbols
    private val createUninitializedInstance = symbols.createUninitializedInstance
    private val createEnumEntries = symbols.createEnumEntries
    private val enumEntriesListConstructor = symbols.enumEntriesListConstructor
    private val initInstance = symbols.initInstance
    private val arrayGet = context.irBuiltIns.arrayClass.owner.functions.single { it.name == KonanNameConventions.getWithoutBoundCheck }.symbol

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

        // Also saves this in enumSupport before removing enum entries from the declaration list.
        private val enumEntriesMap = enumsSupport.enumEntriesMap(irClass)

        fun run() {
            val enumEntries = irClass.declarations.filterIsInstance<IrEnumEntry>()
            val staticPlainEnumEntries = staticPlainEnumEntriesOrNull(enumEntries)
            transformEnumBody()
            // These fields are inserted at the beginning to be initialized before the companion object.
            // Since add(0, ...) reverses declaration order, insert $ENTRIES first so $VALUES comes first.
            if (staticPlainEnumEntries != null) {
                defineStaticPlainEntriesField(staticPlainEnumEntries)
                defineStaticPlainValuesField(staticPlainEnumEntries)
            } else {
                defineEntriesField()
                defineValuesField(enumEntries)
            }
            defineValueGetter()
        }

        private fun transformEnumBody() {
            irClass.declarations.transformFlat { declaration ->
                when (declaration) {
                    is IrEnumEntry -> {
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
                            +irReturn(irCallWithSubstitutedType(symbols.valueOfForEnum, listOf(irClass.defaultType)).apply {
                                arguments[0] = irGet(declaration.parameters[0])
                                arguments[1] = irGetField(null, valuesField)
                            })
                        }

                        IrSyntheticBodyKind.ENUM_VALUES -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                            +irReturn(irCallWithSubstitutedType(symbols.valuesForEnum, listOf(irClass.defaultType)).apply {
                                arguments[0] = irGetField(null, valuesField)
                            })
                        }

                        IrSyntheticBodyKind.ENUM_ENTRIES -> context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                            +irReturn(irGetField(null, entriesField))
                        }
                    }
                }
            }
        }

        // Returns static enum-entry descriptions only for the subset whose construction is fully represented by
        // constant (name, ordinal) arguments. Any extra enum state or initialization work must keep using the
        // dynamic path below.
        private fun staticPlainEnumEntriesOrNull(enumEntries: List<IrEnumEntry>): List<StaticPlainEnumEntry>? {
            if (!context.config.staticPlainEnumEntries) return null

            // Keep this predicate intentionally strict: static data must not skip any user-visible initialization work.
            if (irClass.declarations.any { it is IrAnonymousInitializer }) return null
            if (irClass.declarations.filterIsInstance<IrField>().any { !it.isStatic }) return null
            if (!irClass.constructors.all { it.isLoweredSimpleEnumConstructor() }) return null

            return enumEntries.map { enumEntry ->
                if (enumEntry.correspondingClass != null) return null
                val constructorCall = enumEntry.loweredConstructorCallOrNull() ?: return null
                if (constructorCall.symbol.owner.parentAsClass != irClass) return null
                if (constructorCall.symbol.owner.parameters.size != 2) return null
                if (!constructorCall.arguments[0].isConstString(enumEntry.name.asString())) return null
                if (!constructorCall.arguments[1].isConstInt(enumEntriesMap.getValue(enumEntry.name).ordinal)) return null
                StaticPlainEnumEntry(enumEntry, constructorCall)
            }
        }

        private fun IrConstructor.isLoweredSimpleEnumConstructor(): Boolean {
            if (parameters.size != 2) return false
            val delegatingCall = body?.statements?.singleOrNull() as? IrDelegatingConstructorCall ?: return false
            if (delegatingCall.symbol.owner.constructedClass != context.irBuiltIns.enumClass.owner) return false
            return delegatingCall.arguments[0].isGetOf(parameters[0]) &&
                    delegatingCall.arguments[1].isGetOf(parameters[1])
        }

        private fun IrExpression?.isGetOf(parameter: IrValueParameter): Boolean =
                this is IrGetValue && symbol == parameter.symbol

        private fun IrExpression?.isConstString(value: String): Boolean =
                this is IrConst && kind == IrConstKind.String && this.value == value

        private fun IrExpression?.isConstInt(value: Int): Boolean =
                this is IrConst && kind == IrConstKind.Int && this.value == value

        private fun IrEnumEntry.loweredConstructorCallOrNull(): IrConstructorCall? {
            val initializer = initializerExpression?.expression ?: return null
            return when (initializer) {
                is IrConstructorCall -> initializer
                is IrBlock -> if (initializer.origin == ARGUMENTS_REORDERING_FOR_CALL) {
                    initializer.statements.lastOrNull() as? IrConstructorCall
                } else {
                    null
                }
                else -> null
            }
        }

        private fun IrEnumEntry.loweredConstructorCall(): IrConstructorCall =
                loweredConstructorCallOrNull() ?: error("Unexpected initializer: ${initializerExpression?.expression}")

        private fun defineValueGetter() {
            val valueGetter = enumsSupport.getValueGetter(irClass)
            context.createIrBuilder(valueGetter.symbol).run {
                valueGetter.body = irBlockBody(valueGetter) {
                    +irReturn(irCall(arrayGet, irClass.defaultType).apply {
                        dispatchReceiver = irGetField(null, valuesField)
                        arguments[1] = irGet(valueGetter.parameters[0])
                    })
                }
            }
            irClass.declarations.add(valueGetter)
        }

        private fun IrBlockBuilder.irInitInstanceCall(instance: IrCall, constructor: IrConstructorCall): IrCall =
                irCall(initInstance).apply {
                    arguments[0] = instance
                    arguments[1] = constructor
                }

        private fun defineStaticPlainValuesField(enumEntries: List<StaticPlainEnumEntry>) {
            irClass.declarations.add(0, valuesField)
            valuesField.parent = irClass
            val irBuilder = context.createIrBuilder(valuesField.symbol, irClass.startOffset, irClass.endOffset)

            valuesField.initializer = irBuilder.irExprBody(
                    irBuilder.irConstantArray(
                            valuesField.type,
                            enumEntries
                                    .sortedBy { it.enumEntry.name }
                                    .map { it.toStaticConstantObject(irBuilder) }
                    )
            ).also {
                it.setDeclarationsParent(valuesField)
            }
        }

        private fun StaticPlainEnumEntry.toStaticConstantObject(irBuilder: IrBuilderWithScope): IrConstantValue {
            val constructorArguments = constructorCall.arguments.mapIndexed { index, argument ->
                argument.toConstantValue(irBuilder)
                        ?: error("Unexpected non-constant enum constructor argument #$index: $argument")
            }
            return irBuilder.irConstantObject(
                    constructorCall.symbol,
                    constructorArguments,
                    constructorCall.typeArguments.map { it!! }
            )
        }

        private fun IrExpression?.toConstantValue(irBuilder: IrBuilderWithScope): IrConstantValue? = when (this) {
            is IrConstantValue -> this
            // The same enum entry can appear in both $VALUES and $ENTRIES constant graphs, so do not reuse IR nodes.
            is IrConst -> irBuilder.irConstantPrimitive(copyForConstantGraph(irBuilder))
            else -> null
        }

        private fun IrConst.copyForConstantGraph(irBuilder: IrBuilderWithScope): IrConst = when (kind) {
            IrConstKind.String -> irBuilder.irString(value as String)
            IrConstKind.Int -> irBuilder.irInt(value as Int)
            else -> error("Unexpected enum constructor constant kind: $kind")
        }

        private fun defineStaticPlainEntriesField(enumEntries: List<StaticPlainEnumEntry>) {
            irClass.declarations.add(0, entriesField)
            entriesField.parent = irClass
            val irBuilder = context.createIrBuilder(entriesField.symbol, irClass.startOffset, irClass.endOffset)
            // EnumEntries keeps declaration order, unlike the name-sorted $VALUES array used by valueOf().
            val entriesArray = irBuilder.irConstantArray(
                    valuesField.type,
                    enumEntries
                            .sortedBy { enumEntriesMap.getValue(it.enumEntry.name).ordinal }
                            .map { it.toStaticConstantObject(irBuilder) }
            )

            entriesField.initializer = irBuilder.irExprBody(
                    irBuilder.irConstantObject(
                            enumEntriesListConstructor,
                            listOf(entriesArray),
                            listOf(irClass.defaultType),
                    )
            ).also {
                it.setDeclarationsParent(entriesField)
            }
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
                                    val entryConstructorCall = it.loweredConstructorCall()
                                    val entryClass = entryConstructorCall.symbol.owner.constructedClass

                                    irCallWithSubstitutedType(createUninitializedInstance, listOf(entryClass.defaultType))
                                }
                )
                val instances = irTemporary(irValuesInitializer)
                +irSetField(null, valuesField, irGet(instances), origin = ObjectClassLowering.IrStatementOriginFieldPreInit)
                callEnumEntriesConstructors(instances, enumEntries)
                +irGet(instances)
            }).also {
                it.setDeclarationsParent(valuesField)
            }
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
                                        arguments[1] = irInt(it.getterId)
                                    }
                                }
                )
                +irCallWithSubstitutedType(createEnumEntries, listOf(irClass.defaultType)).apply {
                    arguments[0] = irEntriesArray
                }
            })
        }

        private fun IrBlockBuilder.callEnumEntriesConstructors(instances: IrVariable, enumEntries: List<IrEnumEntry>) {
            enumEntries.forEach {
                val instance = irCall(arrayGet).apply {
                    dispatchReceiver = irGet(instances)
                    arguments[1] = irInt(enumEntriesMap[it.name]!!.getterId)
                }
                when (val initializer = it.initializerExpression!!.expression) {
                    is IrConstructorCall -> +irInitInstanceCall(instance, initializer)

                    is IrBlock -> {
                        if (initializer.origin != ARGUMENTS_REORDERING_FOR_CALL) {
                            error("Unexpected initializer: $initializer")
                        }
                        val statements = initializer.statements
                        val constructorCall = statements.last() as IrConstructorCall
                        statements[statements.lastIndex] = irInitInstanceCall(instance, constructorCall)
                        initializer.type = context.irBuiltIns.unitType
                        +initializer
                    }

                    else -> error("Unexpected initializer: $initializer")
                }
            }
        }
    }
}
