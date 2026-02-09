/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_PROVIDER
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.utils.assertCompilerOutputHasKlibResolverIncompatibleAbiMessages
import org.jetbrains.kotlin.test.utils.patchManifestAsMap
import org.jetbrains.kotlin.test.utils.patchManifestToBumpAbiVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.collections.set
import kotlin.text.contains

@Tag("klib")
class KlibCliSanityTest : AbstractNativeSimpleTest() {
    @Test
    fun `Compiler consumes unpacked KLIBs passed via CLI arguments`() {
        val modules = newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            addRegularModule("c") { dependsOn("a") }
            addRegularModule("d") { dependsOn("b", "c", "a") }
        }

        modules.compileToKlibsViaCli()
    }

    @Test
    fun `Compiler consumes packed KLIBs passed via CLI arguments`() {
        val modules = newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            addRegularModule("c") { dependsOn("a") }
            addRegularModule("d") { dependsOn("b", "c", "a") }
        }

        modules.compileToKlibsViaCli(produceUnpackedKlibs = false)
    }

    @Test
    fun `Compiler warns on non-existent transitive dependency in depends= property of regular KLIB, v1`() =
        doTestWarnOnNonexistentTransitiveDependencyInManifestV1(lastModuleIsCInterop = false)

    @Test
    fun `Compiler warns on non-existent transitive dependency in depends= property of regular KLIB, v2`() =
        doTestWarnOnNonexistentTransitiveDependencyInManifestV2(lastModuleIsCInterop = false)

    @Test
    fun `Compiler warns on non-existent transitive dependency in depends= property of C-interop KLIB, v1`() =
        doTestWarnOnNonexistentTransitiveDependencyInManifestV1(lastModuleIsCInterop = true)

    @Test
    fun `Compiler warns on non-existent transitive dependency in depends= property of C-interop KLIB, v2`() =
        doTestWarnOnNonexistentTransitiveDependencyInManifestV2(lastModuleIsCInterop = true)

    private fun doTestWarnOnNonexistentTransitiveDependencyInManifestV1(lastModuleIsCInterop: Boolean) {
        var aKlib: KLIB? = null

        newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            if (lastModuleIsCInterop)
                addCInteropModule("c") { dependsOn("b") }
            else
                addRegularModule("c") { dependsOn("b") }
        }.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "a" -> aKlib = successKlib.resultingArtifact
                "b" -> aKlib!!.klibFile.delete() // remove transitive dependency `a`, so subsequent compilation of `c` would miss it.
                "c" -> {
                    val compilationToolCall = successKlib.loggedData as LoggedData.CompilationToolCall
                    assertEquals(ExitCode.OK, compilationToolCall.exitCode)
                    val warnings = compilationToolCall.toolOutput.lineSequence().filter { "KLIB resolver:" in it }.toList()
                    assertTrue(warnings.isEmpty())
                }
            }
        }
    }

    private fun doTestWarnOnNonexistentTransitiveDependencyInManifestV2(lastModuleIsCInterop: Boolean) {
        newSourceModules {
            addRegularModule("b")
            if (lastModuleIsCInterop)
                addCInteropModule("c") { dependsOn("b") }
            else
                addRegularModule("c") { dependsOn("b") }
        }.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "b" -> {
                    // Making cinterop add something to its `depends =` is tricky in these tests:
                    // for that, we need the same header file accessible to both `a` and `b`.
                    // Instead of overcomplicating the test engine, let's do a little shortcut:
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        properties[KLIB_PROPERTY_DEPENDS] += " a"
                    }
                    // In other words: unlike the previous test, don't compile-and-remove "a" at all,
                    // but just patch the manifest to mention it as if it existed.
                }
                "c" -> {
                    val compilationToolCall = successKlib.loggedData as LoggedData.CompilationToolCall
                    assertEquals(ExitCode.OK, compilationToolCall.exitCode)
                    val warnings = compilationToolCall.toolOutput.lineSequence().filter { "KLIB resolver:" in it }.toList()
                    assertTrue(warnings.isEmpty())
                }
            }
        }
    }

    @Test
    fun `Compiler rejects KLIB with unsupported ABI version`() {
        val modules = newSourceModules {
            addRegularModule("lib1")
            addRegularModule("lib2") { dependsOn("lib1") }
        }

        // Control compilation -- should finish successfully.
        modules.compileToKlibsViaCli()

        // Compilation with patched manifest -- should fail.
        try {
            modules.compileToKlibsViaCli { module, successKlib ->
                if (module.name == "lib1") {
                    patchManifestToBumpAbiVersion(JUnit5Assertions, successKlib.resultingArtifact.klibFile)
                }
            }

            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            assertCompilerOutputHasKlibResolverIncompatibleAbiMessages(
                assertions = JUnit5Assertions,
                compilerOutput = cte.reason,
                missingLibrary = "/klib-files.unpacked.transformed/lib1",
                baseDir = buildDir
            )
        }
    }

    @Test
    fun `Compiler consumes KLIB with known IR provider`() {
        doTestIrProviders(knownIrProvider = true)
    }

    @Test
    fun `Compiler rejects KLIB with unknown IR provider`() {
        try {
            doTestIrProviders(knownIrProvider = false)
            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("The library requires unknown IR provider"))
                throw cte
        }
    }

    private fun doTestIrProviders(knownIrProvider: Boolean) {
        newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
        }.compileToKlibsViaCli { module, successKlib ->
            if (module.name == "a") {
                patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                    properties[KLIB_PROPERTY_IR_PROVIDER] = if (knownIrProvider) KLIB_INTEROP_IR_PROVIDER_IDENTIFIER else "QWERTY"
                }
            }
        }
    }

}