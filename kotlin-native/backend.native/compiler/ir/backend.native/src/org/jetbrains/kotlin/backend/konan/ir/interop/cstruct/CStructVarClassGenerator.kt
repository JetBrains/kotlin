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
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
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
                    postLinkageSteps.add {
                        setupCppClass(irClass)
                    }
                }
                if (irClass.descriptor.annotations.hasAnnotation(RuntimeNames.managedType)) {
                    postLinkageSteps.add {
                        setupManagedClass(irClass)
                    }
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

            //val rawPtr = irClass.declarations
            //        .filterIsInstance<IrProperty>()
            //        .filter { it.name.toString() == "rawPtr" }
            //        .single()

            val getPtr = symbols.interopGetPtr

            destroy.body = irBuilder(irBuiltIns, destroy.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                    .irBlockBody {
                        +irCall(companionDestroy).apply {
                            dispatchReceiver = irGetObject(irClass.companionObject()!!.symbol)
                            putValueArgument(0,
                                    irCall(getPtr).apply {
                                        extensionReceiver = irGet(destroy.dispatchReceiverParameter!!)
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

        val cppValType = cppVal.getter!!.returnType

        cppVal.backingField = symbolTable.declareField(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                cppVal.descriptor,
                cppValType,
                DescriptorVisibilities.PRIVATE
        ).also {
            it.parent = irClass
            it.initializer = irBuilder(irBuiltIns, it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                irExprBody(irGet(irClass.primaryConstructor!!.valueParameters.single()))
            }
        }

        cppVal.getter!!.body = irBuilder(irBuiltIns, cppVal.getter!!.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                .irBlockBody {
                    +irReturn(irGetField(irGet(cppVal.getter!!.dispatchReceiverParameter!!), cppVal.backingField!!))

                }

        val cleanerVal = irClass.declarations
                .filterIsInstance<IrProperty>()
                .filter { it.name.toString() == "cleaner" }
                .single()

        cleanerVal.backingField = symbolTable.declareField(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                cleanerVal.descriptor,
                cleanerVal.descriptor.type.toIrType(),
                DescriptorVisibilities.PRIVATE
        ).also { field ->
            field.parent = irClass
            //      PROPERTY name:cleaner visibility:private modality:FINAL [val]
            //        FIELD PROPERTY_BACKING_FIELD name:cleaner type:kotlin.native.internal.Cleaner visibility:private [final]
            //          EXPRESSION_BODY
            //            CALL 'public final fun createCleaner <T> (argument: T of kotlin.native.internal.createCleaner, block: kotlin.Function1<T of kotlin.native.internal.createCleaner, kotlin.Unit>): kotlin.native.internal.Cleaner declared in kotlin.native.internal' type=kotlin.native.internal.Cleaner origin=null
            //              <T>: kotlin.String
            //              argument: CALL 'public final fun <get-message> (): kotlin.String declared in <root>.Upper' type=kotlin.String origin=GET_PROPERTY
            //                $this: GET_VAR '<this>: <root>.Upper declared in <root>.Upper' type=<root>.Upper origin=null
            //              block: FUN_EXPR type=kotlin.Function1<kotlin.String, kotlin.Unit> origin=LAMBDA
            //                FUN LOCAL_FUNCTION_FOR_LAMBDA name:<anonymous> visibility:local modality:FINAL <> (field:kotlin.String) returnType:kotlin.Unit
            //                  VALUE_PARAMETER name:field index:0 type:kotlin.String
            //                  BLOCK_BODY
            //                    CALL 'public final fun println (message: kotlin.String): kotlin.Unit [external] declared in kotlin.io' type=kotlin.Unit origin=null
            //                      message: GET_VAR 'field: kotlin.String declared in <root>.Upper.cleaner.<anonymous>' type=kotlin.String origin=null
            field.initializer = irBuilder(irBuiltIns, field.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                val lambda = context.irFactory.buildFun {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                    name = Name.special("<anonymous>")
                    visibility = DescriptorVisibilities.LOCAL
                    returnType = irBuiltIns.unitType
                }.apply {
                    parent = field
                    valueParameters = listOf (
                            buildValueParameter(this) {
                                origin = IrDeclarationOrigin.DEFINED
                                name = Name.identifier("field")
                                index = 0
                                type = cppValType
                            }
                    )
                    body = irBlockBody {
                        val destroy = cppValType.classOrNull!!.owner.declarations
                                .filterIsInstance<IrSimpleFunction>()
                                .single { it.name.toString() == "__destroy__" }
                        +irCall(destroy).apply {
                            dispatchReceiver = irGet(valueParameters.single())
                        }
                    }
                }
                val callCreateCleaner = irCall(symbols.createCleaner).apply {
                    dispatchReceiver = null
                    putTypeArgument(0, cppValType)
                    putValueArgument(0,
                            irCall(cppVal.getter!!).apply {
                                dispatchReceiver = irGet(irClass.thisReceiver!!)
                            }
                    )
                    putValueArgument(1,
                            IrFunctionExpressionImpl(
                                    startOffset = SYNTHETIC_OFFSET,
                                    endOffset = SYNTHETIC_OFFSET,
                                    // type = irBuiltIns.function(1).defaultType,
                                    type = irBuiltIns.function(1).typeWith(cppValType, irBuiltIns.unitType),
                                    origin = IrStatementOrigin.LAMBDA,
                                    function = lambda
                            )
                    )
                }
                irExprBody(callCreateCleaner)
            }
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
                                it.putTypeArgument(0, irConstructor.valueParameters[0].type)
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