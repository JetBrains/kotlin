/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.descriptors.getArgumentValueOrNull
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

private val typeSizeAnnotation = FqName("kotlinx.cinterop.internal.CEnumVarTypeSize")

internal class CEnumVarClassGenerator(
        context: GeneratorContext,
        private val interopBuiltIns: InteropBuiltIns
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
        val enumVarConstructorSymbol = symbolTable.referenceConstructor(
                interopBuiltIns.cEnumVar.unsubstitutedPrimaryConstructor!!
        )
        val classSymbol = symbolTable.referenceClass(enumVarClass.descriptor)
        postLinkageSteps.add {
            irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                        startOffset, endOffset, context.irBuiltIns.unitType, enumVarConstructorSymbol
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
        val superConstructorSymbol = symbolTable.referenceConstructor(interopBuiltIns.cPrimitiveVarType.unsubstitutedPrimaryConstructor!!)
        val classSymbol = symbolTable.referenceClass(companionObjectDescriptor)
        return createConstructor(companionObjectDescriptor.unsubstitutedPrimaryConstructor!!).also {
            postLinkageSteps.add {
                it.body = irBuilder(irBuiltIns, it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                    +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                            startOffset, endOffset, context.irBuiltIns.unitType,
                            superConstructorSymbol
                    ).also {
                        it.putValueArgument(0, irInt(typeSize))
                    }
                    +irInstanceInitializer(classSymbol)
                }
            }
        }
    }
}
