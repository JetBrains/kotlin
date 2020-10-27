/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cstruct

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.descriptors.getArgumentValueOrNull
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irBuilder
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

    fun generate(structDescriptor: ClassDescriptor): IrClass =
            createClass(structDescriptor.companionObjectDescriptor!!) { companionIrClass ->
                val annotation = companionIrClass.descriptor.annotations
                        .findAnnotation(varTypeAnnotationFqName)!!
                val size = annotation.getArgumentValueOrNull<Long>("size")!!
                val align = annotation.getArgumentValueOrNull<Int>("align")!!
                companionIrClass.addMember(createCompanionConstructor(companionIrClass.descriptor, size, align))
            }

    private fun createCompanionConstructor(companionObjectDescriptor: ClassDescriptor, size: Long, align: Int): IrConstructor {
        val superConstructorSymbol = symbolTable.referenceConstructor(interopBuiltIns.cStructVarType.unsubstitutedPrimaryConstructor!!)
        return createConstructor(companionObjectDescriptor.unsubstitutedPrimaryConstructor!!).also { irConstructor ->
            irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                        startOffset, endOffset, context.irBuiltIns.unitType,
                        superConstructorSymbol,
                        irConstructor.typeParameters.size,
                        2
                ).also {
                    it.putValueArgument(0, irLong(size))
                    it.putValueArgument(1, irInt(align))
                }
                +irInstanceInitializer(symbolTable.referenceClass(companionObjectDescriptor))
            }
        }
    }
}