/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.library.AbstractNativeKlibWriterTest.NativeParameters
import org.jetbrains.kotlin.konan.library.AbstractNativeKlibWriterTest.NativeParameters.BinaryFile
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_BITCODE_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesConstants.KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.AbstractKlibWriterTest
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SHORT_NAME
import org.jetbrains.kotlin.library.KlibMockDSL
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import kotlin.collections.set
import kotlin.random.Random

abstract class AbstractNativeKlibWriterTest<P : NativeParameters>(newParameters: () -> P) : AbstractKlibWriterTest<P>(newParameters) {
    open class NativeParameters : Parameters() {
        var shortName: String? = null
        var target: KonanTarget = KonanTarget.IOS_ARM64
        var bitCodeFiles: List<BinaryFile> = emptyList()
        var includedFiles: List<BinaryFile> = emptyList()

        final override var builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.NATIVE
            set(_) = abort<Nothing>("only NATIVE built-ins platform is supported")

        class BinaryFile(val file: File, content: ByteArray) {
            init {
                file.writeBytes(content)
            }

            val content: ByteArray get() = file.readBytes()
        }
    }

    @Test
    fun `Writing a klib with different short names`() {
        listOf("foo", "bar-bar", null).forEach { shortName ->
            runTestWithParameters {
                this.shortName = shortName
            }
        }
    }

    @Test
    fun `Writing a klib with different targets`() {
        listOf(KonanTarget.IOS_ARM64, KonanTarget.MACOS_ARM64, KonanTarget.WATCHOS_ARM64).forEach { target ->
            runTestWithParameters {
                this.target = target
            }
        }
    }

    @Test
    fun `Writing a klib with custom included files`() {
        repeat(3) { index ->
            runTestWithParameters {
                includedFiles = mockBinaryFiles(count = index + 1, prefix = index.toString(), extension = "o")
            }
        }
    }

    @Test
    fun `Writing a klib with custom bitcode files`() {
        repeat(3) { index ->
            runTestWithParameters {
                bitCodeFiles = mockBinaryFiles(count = index + 1, prefix = index.toString(), extension = "bc")
            }
        }
    }

    context(dsl: KlibMockDSL)
    override fun customizeMockKlib(parameters: P) {
        super.customizeMockKlib(parameters)
        dsl.targets(parameters.target) {
            included(parameters.includedFiles)
            native(parameters.bitCodeFiles)
        }
    }

    context(properties: Properties)
    override fun customizeManifestForMockKlib(parameters: P) {
        super.customizeManifestForMockKlib(parameters)
        parameters.shortName?.let { shortName -> properties[KLIB_PROPERTY_SHORT_NAME] = shortName }
        properties[KLIB_PROPERTY_NATIVE_TARGETS] = parameters.target.visibleName
    }

    protected fun mockBinaryFiles(count: Int, prefix: String, extension: String): List<BinaryFile> {
        require(count >= 0) { "Count must be non-negative, but was $count" }

        if (count == 0) return emptyList()

        val random = Random(System.nanoTime())

        return List(count) { index ->
            BinaryFile(tmpDir.resolve("file-$prefix-$index.$extension"), random.nextBytes(100))
        }
    }
}

fun KlibMockDSL.targets(target: KonanTarget, init: KlibMockDSL.() -> Unit = {}) {
    dir(KLIB_TARGETS_FOLDER_NAME) {
        dir(target.name, init)
    }
}

fun KlibMockDSL.included(binaryFiles: List<BinaryFile> = emptyList()) {
    dir(KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME) {
        binaryFiles.forEach { binaryFile -> file(binaryFile.file.name, binaryFile.content) }
    }
}

fun KlibMockDSL.native(binaryFiles: List<BinaryFile> = emptyList()) {
    dir(KLIB_BITCODE_FOLDER_NAME) {
        binaryFiles.forEach { binaryFile -> file(binaryFile.file.name, binaryFile.content) }
    }
}
