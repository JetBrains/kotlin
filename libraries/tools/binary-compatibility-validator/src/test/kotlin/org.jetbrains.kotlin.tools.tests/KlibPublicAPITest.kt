/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBCVApi::class)

package org.jetbrains.kotlin.tools.tests

import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.api.klib.*
import org.junit.Assume
import java.io.File
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class KlibPublicAPITest {

    @Test
    fun jsWasmJsWasmWasiStdlib() {
        val dump = regularDump(
            "kotlin-stdlib-js-wasm",
            "../../stdlib/build/libs",
            listOf("kotlin-stdlib-js", "kotlin-stdlib-wasm-js", "kotlin-stdlib-wasm-wasi"),
            KLibDumpFilters {
                ignoredPackages += setOf(
                    "org.w3c",
                    "org.khronos.webgl",
                    "kotlinx.dom",
                    "kotlinx.browser",
                )
            }
        )
        mergeAndCompare("kotlin-stdlib", dump)
    }

    @Test
    fun nativeStdlib() {
        Assume.assumeTrue("Skipped, pass kotlin.native.enabled gradle property to enable", NATIVE_ENABLED)
        val dump = nativeDump("kotlin-stdlib-native", "../../../kotlin-native/runtime/build/nativeStdlib")
        mergeAndCompare("kotlin-stdlib", dump)
    }

    private fun regularDump(apiFileBaseName: String, basePath: String, klibPatterns: List<String>, filters: KlibDumpFilters = KlibDumpFilters.DEFAULT): KlibDump {
        val base = File(basePath).absoluteFile.normalize()

        val mergedDump = KlibDump().apply {
            for (klibPattern in klibPatterns) {
                mergeFromKlib(getKlibFile(base, klibPattern, System.getProperty("kotlinVersion")), filters = filters)
            }
        }
        mergedDump.saveTo(pathInCreatedDirectory("build/api/$apiFileBaseName.api").toFile())
        return mergedDump
    }

    private fun nativeDump(apiFileBaseName: String, klibPath: String, filters: KlibDumpFilters = KlibDumpFilters.DEFAULT): KlibDump {
        val nativeDumpPath = pathInCreatedDirectory("build/api/$apiFileBaseName.api")
        val nativeDump = KlibDump.fromKlib(File(klibPath), filters = filters)
        val nativeDumpText = StringBuilder()
        nativeDump.saveTo(nativeDumpText)
        val uniqueNameHeader = "Library unique name: <stdlib>"
        val uniqueNameReplacement = "Library unique name: <kotlin>"
        val headerIndex = nativeDumpText.indexOf(uniqueNameHeader)
        try {
            assertTrue(headerIndex >= 0, "Expected header '$uniqueNameHeader' is missing in K/N dump file $nativeDumpPath")
            nativeDumpText.replace(headerIndex, headerIndex + uniqueNameHeader.length, uniqueNameReplacement)
        } finally {
            nativeDumpPath.writeText(nativeDumpText)
        }
        return KlibDump.from(nativeDumpPath.toFile())
    }

    private fun mergeAndCompare(apiFileBaseName: String, dump: KlibDump) {
        val mergedDumpFile = File("klib-public-api").resolve("$apiFileBaseName.api")
        if (!mergedDumpFile.exists()) {
            mergedDumpFile.toPath().createParentDirectories()
            dump.saveTo(mergedDumpFile)
            fail("Expected api file did not exist. Generating: $mergedDumpFile")
        }
        val mergedDump = KlibDump.from(mergedDumpFile)
        mergedDump.remove(dump.targets)
        mergedDump.merge(dump)
        assertEqualsToFile(mergedDumpFile, StringBuilder().also { mergedDump.saveTo(it) })
    }

    private val NATIVE_ENABLED = System.getProperty("native.enabled")?.toBoolean() ?: false

    private fun getKlibFile(base: File, namePattern: String, kotlinVersion: String?): File =
        getLibFile(base, namePattern, kotlinVersion, "klib")

    private fun pathInCreatedDirectory(path: String) = Path(path).absolute().normalize().createParentDirectories()
}