/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.annotations.getArgumentValueOrNull

private val typeSizeAnnotation = FqName("kotlinx.cinterop.internal.CEnumVarTypeSize")

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class CEnumVarClassGenerator(
        context: GeneratorContext,
        private val symbols: KonanSymbols
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator
    override val postLinkageSteps: MutableList<() -> Unit> = mutableListOf()

    fun generate(enumIrClass: IrClass): IrClass {
        val enumVarClassDescriptor = enumIrClass.descriptor.unsubstitutedMemberScope
                .getContributedClassifier(Name.identifier("Var"), NoLookupLocation.FROM_BACKEND)!! as ClassDescriptor
        return createClass(enumVarClassDescriptor) { enumVarClass ->
            enumVarClass.addMember(createPrimaryConstructor(enumVarClass))
            enumVarClass.addMember(createCompanionObject(enumVarClass))
            enumVarClass.addMember(createValueProperty(enumVarClass))
        }
    }

    private fun createValueProperty(enumVarClass: IrClass): IrProperty {
        val valuePropertyDescriptor = enumVarClass.descriptor.unsubstitutedMemberScope
                .getContributedVariables(Name.identifier("value"), NoLookupLocation.FROM_BACKEND).single()
        return createProperty(valuePropertyDescriptor)
    }

    private fun createPrimaryConstructor(enumVarClass: IrClass): IrConstructor {
        val irConstructor = createConstructor(enumVarClass.descriptor.unsubstitutedPrimaryConstructor!!)
        val classSymbol = symbolTable.descriptorExtension.referenceClass(enumVarClass.descriptor)
        postLinkageSteps.add {
            irConstructor.body = irBuiltIns.createIrBuilder(irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset, context.irBuiltIns.unitType, symbols.enumVarConstructorSymbol
                ).also {
                    it.putValueArgument(0, irGet(irConstructor.valueParameters[0]))
                }
                +irInstanceInitializer(classSymbol)
            }
        }
        return irConstructor
    }

    private fun createCompanionObject(enumVarClass: IrClass): IrClass =
            createClass(enumVarClass.descriptor.companionObjectDescriptor!!) { companionIrClass ->
                val typeSize = companionIrClass.descriptor.annotations
                        .findAnnotation(typeSizeAnnotation)!!
                        .getArgumentValueOrNull<Int>("size")!!
                companionIrClass.addMember(createCompanionConstructor(companionIrClass.descriptor, typeSize))
            }

    private fun createCompanionConstructor(companionObjectDescriptor: ClassDescriptor, typeSize: Int): IrConstructor {
        val classSymbol = symbolTable.descriptorExtension.referenceClass(companionObjectDescriptor)
        return createConstructor(companionObjectDescriptor.unsubstitutedPrimaryConstructor!!).also {
            postLinkageSteps.add {
                it.body = irBuiltIns.createIrBuilder(it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                    +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                        startOffset, endOffset, context.irBuiltIns.unitType,
                        symbols.primitiveVarPrimaryConstructor
                    ).also {
                        it.putValueArgument(0, irInt(typeSize))
                    }
                    +irInstanceInitializer(classSymbol)
                }
            }
        }
    }
}
