/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.v2.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.name.Name

internal val DECLARATION_ORIGIN_ENTRY_POINT = IrDeclarationOriginImpl("ENTRY_POINT")

internal fun makeEntryPoint(generationState: NativeGenerationState): IrFunction {
    val context = generationState.context
    val actualMain = context.symbols.entryPoint!!.owner
    // TODO: Do we need to do something with the offsets if <main> is in a cached library?
    val startOffset = if (generationState.llvmModuleSpecification.containsDeclaration(actualMain))
        actualMain.startOffset
    else
        SYNTHETIC_OFFSET
    val endOffset = if (generationState.llvmModuleSpecification.containsDeclaration(actualMain))
        actualMain.endOffset
    else
        SYNTHETIC_OFFSET
    val entryPoint = context.irFactory.buildFun {
        this.startOffset = startOffset
        this.endOffset = endOffset
        origin = DECLARATION_ORIGIN_ENTRY_POINT
        name = Name.identifier("Konan_start")
        visibility = DescriptorVisibilities.PRIVATE
        returnType = context.irBuiltIns.intType
    }.apply {
        addValueParameter {
            this.startOffset = startOffset
            this.endOffset = endOffset
            origin = DECLARATION_ORIGIN_ENTRY_POINT
            name = Name.identifier("args")
            type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
        }
        annotations += buildSimpleAnnotation(context.irBuiltIns,
                startOffset, endOffset,
                context.symbols.exportForCppRuntime.owner, "Konan_start")
        buildBody {
            withBuiltIns(context.irBuiltIns) {
                +irTry(
                        context.irBuiltIns.nothingType,
                        result = irBlock {
                            +this.irCall(actualMain.symbol, context.irBuiltIns.unitType).apply {
                                when (actualMain.parameters.size) {
                                    0 -> {}
                                    1 -> arguments[0] = irGet(parameters[0])
                                    else -> error("Too many parameters")
                                }
                            }
                            +irReturn(irInt(0))
                        },
                        body = {
                            addCatch(Name.identifier("e"), context.irBuiltIns.throwableType) { e ->
                                irBlock {
                                    +irCall(context.symbols.processUnhandledException, context.irBuiltIns.unitType).apply {
                                        arguments[0] = irGet(e.owner)
                                    }
                                    +irCall(context.symbols.terminateWithUnhandledException, context.irBuiltIns.nothingType).apply {
                                        arguments[0] = irGet(e.owner)
                                    }
                                }
                            }
                        }
                )
            }
        }
    }

    return entryPoint
}
