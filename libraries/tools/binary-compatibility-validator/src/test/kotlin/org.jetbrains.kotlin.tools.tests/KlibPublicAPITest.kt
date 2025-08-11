/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.AbiToolsFactory
import org.jetbrains.kotlin.abi.tools.api.v2.AbiToolsV2
import org.jetbrains.kotlin.abi.tools.api.v2.KlibDump
import org.junit.Assume
import java.io.File
import java.util.ServiceLoader
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class KlibPublicAPITest {
    @JvmField
    val abiTools: AbiToolsV2 = ServiceLoader<AbiToolsFactory>.load(AbiToolsFactory::class.java).single().get().v2


    @Test
    fun jsWasmJsWasmWasiStdlib() {
        val filters = AbiFilters(
            emptySet(),
            setOf(
                "org.w3c.**",
                "org.khronos.webgl.**",
                "kotlinx.dom.**",
                "kotlinx.browser.**",
            ),
            emptySet(),
            emptySet()
        )

        val dump = regularDump(
            "kotlin-stdlib-js-wasm",
            "../../stdlib/build/libs",
            listOf("kotlin-stdlib-js", "kotlin-stdlib-wasm-js", "kotlin-stdlib-wasm-wasi"),
            filters
        )
        mergeAndCompare("kotlin-stdlib", dump)
    }

    @Test
    fun nativeStdlib() {
        Assume.assumeTrue("Skipped, to enable it, either pass `kotlin.native.enabled` gradle property (if running with Gradle; the preferred way, see `ReadMe.md`), or `-Dnative.enabled=true` (if using the generated JUnit config; discouraged as it won't rebuild stdlib artifacts).", NATIVE_ENABLED)
        val dump = nativeDump("kotlin-stdlib-native", "../../../kotlin-native/runtime/build/nativeStdlib")
        mergeAndCompare("kotlin-stdlib", dump)
    }

    private fun regularDump(apiFileBaseName: String, basePath: String, klibPatterns: List<String>, filters: AbiFilters = AbiFilters.EMPTY): KlibDump {
        val base = File(basePath).absoluteFile.normalize()

        val mergedDump = abiTools.createKlibDump().apply {
            for (klibPattern in klibPatterns) {
                val klibFile = getKlibFile(base, klibPattern, System.getProperty("kotlinVersion"))
                merge(abiTools.extractKlibAbi(klibFile, filters = filters))
            }
        }

        mergedDump.print(pathInCreatedDirectory("build/api/$apiFileBaseName.api").toFile())
        return mergedDump
    }

    private fun nativeDump(apiFileBaseName: String, klibPath: String): KlibDump {
        val nativeDumpPath = pathInCreatedDirectory("build/api/$apiFileBaseName.api")
        val nativeDump = abiTools.extractKlibAbi(File(klibPath))
        val nativeDumpText = StringBuilder()
        nativeDump.print(nativeDumpText)
        val uniqueNameHeader = "Library unique name: <stdlib>"
        val uniqueNameReplacement = "Library unique name: <kotlin>"
        val headerIndex = nativeDumpText.indexOf(uniqueNameHeader)
        try {
            assertTrue(headerIndex >= 0, "Expected header '$uniqueNameHeader' is missing in K/N dump file $nativeDumpPath")
            nativeDumpText.replace(headerIndex, headerIndex + uniqueNameHeader.length, uniqueNameReplacement)
        } finally {
            nativeDumpPath.writeText(nativeDumpText)
        }
        return abiTools.loadKlibDump(nativeDumpPath.toFile())
    }

    private fun mergeAndCompare(apiFileBaseName: String, dump: KlibDump) {
        val mergedDumpFile = File("klib-public-api").resolve("$apiFileBaseName.api")
        if (!mergedDumpFile.exists()) {
            mergedDumpFile.toPath().createParentDirectories()
            dump.print(mergedDumpFile)
            fail("Expected api file did not exist. Generating: $mergedDumpFile")
        }
        val mergedDump = abiTools.loadKlibDump(mergedDumpFile)
        mergedDump.remove(dump.targets)
        mergedDump.merge(dump)
        assertEqualsToFile(mergedDumpFile, StringBuilder().also { mergedDump.print(it) })
    }

    private val NATIVE_ENABLED = System.getProperty("native.enabled")?.toBoolean() ?: false

    private fun getKlibFile(base: File, namePattern: String, kotlinVersion: String?): File =
        getLibFile(base, namePattern, kotlinVersion, "klib")

    private fun pathInCreatedDirectory(path: String) = Path(path).absolute().normalize().createParentDirectories()
}