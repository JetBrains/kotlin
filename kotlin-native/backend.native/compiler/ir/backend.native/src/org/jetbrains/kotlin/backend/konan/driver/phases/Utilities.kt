/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKStringFromUtf8
import llvm.LLVMGetModuleIdentifier
import llvm.LLVMPrintModuleToFile
import llvm.size_tVar
import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.LlvmModuleCompilation
import org.jetbrains.kotlin.backend.konan.llvm.LlvmModuleCompilationOwner
import org.jetbrains.kotlin.backend.konan.reportCompilationWarning
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal fun irValidationCallback(state: ActionState, element: IrElement, context: Context) {
    if (!context.config.needVerifyIr) return

    val validatorConfig = IrValidatorConfig(
            abortOnError = false,
            ensureAllNodesAreDifferent = true,
            checkTypes = true,
            checkDescriptors = false
    )
    try {
        element.accept(IrValidator(context, validatorConfig), null)
        element.checkDeclarationParents()
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

internal val nativeStateIrValidator =
        fun(actionState: ActionState, data: IrElement, context: NativeGenerationState) {
            irValidationCallback(actionState, data, context.context)
        }

internal val nativeStateDumper =
        fun(actionState: ActionState, data: IrElement, context: NativeGenerationState) {
            defaultDumper(actionState, data, context.context)
        }

internal fun <C : PhaseContext, Input, Output> SimpleNamedCompilerPhase<C, Input, Output>.copy(
        preactions: Set<Action<Input, C>> = emptySet(),
        postactions: Set<Action<Output, C>> = emptySet(),
) = object : SimpleNamedCompilerPhase<C, Input, Output>(
        name,
        description,
        preactions = preactions,
        postactions = postactions,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: C, input: Input): Output =
            this@copy.outputIfNotEnabled(phaseConfig, phaserState, context, input)

    override fun phaseBody(context: C, input: Input): Output =
            this@copy.phaseBody(context, input)
}

internal fun <C: PhaseContext> llvmIrDumpCallback(state: ActionState, llvm: LlvmModuleCompilation, context: C) {
    if (state.beforeOrAfter == BeforeOrAfter.AFTER && state.phase.name in context.config.configuration.getList(KonanConfigKeys.SAVE_LLVM_IR)) {
        val moduleName: String = memScoped {
            val sizeVar = alloc<size_tVar>()
            LLVMGetModuleIdentifier(llvm.module, sizeVar.ptr)!!.toKStringFromUtf8()
        }
        val output = context.tempFiles.create("$moduleName.${state.phase.name}", ".ll")
        if (LLVMPrintModuleToFile(llvm.module, output.absolutePath, null) != 0) {
            error("Can't dump LLVM IR to ${output.absolutePath}")
        }
    }
}

// Actions:
// 1. IR module validation.
// 2. IR module dump.
// 3. LLVM module dump.
// 4. File validation