/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.DIFinalize
import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultLlvmModuleActions
import org.jetbrains.kotlin.backend.konan.initializeCachedBoxes
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.RTTIGeneratorVisitor
import org.jetbrains.kotlin.backend.konan.llvm.createLlvmDeclarations
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal val CreateLLVMDeclarationsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, List<IrFile>>(
        name = "CreateLLVMDeclarations",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, files ->
            generationState.llvmDeclarations = createLlvmDeclarations(generationState, files)
        }
)

internal data class RTTIInput(
        val irFiles: List<IrFile>,
        val referencedFunctions: Set<IrSimpleFunction>?
)

internal val RTTIPhase = createSimpleNamedCompilerPhase<NativeGenerationState, RTTIInput>(
        name = "RTTI",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val visitor = RTTIGeneratorVisitor(generationState, input.referencedFunctions)
            input.irFiles.forEach {
                it.acceptVoid(visitor)
            }
            visitor.dispose()
        }
)

internal data class CodegenInput(
        val irModule: IrModuleFragment,
        val irFiles: List<IrFile>,
        val irBuiltIns: IrBuiltIns,
        val lifetimes: Map<IrElement, Lifetime>
)

internal val CodegenPhase = createSimpleNamedCompilerPhase<NativeGenerationState, CodegenInput>(
        name = "Codegen",
        preactions = getDefaultIrActions<CodegenInput, NativeGenerationState>() + getDefaultLlvmModuleActions(),
        postactions = getDefaultIrActions<CodegenInput, NativeGenerationState>() + getDefaultLlvmModuleActions(),
        op = { generationState, input ->
            val context = generationState.context
            generationState.objCExport = ObjCExport(
                    generationState,
                    input.irModule.descriptor,
                    context.objCExportedInterface,
                    context.objCExportCodeSpec
            )
            generationState.hasObjCExport = true

            initializeCachedBoxes(generationState)

            val visitor = CodeGeneratorVisitor(generationState, input.irBuiltIns, input.lifetimes)

            input.irFiles.forEach {
                it.acceptVoid(visitor)
            }

            visitor.processAllInitializers(generationState.context.config.isFinalBinary && (!generationState.shouldOptimize() || generationState.llvmModuleSpecification.isFinal))

            if (generationState.hasDebugInfo())
                DIFinalize(generationState.debugInfo.builder)
        }
)
