/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.utils.StringUtilsKt
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.math.min

@Tag("klib")
@OptIn(ExperimentalLibraryAbiReader::class)
class NativeLibraryAbiReaderWithManifestTest : AbstractNativeSimpleTest() {
    @Test
    fun testRenderingAbiWithAndWithoutManifestInfo() {
        val sourceFile = buildDir.resolve("source.kt").apply { writeText("fun foo() = Unit") }
        val libraryFile = compileToLibrary(sourceFile).klibFile

        val libraryAbi = LibraryAbiReader.readAbiInfo(libraryFile)
        val mostRecentSupportedSignatureVersion = libraryAbi.signatureVersions
            .filter { it.isSupportedByAbiReader }
            .maxByOrNull { it.versionNumber }!!

        val abiDumpWithoutManifest = LibraryAbiRenderer.render(
            libraryAbi,
            AbiRenderingSettings(
                renderedSignatureVersion = mostRecentSupportedSignatureVersion,
                renderManifest = false
            )
        ).filterOutShowManifestPropertiesFlag()

        val abiDumpWithManifest = LibraryAbiRenderer.render(
            libraryAbi,
            AbiRenderingSettings(
                renderedSignatureVersion = mostRecentSupportedSignatureVersion,
                renderManifest = true
            )
        ).filterOutShowManifestPropertiesFlag()

        val manifestDump = removeCommonLines(abiDumpWithoutManifest, abiDumpWithManifest)
        validateManifest(manifestDump, libraryAbi.manifest)
    }

    // TODO: Migrate this to ABI dump parser when it will become a part of ABI reader API.
    companion object {
        private fun String.filterOutShowManifestPropertiesFlag(): String =
            lineSequence().filter { "Show manifest properties:" !in it }.joinToString("\n")

        private fun removeCommonLines(abiDumpWithoutManifest: String, abiDumpWithManifest: String): String {
            val prefixLength = commonPrefixLength(abiDumpWithoutManifest, abiDumpWithManifest)
            val suffixLength = commonSuffixLength(abiDumpWithoutManifest, abiDumpWithManifest)

            val abiDumpWithoutManifestRemainder = abiDumpWithoutManifest.drop(prefixLength).dropLast(suffixLength)
            assertTrue(abiDumpWithoutManifestRemainder.isEmpty()) {
                """
                ABI dump without manifest has unexpected lines (${abiDumpWithoutManifestRemainder.lines().size}):
                $abiDumpWithoutManifestRemainder
            """.trimIndent()
            }

            val abiDumpWithManifestRemainder = abiDumpWithManifest.drop(prefixLength).dropLast(suffixLength)
            assertTrue(abiDumpWithManifestRemainder.isNotEmpty()) {
                "The manifest part of ABI dump with manifest is empty"
            }

            return abiDumpWithManifestRemainder
        }

        private fun validateManifest(manifestDump: String, manifest: LibraryManifest) {
            manifestDump.lines().map { line ->
                line.removePrefix("// ")
            }.forEach { line ->
                val [key, value] = line.split(": ", limit = 2).takeIf { it.size == 2 }
                    ?: fail { "Malformed line in manifest dump: $line" }

                fun assertManifestProperty(expected: String?) = assertTrue(expected == value) {
                    "Manifest property $key differs, expected = $expected but found = $value"
                }

                when (key) {
                    "Platform" -> assertManifestProperty(manifest.platform)
                    "Native targets" -> assertManifestProperty(manifest.dumpNativeTargets())
                    "Compiler version" -> assertManifestProperty(manifest.compilerVersion)
                    "ABI version" -> assertManifestProperty(manifest.abiVersion)
                    "IR provider" -> assertManifestProperty(manifest.irProviderName)
                    else -> fail { "Unexpected manifest property: $line" }
                }
            }
        }

        private fun LibraryManifest.dumpNativeTargets(): String =
            platformTargets.filterIsInstance<LibraryTarget.Native>().joinToString { it.name }

        private fun commonPrefixLength(s1: CharSequence, s2: CharSequence): Int {
            val minLength = min(s1.length, s2.length)
            var i = 0
            while (i < minLength) {
                if (s1[i] != s2[i]) {
                    break
                }
                i++
            }
            return i
        }

        private fun commonSuffixLength(s1: CharSequence, s2: CharSequence): Int {
            val s1Length = s1.length
            val s2Length = s2.length
            if (s1Length == 0 || s2Length == 0) return 0
            var i = 0
            while (i < s1Length && i < s2Length) {
                if (s1[s1Length - i - 1] != s2[s2Length - i - 1]) {
                    break
                }
                i++
            }
            return i
        }
    }
}
