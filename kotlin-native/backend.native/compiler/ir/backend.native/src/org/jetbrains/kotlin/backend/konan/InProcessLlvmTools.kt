/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMDisposeMessage
import llvm.LLVMKotlinWriteArchive
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.konan.target.ClangFlags

// Where the LLVM linked into the compiler replaces the `clang` and `llvm-ar` processes by default
// (unless `-Xbinary=inProcessLlvmTools=false`): only where the results were checked to be byte-for-byte identical
// to the ones of the external tools, so that turning it on changes nothing but the build time.

/**
 * The targets for which [LlvmBitcodeCompiler] was checked to produce the same non-optimized object files as `clang`
 * (the per-file caches of the standard library and of a project, and the executable linked from them).
 *
 * aarch64 Linux is not here: its non-optimized `clang` flags don't put functions and data into separate sections,
 * while the target machine of [LlvmBitcodeCompiler] does, see [emitsLikeClangInProcess].
 */
private val inProcessEmissionTargetTriples = setOf("x86_64-unknown-linux-gnu")

/**
 * The targets for which the static caches written by [writeStaticCacheArchive] were checked to be the same as the ones
 * of `llvm-ar`.
 */
internal val inProcessArchiveTargetTriples = setOf("x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu")

/**
 * Whether [LlvmBitcodeCompiler] emits the same object file as `clang` run with the flags [ClangBitcodeCompiler] would use.
 *
 * Only for non-optimized code generation: when it optimizes, the generated code differs from clang's, because clang reads
 * the module back from bitcode, which doesn't preserve the order of use lists that some code generation passes depend on.
 * Optimized builds are dominated by the LLVM optimizations anyway.
 */
internal fun NativeBackendPhaseContext.emitsLikeClangInProcess(): Boolean {
    if (!config.inProcessLlvmTools || config.llvmTargetTriple !in inProcessEmissionTargetTriples) return false
    // The code generation level of the target machine, see `llvmCodeGenOptLevel`.
    if (shouldOptimize() || !shouldContainDebugInfo()) return false
    val configurables = config.platform.configurables as? ClangFlags ?: return false
    return isEquivalentToInProcessEmission(clangBitcodeCompilerFlags(configurables))
}

/**
 * Whether `clang` run with [flags] generates code the same way as the target machine of [LlvmBitcodeCompiler]
 * (see `LLVMKotlinCreateTargetMachine`) for the targets in [inProcessEmissionTargetTriples]: non-optimized,
 * with functions and data in separate sections. The relocation model is the same by construction: both come from
 * the relocation mode of the output.
 * Any flag not listed here (e.g. from `-Xoverride-clang-options`) may change the generated code, so `clang` has to be run.
 */
private fun NativeBackendPhaseContext.isEquivalentToInProcessEmission(flags: List<String>): Boolean {
    var cc1 = false
    var emitObj = false
    var noLlvmPasses = false
    var irInput = false
    var functionSections = false
    var dataSections = false
    val iterator = flags.iterator()
    while (iterator.hasNext()) {
        when (iterator.next()) {
            "-cc1" -> cc1 = true
            "-emit-obj" -> emitObj = true
            "-disable-llvm-optzns", "-disable-llvm-passes" -> noLlvmPasses = true
            "-x" -> irInput = iterator.hasNext() && iterator.next() == "ir"
            "-triple" -> if (!iterator.hasNext() || iterator.next() != config.llvmTargetTriple) return false
            "-ffunction-sections" -> functionSections = true
            "-fdata-sections" -> dataSections = true
            "-mrelocation-model" -> if (!iterator.hasNext() || iterator.next() !in setOf("pic", "static")) return false
            "-O0" -> {}
            else -> return false
        }
    }
    return cc1 && emitObj && noLlvmPasses && irInput && functionSections && dataSections
}

/**
 * Writes the static library [archivePath] containing [objectFiles] like `llvm-ar qcsDL` does when given object files only.
 */
internal fun writeStaticCacheArchive(archivePath: String, objectFiles: List<String>) = memScoped {
    val members = allocArray<CPointerVar<ByteVar>>(objectFiles.size)
    objectFiles.forEachIndexed { index, path -> members[index] = path.cstr.ptr }
    val errorMessage = alloc<CPointerVar<ByteVar>>()
    if (LLVMKotlinWriteArchive(archivePath, members, objectFiles.size, errorMessage.ptr) != 0) {
        val message = errorMessage.value?.toKString()
        LLVMDisposeMessage(errorMessage.value)
        error("Failed to write the static library $archivePath: $message")
    }
}
