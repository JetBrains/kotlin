/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.konan.descriptors.getArgumentValueOrNull
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

private val cEnumEntryAliasAnnonation = FqName("kotlinx.cinterop.internal.CEnumEntryAlias")

internal class CEnumCompanionGenerator(
        context: GeneratorContext,
        private val cEnumByValueFunctionGenerator: CEnumByValueFunctionGenerator
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator

    // Depends on already generated `.values()` irFunction.
    fun generate(enumClass: IrClass): IrClass =
            createClass(enumClass.descriptor.companionObjectDescriptor!!) { companionIrClass ->
                companionIrClass.superTypes += irBuiltIns.anyType
                companionIrClass.addMember(createCompanionConstructor(companionIrClass.descriptor))
                val valuesFunction = enumClass.functions.single { it.name.identifier == "values" }.symbol
                val byValueIrFunction = cEnumByValueFunctionGenerator
                        .generateByValueFunction(companionIrClass, valuesFunction)
                companionIrClass.addMember(byValueIrFunction)
                findEntryAliases(companionIrClass.descriptor)
                        .map { declareEntryAliasProperty(it, enumClass) }
                        .forEach(companionIrClass::addMember)
            }

    private fun createCompanionConstructor(companionObjectDescriptor: ClassDescriptor): IrConstructor {
        val anyPrimaryConstructor = companionObjectDescriptor.builtIns.any.unsubstitutedPrimaryConstructor!!
        val superConstructorSymbol = symbolTable.referenceConstructor(anyPrimaryConstructor)
        return createConstructor(companionObjectDescriptor.unsubstitutedPrimaryConstructor!!).also {
            it.body = irBuilder(irBuiltIns, it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                        startOffset, endOffset, context.irBuiltIns.unitType,
                        superConstructorSymbol
                )
                +irInstanceInitializer(symbolTable.referenceClass(companionObjectDescriptor))
            }
        }
    }

    /**
     * Returns all properties in companion object that represent aliases to
     * enum entries.
     */
    private fun findEntryAliases(companionDescriptor: ClassDescriptor) =
            companionDescriptor.defaultType.memberScope.getContributedDescriptors()
                    .filterIsInstance<PropertyDescriptor>()
                    .filter { it.annotations.hasAnnotation(cEnumEntryAliasAnnonation) }

    private fun fundCorrespondingEnumEntrySymbol(aliasDescriptor: PropertyDescriptor, irClass: IrClass): IrEnumEntrySymbol {
        val enumEntryName = aliasDescriptor.annotations
                .findAnnotation(cEnumEntryAliasAnnonation)!!
                .getArgumentValueOrNull<String>("entryName")
        return irClass.declarations.filterIsInstance<IrEnumEntry>()
                .single { it.name.identifier == enumEntryName }.symbol
    }

    private fun generateAliasGetterBody(getter: IrSimpleFunction, entrySymbol: IrEnumEntrySymbol): IrBody =
            irBuilder(irBuiltIns, getter.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +irReturn(
                        IrGetEnumValueImpl(startOffset, endOffset, entrySymbol.owner.parentAsClass.defaultType, entrySymbol)
                )
            }

    private fun declareEntryAliasProperty(propertyDescriptor: PropertyDescriptor, enumClass: IrClass): IrProperty {
        val entrySymbol = fundCorrespondingEnumEntrySymbol(propertyDescriptor, enumClass)
        return createProperty(propertyDescriptor).also {
            it.getter!!.body = generateAliasGetterBody(it.getter!!, entrySymbol)
        }
    }
}