/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.konan.NativeMapping
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

internal class EnumsSupport(
        mapping: NativeMapping,
        symbols: KonanSymbols,
        private val irBuiltIns: IrBuiltIns,
        private val irFactory: IrFactory,
) {
    private val enumImplObjects = mapping.enumImplObjects
    private val enumValueGetters = mapping.enumValueGetters
    private val enumEntriesMaps = mapping.enumEntriesMaps
    private val array = symbols.array
    private val genericValueOfSymbol = symbols.valueOfForEnum
    private val genericValuesSymbol = symbols.valuesForEnum

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

    fun getImplObject(enumClass: IrClass): IrClass {
        require(enumClass.isEnumClass) { "Expected enum class but was: ${enumClass.render()}" }
        return enumImplObjects.getOrPut(enumClass) {
            irFactory.buildClass {
                startOffset = enumClass.startOffset
                endOffset = enumClass.endOffset
                origin = DECLARATION_ORIGIN_ENUM
                name = "OBJECT".synthesizedName
                kind = ClassKind.OBJECT
            }.apply {
                superTypes = listOf(irBuiltIns.anyType)
                parent = enumClass

                addChild(irFactory.buildField {
                    startOffset = enumClass.startOffset
                    endOffset = enumClass.endOffset
                    origin = DECLARATION_ORIGIN_ENUM
                    name = "VALUES".synthesizedName
                    type = array.typeWith(enumClass.defaultType)
                    visibility = DescriptorVisibilities.PRIVATE
                    isFinal = true
                })
            }
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

    fun getValuesField(implObject: IrClass) = implObject.fields.single()

    fun IrBuilderWithScope.irGetValuesField(enumClass: IrClass): IrExpression {
        val implObject = getImplObject(enumClass)
        val valuesField = getValuesField(implObject)
        return irGetField(irGetObject(implObject.symbol), valuesField)
    }

    fun IrBuilderWithScope.irEnumValues(enumClass: IrClass) =
            irCall(genericValuesSymbol, listOf(enumClass.defaultType)).apply {
                putValueArgument(0, irGetValuesField(enumClass))
            }

    fun IrBuilderWithScope.irEnumValueOf(enumClass: IrClass, value: IrExpression) =
            irCall(genericValueOfSymbol, listOf(enumClass.defaultType)).apply {
                putValueArgument(0, value)
                putValueArgument(1, irGetValuesField(enumClass))
            }
}

internal object DECLARATION_ORIGIN_ENUM : IrDeclarationOriginImpl("ENUM")

internal data class LoweredEnumEntryDescription(val ordinal: Int, val getterId: Int)
