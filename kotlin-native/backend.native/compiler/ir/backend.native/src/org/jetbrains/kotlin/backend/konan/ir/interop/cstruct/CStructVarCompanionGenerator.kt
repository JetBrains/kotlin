/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cstruct

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.getArgumentValueOrNull
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

private val varTypeAnnotationFqName = FqName("kotlinx.cinterop.internal.CStruct.VarType")

internal class CStructVarCompanionGenerator(
        context: GeneratorContext,
        private val interopBuiltIns: InteropBuiltIns
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator
    override val postLinkageSteps: MutableList<() -> Unit> = mutableListOf()

    fun generate(structDescriptor: ClassDescriptor): IrClass =
            createClass(structDescriptor.companionObjectDescriptor!!) { companionIrClass ->
                if (structDescriptor.annotations.hasAnnotation(RuntimeNames.managedType)) {
                    companionIrClass.addMember(createCompanionConstructor(companionIrClass.descriptor, 0L, 0))
                } else {
                    val annotation = companionIrClass.descriptor.annotations
                            .findAnnotation(varTypeAnnotationFqName)!!
                    val size = annotation.getArgumentValueOrNull<Long>("size")!!
                    val align = annotation.getArgumentValueOrNull<Int>("align")!!
                    companionIrClass.addMember(createCompanionConstructor(companionIrClass.descriptor, size, align))
                }
                companionIrClass.descriptor.unsubstitutedMemberScope
                        .getContributedDescriptors()
                        .filterIsInstance<CallableMemberDescriptor>()
                        .filterNot { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
                        .mapNotNull {
                            when (it) {
                                is PropertyDescriptor -> createProperty(it)
                                is SimpleFunctionDescriptor -> createFunction(it)
                                else -> null
                            }
                        }
                        .forEach(companionIrClass::addMember)
            }

    private fun createCompanionConstructor(companionObjectDescriptor: ClassDescriptor, size: Long, align: Int): IrConstructor {
        if (companionObjectDescriptor.containingDeclaration.annotations.hasAnnotation(RuntimeNames.managedType)) {
            return createConstructor(companionObjectDescriptor.unsubstitutedPrimaryConstructor!!).also { irConstructor ->
                postLinkageSteps.add {
                    irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                        +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                                startOffset, endOffset, context.irBuiltIns.unitType,
                                irBuiltIns.anyClass.owner.primaryConstructor!!.symbol
                        )
                        +irInstanceInitializer(symbolTable.referenceClass(companionObjectDescriptor))
                    }
                }
            }
        } else {
            val superConstructorSymbol = symbolTable.referenceConstructor(interopBuiltIns.cStructVarType.unsubstitutedPrimaryConstructor!!)
            return createConstructor(companionObjectDescriptor.unsubstitutedPrimaryConstructor!!).also { irConstructor ->
                postLinkageSteps.add {
                    irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                        +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                                startOffset, endOffset, context.irBuiltIns.unitType,
                                superConstructorSymbol
                        ).also {
                            it.putValueArgument(0, irLong(size))
                            it.putValueArgument(1, irInt(align))
                        }
                        +irInstanceInitializer(symbolTable.referenceClass(companionObjectDescriptor))
                    }
                }
            }
        }
    }
}