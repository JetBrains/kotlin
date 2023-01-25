/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import kotlinx.cinterop.*
import llvm.LLVMModuleRef
import llvm.LLVMPrintModuleToFile
import org.jetbrains.kotlin.backend.common.phaser.Action
import org.jetbrains.kotlin.backend.common.phaser.ActionState
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.getName
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import java.io.File

/**
 * Implementation of this interface in phase context, input or output
 * enables LLVM IR validation and dumping
 */
interface LlvmIrHolder {
    val llvmModule: LLVMModuleRef
}

/**
 * Create action that searches context and data for LLVM IR and dumps it.
 */
private fun <Data, Context : PhaseContext> createLlvmDumperAction(): Action<Data, Context> =
        fun(state: ActionState, data: Data, context: Context) {
            if (state.phase.name in context.config.configuration.getList(KonanConfigKeys.SAVE_LLVM_IR)) {
                val llvmModule = findLlvmModule(data, context)
                if (llvmModule == null) {
                    context.messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "Cannot dump LLVM IR ${state.beforeOrAfter.name.lowercase()} ${state.phase.name}")
                    return
                }
                val moduleName: String = llvmModule.getName()
                val output = File(context.config.saveLlvmIrDirectory, "$moduleName.${state.phase.name}.ll")
                if (LLVMPrintModuleToFile(llvmModule, output.absolutePath, null) != 0) {
                    error("Can't dump LLVM IR to ${output.absolutePath}")
                }
            }
        }

/**
 *
 */
private fun <Data, Context : PhaseContext> createLlvmVerifierAction(): Action<Data, Context> =
        fun(actionState: ActionState, data: Data, context: Context) {
            if (!context.config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)) {
                return
            }
            val llvmModule = findLlvmModule(data, context)
            if (llvmModule == null) {
                context.messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "Cannot verify LLVM IR ${actionState.beforeOrAfter.name.lowercase()} ${actionState.phase.name}")
                return
            }
            // TODO: Phase name in message
            verifyModule(llvmModule)
        }

/**
 *
 */
@Suppress("UNCHECKED_CAST")
private fun <Data, Context : PhaseContext> findLlvmModule(data: Data, context: Context): LLVMModuleRef? = when {
    // TODO: Not safe at all
    data is CPointer<*> -> data as LLVMModuleRef
    data is LlvmIrHolder -> data.llvmModule
    context is LlvmIrHolder -> context.llvmModule
    else -> null
}

/**
 * Default set of dump and validate actions for LLVM phases.
 */
internal fun <Data, Context : PhaseContext> getDefaultLlvmModuleActions(): Set<Action<Data, Context>> =
        setOf(createLlvmDumperAction(), createLlvmVerifierAction())