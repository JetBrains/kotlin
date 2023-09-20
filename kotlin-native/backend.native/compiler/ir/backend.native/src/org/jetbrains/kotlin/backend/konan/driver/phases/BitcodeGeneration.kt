/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.DIFinalize
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.utilities.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultLlvmModuleActions
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.RTTIGeneratorVisitor
import org.jetbrains.kotlin.backend.konan.llvm.createLlvmDeclarations
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal val CreateLLVMDeclarationsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "CreateLLVMDeclarations",
        description = "Map IR declarations to LLVM",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, module ->
            generationState.llvmDeclarations = createLlvmDeclarations(generationState, module)
        }
)

internal data class RTTIInput(
        val irModule: IrModuleFragment,
        val referencedFunctions: Set<IrFunction>?
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val RTTIPhase = createSimpleNamedCompilerPhase<NativeGenerationState, RTTIInput>(
        name = "RTTI",
        description = "RTTI generation",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val visitor = RTTIGeneratorVisitor(generationState, input.referencedFunctions)
            input.irModule.acceptVoid(visitor)
            visitor.dispose()
        }
)

internal data class CodegenInput(
        val irModule: IrModuleFragment,
        val lifetimes: Map<IrElement, Lifetime>
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val CodegenPhase = createSimpleNamedCompilerPhase<NativeGenerationState, CodegenInput>(
        name = "Codegen",
        description = "Code generation",
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

            input.irModule.acceptVoid(CodeGeneratorVisitor(generationState, input.irModule.irBuiltins, input.lifetimes))

            if (generationState.hasDebugInfo())
                DIFinalize(generationState.debugInfo.builder)
        }
)