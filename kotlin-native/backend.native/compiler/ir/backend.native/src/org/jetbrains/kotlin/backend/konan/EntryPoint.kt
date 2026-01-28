/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
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
    }
    entryPoint.annotations += buildSimpleAnnotation(context.irBuiltIns,
            startOffset, endOffset,
            context.symbols.exportForCppRuntime.owner, "Konan_start")

    val builder = context.createIrBuilder(entryPoint.symbol, startOffset, endOffset)
    entryPoint.body = builder.irBlockBody(entryPoint) {
        +IrTryImpl(startOffset, endOffset, context.irBuiltIns.nothingType).apply {
            tryResult = irBlock {
                +irCall(actualMain).apply {
                    when (actualMain.parameters.size) {
                        0 -> Unit
                        1 -> arguments[0] = irGet(entryPoint.parameters[0])
                        else -> error("Too many parameters")
                    }
                }
                +irReturn(irInt(0))
            }
            val catchParameter = buildVariable(
                    builder.parent, startOffset, endOffset,
                    IrDeclarationOrigin.CATCH_PARAMETER,
                    Name.identifier("e"),
                    context.irBuiltIns.throwableType
            )
            catches += irCatch(
                    catchParameter,
                    result = irBlock {
                        +irCall(context.symbols.processUnhandledException).apply {
                            arguments[0] = irGet(catchParameter)
                        }
                        +irCall(context.symbols.terminateWithUnhandledException).apply {
                            arguments[0] = irGet(catchParameter)
                        }
                    }
            )
            Unit
        }
    }

    return entryPoint
}

/**
 * Origin for hot reload entry point declarations (Konan_main).
 * This is used to identify the entry point after codegen so we can
 * set the proper LLVM visibility/linkage attributes.
 */
internal val DECLARATION_ORIGIN_HOT_RELOAD_ENTRY_POINT = IrDeclarationOriginImpl("HOT_RELOAD_ENTRY_POINT")

/**
 * Creates the entry point function for hot reload bootstrap (Konan_main).
 * This is the user's entry point that will be called by the HotReload launcher.
 *
 * Unlike [makeEntryPoint] which creates Konan_start, this creates Konan_main which:
 * - Has PUBLIC visibility (will be exported via LLVM APIs after codegen)
 * - Will be looked up by JITLink and called from the HotReload launcher
 * - Contains the same logic as Konan_start (call user's main, handle exceptions)
 *
 * NOTE: We do NOT use @exportForCppRuntime annotation here. Instead, the function
 * is exported by setting LLVM visibility/linkage attributes directly after codegen
 * in [exportHotReloadEntryPoint].
 *
 * The HotReload launcher (C++ side) provides Konan_start which:
 * 1. Initializes the K/N runtime
 * 2. Loads bootstrap.o via JITLink
 * 3. Looks up and calls Konan_main
 */
internal fun makeHotReloadEntryPoint(generationState: NativeGenerationState): IrFunction {
    val context = generationState.context
    val actualMain = context.symbols.entryPoint!!.owner
    val startOffset = if (generationState.llvmModuleSpecification.containsDeclaration(actualMain))
        actualMain.startOffset
    else
        SYNTHETIC_OFFSET
    val endOffset = if (generationState.llvmModuleSpecification.containsDeclaration(actualMain))
        actualMain.endOffset
    else
        SYNTHETIC_OFFSET

    // Create Konan_main function with PUBLIC visibility
    // The function will be exported via LLVM APIs after codegen (see exportHotReloadEntryPoint)
    val entryPoint = context.irFactory.buildFun {
        this.startOffset = startOffset
        this.endOffset = endOffset
        origin = DECLARATION_ORIGIN_HOT_RELOAD_ENTRY_POINT
        name = Name.identifier("Konan_main")  // Different name for hot reload
        visibility = DescriptorVisibilities.PUBLIC  // PUBLIC so it can be exported
        returnType = context.irBuiltIns.intType
    }.apply {
        addValueParameter {
            this.startOffset = startOffset
            this.endOffset = endOffset
            origin = DECLARATION_ORIGIN_HOT_RELOAD_ENTRY_POINT
            name = Name.identifier("args")
            type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
        }
    }

    // NOTE: We do NOT add @exportForCppRuntime annotation here.
    // The function will be exported via LLVM APIs after codegen.

    val builder = context.createIrBuilder(entryPoint.symbol, startOffset, endOffset)
    entryPoint.body = builder.irBlockBody(entryPoint) {
        +IrTryImpl(startOffset, endOffset, context.irBuiltIns.nothingType).apply {
            tryResult = irBlock {
                +irCall(actualMain).apply {
                    when (actualMain.parameters.size) {
                        0 -> Unit
                        1 -> arguments[0] = irGet(entryPoint.parameters[0])
                        else -> error("Too many parameters")
                    }
                }
                +irReturn(irInt(0))
            }
            val catchParameter = buildVariable(
                    builder.parent, startOffset, endOffset,
                    IrDeclarationOrigin.CATCH_PARAMETER,
                    Name.identifier("e"),
                    context.irBuiltIns.throwableType
            )
            catches += irCatch(
                    catchParameter,
                    result = irBlock {
                        +irCall(context.symbols.processUnhandledException).apply {
                            arguments[0] = irGet(catchParameter)
                        }
                        +irCall(context.symbols.terminateWithUnhandledException).apply {
                            arguments[0] = irGet(catchParameter)
                        }
                    }
            )
            Unit
        }
    }

    return entryPoint
}
