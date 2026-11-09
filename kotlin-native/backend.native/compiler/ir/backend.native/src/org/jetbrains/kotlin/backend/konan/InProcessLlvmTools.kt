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

// Where the LLVM linked into the compiler replaces the `llvm-ar` processes by default
// (unless `-Xbinary=inProcessLlvmTools=false`): only where the results were checked to be byte-for-byte identical
// to the ones of the external tools, so that turning it on changes nothing but the build time.

/**
 * The targets for which the static caches written by [writeStaticCacheArchive] were checked to be the same as the ones
 * of `llvm-ar`.
 */
internal val inProcessArchiveTargetTriples = setOf("x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu")

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
