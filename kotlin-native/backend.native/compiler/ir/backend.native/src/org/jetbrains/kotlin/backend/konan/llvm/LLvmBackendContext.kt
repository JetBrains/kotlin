/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMDisposeMessage
import llvm.LLVMDisposeTargetMachine
import llvm.LLVMGetTarget
import llvm.LLVMKotlinCreateTargetMachine
import llvm.LLVMKotlinIsTargetTripleCompatible
import llvm.LLVMModuleRef
import llvm.LLVMTargetMachineRef
import org.jetbrains.kotlin.backend.konan.NativeBackendDiagnostics
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.cli.report

/**
 * Owns the target machine for one [withLlvmBackendContext] block.
 *
 * Only valid inside that block, on the thread running it. Don't keep this context,
 * or its [targetMachine], after the block returns.
 */
interface LlvmBackendContext {
    val config: LLvmBackendConfig
    val targetMachine: LLVMTargetMachineRef
}

private class LLvmBackendContextImpl(
        override val config: LLvmBackendConfig
) : LlvmBackendContext {

    private val targetMachineDelegate: Lazy<LLVMTargetMachineRef> = lazy {
        with(config.targetSpec) {
            memScoped {
                val errorMessage = alloc<CPointerVar<ByteVar>>()
                LLVMKotlinCreateTargetMachine(
                        targetTriple,
                        cpuModel,
                        cpuFeatures,
                        relocMode,
                        codeModel,
                        config.codegenOptimizationLevel,
                        config.instructionSelector.llvmValue,
                        errorMessage.ptr,
                ) ?: run {
                    val reason = errorMessage.value?.toKString()
                    LLVMDisposeMessage(errorMessage.value)
                    error(
                            "LLVM failed to create a target machine for triple '$targetTriple' (cpu='$cpuModel', features='$cpuFeatures'): $reason. " +
                                    "Either the target is not supported by the bundled LLVM, or LLVM targets were not initialized (see LlvmEnvironment.init)."
                    )
                }
            }
        }
    }

    override val targetMachine: LLVMTargetMachineRef by targetMachineDelegate

    fun dispose() {
        if (targetMachineDelegate.isInitialized()) {
            LLVMDisposeTargetMachine(targetMachine)
        }
    }
}

fun <T> withLlvmBackendContext(llvmBackendConfig: LLvmBackendConfig, block: LlvmBackendContext.() -> T): T {
    val context = LLvmBackendContextImpl(llvmBackendConfig)
    return try {
        context.block()
    } finally {
        context.dispose()
    }
}

internal fun NativeBackendPhaseContext.checkTargetTriple(module: LLVMModuleRef) {
    val expected = config.llvmTargetTriple
    val actual = LLVMGetTarget(module)!!.toKString()
    if (LLVMKotlinIsTargetTripleCompatible(expected, actual) == 0) {
        config.configuration.report(NativeBackendDiagnostics.LLVM_WARNING,
                "Bitcode module ${module.getName()} targets '$actual', incompatible with compilation target '$expected'")
    }
}
