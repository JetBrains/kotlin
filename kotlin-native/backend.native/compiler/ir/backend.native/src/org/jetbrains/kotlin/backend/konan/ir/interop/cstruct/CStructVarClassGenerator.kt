/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cstruct

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

internal class CStructVarClassGenerator(
        context: GeneratorContext,
        private val interopBuiltIns: InteropBuiltIns
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator

    private val companionGenerator = CStructVarCompanionGenerator(context, interopBuiltIns)

    fun findOrGenerateCStruct(classDescriptor: ClassDescriptor, parent: IrDeclarationContainer): IrClass {
        val irClassSymbol = symbolTable.referenceClass(classDescriptor)
        return if (!irClassSymbol.isBound) {
            provideIrClassForCStruct(classDescriptor).also {
                it.patchDeclarationParents(parent)
                parent.declarations += it
            }
        } else {
            irClassSymbol.owner
        }
    }

    private fun provideIrClassForCStruct(descriptor: ClassDescriptor): IrClass =
            createClass(descriptor) { irClass ->
                irClass.addMember(createPrimaryConstructor(irClass))
                irClass.addMember(companionGenerator.generate(descriptor))
                descriptor.unsubstitutedMemberScope
                        .getContributedDescriptors()
                        .filterIsInstance<PropertyDescriptor>()
                        .filter { it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
                        .map(this::createProperty)
                        .forEach(irClass::addMember)
            }

    private fun createPrimaryConstructor(irClass: IrClass): IrConstructor {
        val enumVarConstructorSymbol = symbolTable.referenceConstructor(
                interopBuiltIns.cStructVar.unsubstitutedPrimaryConstructor!!
        )
        return createConstructor(irClass.descriptor.unsubstitutedPrimaryConstructor!!).also { irConstructor ->
            irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType, enumVarConstructorSymbol,
                        irConstructor.typeParameters.size,
                        1
                ).also {
                    it.putValueArgument(0, irGet(irConstructor.valueParameters[0]))
                }
                +irInstanceInitializer(symbolTable.referenceClass(irClass.descriptor))
            }
        }
    }
}