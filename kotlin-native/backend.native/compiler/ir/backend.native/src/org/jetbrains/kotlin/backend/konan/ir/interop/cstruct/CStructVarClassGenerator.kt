/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cstruct

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

internal class CStructVarClassGenerator(
        context: GeneratorContext,
        private val interopBuiltIns: InteropBuiltIns,
        private val companionGenerator: CStructVarCompanionGenerator,
        private val symbols: KonanSymbols
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator
    override val postLinkageSteps: MutableList<() -> Unit> = mutableListOf()

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
                descriptor.constructors
                    .filterNot { it.isPrimary }
                    .map {
                        val constructor = createSecondaryConstructor(it)
                        irClass.addMember(constructor)
                    }
                descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filterNot { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
                    .map {
                        when (it) {
                            is PropertyDescriptor -> createProperty(it)
                            is SimpleFunctionDescriptor -> createFunction(it)
                            else -> null
                        }
                    }
                    .filterNotNull()
                    .forEach(irClass::addMember)
            }

    private fun createPrimaryConstructor(irClass: IrClass): IrConstructor {
        val enumVarConstructorSymbol = symbolTable.referenceConstructor(
                interopBuiltIns.cStructVar.unsubstitutedPrimaryConstructor!!
        )
        return createConstructor(irClass.descriptor.unsubstitutedPrimaryConstructor!!).also { irConstructor ->
            postLinkageSteps.add {
                irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                    +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                            startOffset, endOffset,
                            context.irBuiltIns.unitType, enumVarConstructorSymbol
                    ).also {
                        it.putValueArgument(0, irGet(irConstructor.valueParameters[0]))
                    }
                    +irInstanceInitializer(symbolTable.referenceClass(irClass.descriptor))
                }
            }
        }
    }

    private fun createSecondaryConstructor(descriptor: ClassConstructorDescriptor): IrConstructor {
        return createConstructor(descriptor).also {
            postLinkageSteps.add {
                it.body = irBuilder(irBuiltIns, it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                    // Empty. The real body is constructed at the call site by the interop lowering phase.
                }
            }
        }
    }
}