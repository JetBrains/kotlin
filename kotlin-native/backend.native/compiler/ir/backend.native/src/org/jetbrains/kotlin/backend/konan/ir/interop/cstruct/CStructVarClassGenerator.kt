/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cstruct

import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
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
                if (!descriptor.annotations.hasAnnotation(RuntimeNames.managedType)) { // TODO: we need a companion here, but later
                    irClass.addMember(companionGenerator.generate(descriptor))
                }
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
            }.also { irClass ->
                if (irClass.descriptor.annotations.hasAnnotation(RuntimeNames.cppClass)) {
                    setupCppClass(irClass)
                }
                if (irClass.descriptor.annotations.hasAnnotation(RuntimeNames.managedType)) {
                    setupManagedClass(irClass)
                }
            }

    private fun setupCppClass(irClass: IrClass) {
        if (irClass.descriptor.annotations.hasAnnotation(RuntimeNames.cppClass)) {
            val companionDestroy = irClass.companionObject()!!.declarations
                    .filterIsInstance<IrSimpleFunction>()
                    .filter { it.name.toString() == "__destroy__" }
                    .singleOrNull() ?: return

            val destroy = irClass.declarations
                    .filterIsInstance<IrSimpleFunction>()
                    .filter { it.name.toString() == "__destroy__" }
                    .single()

            val rawPtr = irClass.declarations
                    .filterIsInstance<IrProperty>()
                    .filter { it.name.toString() == "rawPtr" }
                    .single()

            destroy.body = irBuilder(irBuiltIns, destroy.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                    .irBlockBody {
                        +irCall(companionDestroy).apply {
                            dispatchReceiver = irGetObject(irClass.companionObject()!!.symbol)
                            putValueArgument(0,
                                    irCall(rawPtr.getter!!).apply {
                                        dispatchReceiver = irGet(destroy.dispatchReceiverParameter!!)
                                    }
                            )
                        }
                    }
        }
    }

    private fun setupManagedClass(irClass: IrClass) {
        val cppVal = irClass.declarations
                .filterIsInstance<IrProperty>()
                .filter { it.name.toString() == "cpp" }
                .single()

        cppVal.backingField = IrFieldImpl(
                cppVal.startOffset,
                cppVal.endOffset,
                cppVal.origin,
                symbolTable.referenceField(cppVal.descriptor),
                cppVal.name,
                irClass.primaryConstructor!!.valueParameters.single().type,
                cppVal.visibility,
                isFinal = true,
                isExternal = false,
                isStatic = false
        ).also {
            it.initializer = irBuilder(irBuiltIns, it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                irExprBody(irGet(irClass.primaryConstructor!!.valueParameters.single()))
            }
        }

        cppVal.getter!!.body = irBuilder(irBuiltIns, cppVal.getter!!.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                .irBlockBody {
                    +irReturn(irGetField(irGet(cppVal.getter!!.dispatchReceiverParameter!!), cppVal.backingField!!))
                }
        println("GENERATED IR FOR ${irClass.name}")
        println(ir2stringWhole(irClass))
    }

    private fun createPrimaryConstructor(irClass: IrClass): IrConstructor {
        if (!irClass.descriptor.annotations.hasAnnotation(RuntimeNames.managedType)) {
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
        } else {
            return createConstructor(irClass.descriptor.unsubstitutedPrimaryConstructor!!).also { irConstructor ->
                val managedTypeConstructor = symbolTable.referenceConstructor(
                        interopBuiltIns.managedType.unsubstitutedPrimaryConstructor!!
                )
                postLinkageSteps.add {
                    irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                        +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                                startOffset, endOffset,
                                context.irBuiltIns.unitType, managedTypeConstructor
                        ).also {
                                it.putValueArgument(0, irGet(irConstructor.valueParameters[0]))
                        }
                        +irInstanceInitializer(symbolTable.referenceClass(irClass.descriptor))
                    }
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