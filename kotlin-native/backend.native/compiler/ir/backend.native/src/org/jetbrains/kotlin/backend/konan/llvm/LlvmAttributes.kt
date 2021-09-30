/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isThrowable
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun addLlvmFunctionWithDefaultAttributes(
        context: Context,
        module: LLVMModuleRef,
        name: String,
        type: LLVMTypeRef
): LLVMValueRef = LLVMAddFunction(module, name, type)!!.also {
    addDefaultLlvmFunctionAttributes(context, it)
    addTargetCpuAndFeaturesAttributes(context, it)
}

/**
 * Mimics parts of clang's `CodeGenModule::getDefaultFunctionAttributes`
 * that are required for Kotlin/Native compiler.
 */
private fun addDefaultLlvmFunctionAttributes(context: Context, llvmFunction: LLVMValueRef) {
    if (shouldEnforceFramePointer(context)) {
        // Note: this is default for clang on at least on iOS and macOS.
        enforceFramePointer(llvmFunction, context)
    }
}

/**
 * Set target cpu and its features to make LLVM generate correct machine code.
 */
private fun addTargetCpuAndFeaturesAttributes(context: Context, llvmFunction: LLVMValueRef) {
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
        target == KonanTarget.WATCHOS_ARM64 -> false
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

interface LlvmAttribute {
    fun asAttributeKindId(): LLVMAttributeKindId
}

// We use sealed class instead of enum because there are attributes with parameters
// that we might want to use later. For example, align(<n>).
sealed class LlvmParameterAttribute(private val llvmAttributeName: String) : LlvmAttribute {

    override fun asAttributeKindId(): LLVMAttributeKindId = llvmAttributeKindIdCache.getOrPut(this) {
        getLlvmAttributeKindId(llvmAttributeName)
    }

    companion object {
        private val llvmAttributeKindIdCache = mutableMapOf<LlvmParameterAttribute, LLVMAttributeKindId>()
    }

    object SignExt : LlvmParameterAttribute("signext")
    object ZeroExt : LlvmParameterAttribute("zeroext")
}

sealed class LlvmFunctionAttribute(private val llvmAttributeName: String) : LlvmAttribute {

    override fun asAttributeKindId(): LLVMAttributeKindId = llvmAttributeKindIdCache.getOrPut(this) {
        getLlvmAttributeKindId(llvmAttributeName)
    }

    companion object {
        private val llvmAttributeKindIdCache = mutableMapOf<LlvmFunctionAttribute, LLVMAttributeKindId>()
    }

    object NoUnwind : LlvmFunctionAttribute("nounwind")
    object NoReturn : LlvmFunctionAttribute("noreturn")
    object NoInline : LlvmFunctionAttribute("noinline")
}
