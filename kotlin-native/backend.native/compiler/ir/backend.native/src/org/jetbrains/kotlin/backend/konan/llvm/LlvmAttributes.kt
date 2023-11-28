/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget


/**
 * Mimics parts of clang's `CodeGenModule::getDefaultFunctionAttributes`
 * that are required for Kotlin/Native compiler.
 */
internal fun addDefaultLlvmFunctionAttributes(context: Context, llvmFunction: LLVMValueRef) {
    if (shouldEnforceFramePointer(context)) {
        // Note: this is default for clang on at least on iOS and macOS.
        enforceFramePointer(llvmFunction, context)
    }
}

/**
 * Set target cpu and its features to make LLVM generate correct machine code.
 */
internal fun addTargetCpuAndFeaturesAttributes(context: Context, llvmFunction: LLVMValueRef) {
    context.config.platform.targetCpu?.let {
        LLVMAddTargetDependentFunctionAttr(llvmFunction, "target-cpu", it)
    }
    context.config.platform.targetCpuFeatures?.let {
        LLVMAddTargetDependentFunctionAttr(llvmFunction, "target-features", it)
    }
}

private fun shouldEnforceFramePointer(context: Context): Boolean {
    // TODO: do we still need it?
    if (!context.shouldOptimize()) {
        return true
    }

    return when (context.config.target.family) {
        Family.OSX, Family.IOS, Family.WATCHOS, Family.TVOS -> context.shouldContainLocationDebugInfo()
        Family.LINUX, Family.MINGW, Family.ANDROID, Family.WASM, Family.ZEPHYR -> false
    }
}

private fun enforceFramePointer(llvmFunction: LLVMValueRef, context: Context) {
    val target = context.config.target

    // Matches Clang behaviour.
    val omitLeafFp = when {
        target.architecture == Architecture.ARM64 -> true
        else -> false
    }

    val fpKind = if (omitLeafFp) {
        "non-leaf"
    } else {
        "all"
    }

    LLVMAddTargetDependentFunctionAttr(llvmFunction, "frame-pointer", fpKind)
}

