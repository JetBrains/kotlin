/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cstruct

import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.ptr
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
        private val konanSymbols: KonanSymbols
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
                        val constructor = createSecondaryConstructor(irClass, it)
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
                    .filterIsInstance<IrDeclaration>()
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

    private fun createSecondaryConstructor(irClass: IrClass, descriptor: ClassConstructorDescriptor): IrConstructor {
        val primaryConstructor = irClass.primaryConstructor!!.symbol

        val alloc = konanSymbols.interopAllocType

        val nativeheap = symbolTable.referenceClass(
            interopBuiltIns.nativeHeap
        )

        val interopGetPtr = symbolTable.referenceSimpleFunction(
            interopBuiltIns.interopGetPtr
        )

        val irConstructor = createConstructor(descriptor)

        val correspondingInit = irClass.companionObject()!!
            .declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.name.toString() == "__init__"}
            .filter { it.valueParameters.size == irConstructor.valueParameters.size + 1}
            .single {
                it.valueParameters.drop(1).mapIndexed() { index, initParameter ->
                    initParameter.type == irConstructor.valueParameters[index].type
                }.all{ it }
            }

        postLinkageSteps.add {
            irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset,
                    context.irBuiltIns.unitType, primaryConstructor
                ).also {
                    val nativePointed = irCall(alloc).apply {
                        extensionReceiver = irGetObject(nativeheap)
                        putValueArgument(0, irGetObject(irClass.companionObject()!!.symbol))
                    }
                    val nativePtr = irCall(konanSymbols.interopNativePointedGetRawPointer).apply {
                        extensionReceiver = nativePointed
                    }
                    it.putValueArgument(0, nativePtr)
                }

                +irCall(correspondingInit.symbol).apply {
                    dispatchReceiver = irGetObject(irClass.companionObject()!!.symbol)
                    putValueArgument(0,
                        irCall(interopGetPtr).apply {
                            extensionReceiver = irGet(irClass.thisReceiver!!)
                        }
                    )
                    irConstructor.valueParameters.forEachIndexed { index, it ->
                        putValueArgument(index+1, irGet(it))
                    }
                }
            }
        }
        return irConstructor
    }
}