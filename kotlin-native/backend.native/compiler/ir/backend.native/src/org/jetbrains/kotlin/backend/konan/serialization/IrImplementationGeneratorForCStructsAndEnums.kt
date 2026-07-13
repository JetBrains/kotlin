/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.backend.konan.lower.EnumClassLowering
import org.jetbrains.kotlin.backend.konan.lower.EnumConstructorsLowering
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val varTypeAnnotationFqName = FqName("kotlinx.cinterop.internal.CStruct.VarType")
private val cEnumEntryAliasAnnotation = FqName("kotlinx.cinterop.internal.CEnumEntryAlias")
private val cEnumEntryValueAnnotationName = FqName("kotlinx.cinterop.internal.ConstantValue")
private val typeSizeAnnotation = FqName("kotlinx.cinterop.internal.CEnumVarTypeSize")
private val cEnumEntryValueTypes = setOf(
        "Byte", "Short", "Int", "Long",
        "UByte", "UShort", "UInt", "ULong"
)

/**
 * Creates implementation (mostly IR bodies) for C structs and C enums deserialized from C-interop Klib.
 * Unlike other declarations from C/Objective-C, for which only linkable stubs are generated, C structs and C enums
 * are treated much like "real" Kotlin classes and enums. They participate in lowerings and follow a similar compilation scheme.
 * This allows reusing e.g. [EnumClassLowering], [EnumConstructorsLowering] and other compiler phases.
 */
internal class IrImplementationGeneratorForCStructsAndEnums(
        private val builtIns: IrBuiltIns,
        private val symbols: BackendNativeSymbols,
) {
    fun generateImplIfCStructOrEnum(clazz: IrClass) {
        if (clazz.inheritsFromCStruct()) {
            generateImplementationForCStruct(clazz)
        }
        if (clazz.inheritsFromCEnum()) {
            generateImplementationForCEnum(clazz)
        }
    }

    private fun generateImplementationForCStruct(structClass: IrClass) {
        createBodyForCStructConstructors(structClass)
        createBodyForCStructCompanionConstructor(structClass.companionObject()!!)
    }

    private fun createBodyForCStructConstructors(structClass: IrClass) {
        for (constructor in structClass.constructors) {
            constructor.body = builtIns.createIrBuilder(constructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                if (constructor.isPrimary) {
                    +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                            startOffset, endOffset,
                            context.irBuiltIns.unitType, symbols.cStructVarConstructorSymbol
                    ).also {
                        it.arguments[0] = irGet(constructor.parameters[0])
                    }
                    +irInstanceInitializer(structClass.symbol)
                } else {
                    // Empty. The real body is constructed at the call site by the interop lowering phase.
                }
            }
        }
    }

    private fun createBodyForCStructCompanionConstructor(cStructCompanionClass: IrClass) {
        val size = cStructCompanionClass.getAnnotationArgumentValue<Long>(varTypeAnnotationFqName, "size")!!
        val align = cStructCompanionClass.getAnnotationArgumentValue<Int>(varTypeAnnotationFqName, "align")!!

        val primaryConstructor = cStructCompanionClass.primaryConstructor!!
        primaryConstructor.body = builtIns.createIrBuilder(primaryConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset, context.irBuiltIns.unitType,
                    symbols.structVarTypePrimaryConstructor
            ).also {
                it.arguments[0] = irLong(size)
                it.arguments[1] = irInt(align)
            }
            +irInstanceInitializer(cStructCompanionClass.symbol)
        }
    }


    private fun generateImplementationForCEnum(enumClass: IrClass) {
        createBodyForEnumPrimaryConstructor(enumClass)
        createImplForEnumValueProperty(enumClass)
        createBodyForEnumEntries(enumClass)

        val companionObjectClass = enumClass.companionObject()!!
        createBodyForEnumCompanionConstructor(companionObjectClass)
        createBodyForEnumEntryAliases(enumClass, companionObjectClass)
        createBodyForEnumCompanionByValueFunction(enumClass, companionObjectClass)

        val varClass = enumClass.nestedClasses.single { it.name.asString() == "Var" }
        createBodyForEnumVarClassConstructor(varClass)
        createBodyForEnumVarClassCompanionConstructor(varClass)
    }

    private fun createBodyForEnumPrimaryConstructor(enumClass: IrClass) {
        val primaryConstructor = enumClass.primaryConstructor!!
        val enumConstructor = builtIns.enumClass.owner.constructors.single().symbol
        primaryConstructor.body = builtIns.createIrBuilder(primaryConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                .irBlockBody {
                    +IrEnumConstructorCallImpl(
                            startOffset, endOffset,
                            builtIns.unitType,
                            enumConstructor,
                            typeArgumentsCount = 1, // kotlin.Enum<T> has a single type parameter.
                    ).apply {
                        typeArguments[0] = enumClass.defaultType
                    }
                    +irInstanceInitializer(enumClass.symbol)
                }
    }

    /**
     * Creates `value` property that stores integral value of the enum.
     */
    private fun createImplForEnumValueProperty(enumClass: IrClass) {
        val irProperty = enumClass.properties.firstOrNull { it.name.asString() == "value" }
                ?: error("No `value` property in ${enumClass.name}")
        val getter = irProperty.getter!!

        val backingField = IrFactoryImpl.createField(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                irProperty.name, DescriptorVisibilities.PRIVATE, IrFieldSymbolImpl(), getter.returnType,
                isFinal = true, isStatic = false, isExternal = false
        )
        backingField.initializer = builtIns.createIrBuilder(backingField.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
            irExprBody(irGet(enumClass.primaryConstructor!!.parameters[0]))
        }
        irProperty.backingField = backingField
        backingField.parent = enumClass

        getter.correspondingPropertySymbol = irProperty.symbol
        getter.body = builtIns.createIrBuilder(getter.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +irReturn(
                    irGetField(
                            irGet(getter.dispatchReceiverParameter!!),
                            irProperty.backingField!!
                    )
            )
        }
    }

    private fun createBodyForEnumEntries(enumClass: IrClass) {
        val constructorSymbol = enumClass.primaryConstructor!!.symbol
        for (enumEntry in enumClass.declarations.filterIsInstance<IrEnumEntry>()) {
            // Every enum entry that came from metadata-based interop library is annotated with one of
            // [kotlinx.cinterop.internal.ConstantValue.*] annotations that holds internal constant value of the
            // corresponding entry.
            val value = cEnumEntryValueTypes.firstNotNullOfOrNull {
                enumEntry.getAnnotation(cEnumEntryValueAnnotationName.child(Name.identifier(it)))?.argumentMapping[Name.identifier("value")]
            } ?: error("Enum entry ${enumEntry.fqNameWhenAvailable} has no appropriate @$cEnumEntryValueAnnotationName annotation!")

            enumEntry.initializerExpression = IrFactoryImpl.createExpressionBody(
                    IrEnumConstructorCallImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            type = builtIns.unitType,
                            symbol = constructorSymbol,
                            typeArgumentsCount = 0,
                    ).also {
                        it.arguments[0] = value
                    },
            )
        }
    }

    private fun createBodyForEnumCompanionConstructor(enumCompanionClass: IrClass) {
        val primaryConstructor = enumCompanionClass.primaryConstructor!!
        val anyPrimaryConstructor = builtIns.anyClass.owner.primaryConstructor!!
        primaryConstructor.body = builtIns.createIrBuilder(primaryConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset, builtIns.unitType,
                    anyPrimaryConstructor.symbol
            )
            +irInstanceInitializer(enumCompanionClass.symbol)
        }
    }

    private fun createBodyForEnumCompanionByValueFunction(enumClass: IrClass, enumCompanionClass: IrClass) {
        val enumValuesFun = enumClass.getSimpleFunction(StandardNames.ENUM_VALUES.asString())!!

        val byValueFun = enumCompanionClass.getSimpleFunction("byValue")!!.owner
        val valueParameter = byValueFun.parameters[1]
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
        byValueFun.body = builtIns.createIrBuilder(byValueFun.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +irReturn(irBlock {
                val values = irTemporary(irCall(enumValuesFun), isMutable = true)
                val inductionVariable = irTemporary(irInt(0), isMutable = true)
                val arrayClass = values.type.classOrNull!!
                val valuesSize = irCall(symbols.arraySize.getValue(arrayClass), builtIns.intType).also { irCall ->
                    irCall.dispatchReceiver = irGet(values)
                }
                val getElementFn = symbols.arrayGet.getValue(arrayClass)
                val plusFun = builtIns.intPlusSymbol
                val lessFunctionSymbol = builtIns.lessFunByOperandType.getValue(builtIns.intClass)
                +irWhile().also { loop ->
                    loop.condition = irCall(lessFunctionSymbol, builtIns.booleanType).also { irCall ->
                        irCall.arguments[0] = irGet(inductionVariable)
                        irCall.arguments[1] = valuesSize
                    }
                    loop.body = irBlock {
                        val entry = irTemporary(irCall(getElementFn, byValueFun.returnType).also { irCall ->
                            irCall.arguments[0] = irGet(values)
                            irCall.arguments[1] = irGet(inductionVariable)
                        }, isMutable = true)
                        val valueGetter = entry.type.getClass()!!.getPropertyGetter("value")!!
                        val entryValue = irGet(valueParameter.type, irGet(entry), valueGetter)
                        +irIfThenElse(
                                type = builtIns.unitType,
                                condition = irEquals(entryValue, irGet(valueParameter)),
                                thenPart = irReturn(irGet(entry)),
                                elsePart = irSet(
                                        inductionVariable,
                                        irCallOp(plusFun, builtIns.intType,
                                                irGet(inductionVariable),
                                                irInt(1)
                                        )
                                )
                        )
                    }
                }
                +IrCallImpl.fromSymbolOwner(startOffset, endOffset, builtIns.nothingType,
                        symbols.throwNullPointerException)
            })
        }
    }

    private fun createBodyForEnumEntryAliases(enumClass: IrClass, enumCompanionClass: IrClass) {
        for (aliasProperty in enumCompanionClass.properties) {
            val enumEntryName = aliasProperty.getAnnotationArgumentValue<String>(cEnumEntryAliasAnnotation, "entryName") ?: continue
            val correspondingEntry = enumClass.declarations.filterIsInstance<IrEnumEntry>().single { it.name.identifier == enumEntryName }

            val getter = aliasProperty.getter!!
            getter.body = builtIns.createIrBuilder(getter.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +irReturn(IrGetEnumValueImpl(startOffset, endOffset, enumClass.defaultType, correspondingEntry.symbol))
            }
        }
    }

    private fun createBodyForEnumVarClassConstructor(enumVarClass: IrClass): IrConstructor {
        val primaryConstructor = enumVarClass.primaryConstructor!!
        primaryConstructor.body = builtIns.createIrBuilder(primaryConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset, builtIns.unitType, symbols.enumVarConstructorSymbol
            ).also {
                it.arguments[0] = irGet(primaryConstructor.parameters[0])
            }
            +irInstanceInitializer(enumVarClass.symbol)
        }
        return primaryConstructor
    }

    private fun createBodyForEnumVarClassCompanionConstructor(enumVarClass: IrClass) {
        val companionClass = enumVarClass.companionObject()!!
        val typeSize = companionClass.getAnnotationArgumentValue<Int>(typeSizeAnnotation, "size")!!
        val companionClassConstructor = companionClass.primaryConstructor!!
        companionClassConstructor.body = builtIns.createIrBuilder(companionClassConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset, builtIns.unitType,
                    symbols.primitiveVarTypePrimaryConstructor
            ).also {
                it.arguments[0] = irInt(typeSize)
            }
            +irInstanceInitializer(companionClass.symbol)
        }
    }
}

private fun IrBuilder.irInstanceInitializer(classSymbol: IrClassSymbol): IrExpression =
        IrInstanceInitializerCallImpl(startOffset, endOffset, classSymbol, context.irBuiltIns.unitType)
