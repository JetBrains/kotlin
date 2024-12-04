/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import kotlinx.cinterop.JvmCInteropCallbacks
import org.jetbrains.kotlin.utils.NativeMemoryAllocator
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class IndexerTests {
    init {
        System.load(System.getProperty("kotlin.native.llvm.libclang"))
    }

    @BeforeTest
    fun init() {
        NativeMemoryAllocator.init()
        JvmCInteropCallbacks.init()
    }

    @AfterTest
    fun dispose() {
        JvmCInteropCallbacks.dispose()
        NativeMemoryAllocator.dispose()
    }

    class TempFiles(name: String) {
        private val tempRootDir = System.getProperty("kotlin.native.interop.indexer.temp") ?: System.getProperty("java.io.tmpdir") ?: "."

        val directory: File = File(tempRootDir, name).canonicalFile.also {
            it.mkdirs()
        }

        fun file(relativePath: String, contents: String): File = File(directory, relativePath).canonicalFile.apply {
            parentFile.mkdirs()
            writeText(contents)
        }
    }
}
