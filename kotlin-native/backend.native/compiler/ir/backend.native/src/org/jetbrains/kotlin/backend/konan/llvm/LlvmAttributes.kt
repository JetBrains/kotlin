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
import org.jetbrains.kotlin.ir.util.isThrowable
import org.jetbrains.kotlin.konan.target.Family

internal fun addLlvmFunctionWithDefaultAttributes(
        context: Context,
        module: LLVMModuleRef,
        name: String,
        type: LLVMTypeRef
): LLVMValueRef = LLVMAddFunction(module, name, type)!!.also {
    addDefaultLlvmFunctionAttributes(context, it)
}

/**
 * Mimics parts of clang's `CodeGenModule::getDefaultFunctionAttributes`
 * that are required for Kotlin/Native compiler.
 */
private fun addDefaultLlvmFunctionAttributes(context: Context, llvmFunction: LLVMValueRef) {
    if (shouldEnforceFramePointer(context)) {
        // Note: this is default for clang on at least on iOS and macOS.
        enforceFramePointer(llvmFunction)
    }
}

internal fun addLlvmAttributesForKotlinFunction(context: Context, irFunction: IrFunction, llvmFunction: LLVMValueRef) {
    if (irFunction.returnType.isNothing()) {
        setFunctionNoReturn(llvmFunction)
    }

    if (mustNotInline(context, irFunction)) {
        setFunctionNoInline(llvmFunction)
    }
}

private fun mustNotInline(context: Context, irFunction: IrFunction): Boolean {
    if (context.shouldContainLocationDebugInfo()) {
        if (irFunction is IrConstructor && irFunction.isPrimary && irFunction.returnType.isThrowable()) {
            // To simplify skipping this constructor when scanning call stack in Kotlin_getCurrentStackTrace.
            return true
        }
    }

    return false
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

private fun enforceFramePointer(llvmFunction: LLVMValueRef) {
    LLVMAddTargetDependentFunctionAttr(llvmFunction, "no-frame-pointer-elim", "true")
    LLVMAddTargetDependentFunctionAttr(llvmFunction, "no-frame-pointer-elim-non-leaf", "")
}
