/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.asLibraryDependency
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToExecutableInOneStage
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleModule
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.library.assertNoEmptyPackageFragmentsInKlib
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("klib")
class KlibEmptyPackageFragmentsTest : AbstractNativeSimpleTest() {
    @Test
    fun `Regular unpacked KLIB`() = testRegularKlib(produceUnpackedKlib = true)

    @Test
    fun `Regular packed KLIB`() = testRegularKlib(produceUnpackedKlib = false)

    @Test
    fun `C-interop unpacked KLIB`() = testCInteropKlib(produceUnpackedKlib = true)

    @Test
    fun `C-interop packed KLIB`() = testCInteropKlib(produceUnpackedKlib = false)

    private fun testRegularKlib(produceUnpackedKlib: Boolean) {
        val sourcesDir = buildDir.resolve("regular-sources").apply { mkdirs() }
        sourcesDir.resolve("withDeclarations.kt").writeText(
            """
                package foo.bar.baz

                fun baz() {}
            """.trimIndent()
        )
        sourcesDir.resolve("withoutDeclarations.kt").writeText(
            """
                package foo.bar.empty
            """.trimIndent()
        )

        val compilerArgs = if (produceUnpackedKlib) TestCompilerArgs("-nopack") else TestCompilerArgs.EMPTY
        val testCase = generateTestCaseWithSingleModule(sourcesDir, compilerArgs)

        val klibPath = buildDir.resolve(if (produceUnpackedKlib) "regular" else "regular.klib")
        val klib = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = emptySet(),
            expectedArtifact = KLIB(klibPath)
        ).result.assertSuccess().resultingArtifact

        assertNoEmptyPackageFragmentsInKlib(klib.klibFile.toPath(), expectedPackageFqNames = setOf("foo.bar.baz"))

        val mainFile = buildDir.resolve("main.kt").apply {
            writeText(
                """
                    import foo.bar.baz.baz

                    fun main() = baz()
                """.trimIndent()
            )
        }
        compileToExecutableInOneStage(generateTestCaseWithSingleModule(mainFile), klib.asLibraryDependency()).assertSuccess()
    }

    private fun testCInteropKlib(produceUnpackedKlib: Boolean) {
        val modules = newSourceModules {
            addCInteropModule("cinterop") {
                defFileAddend("package = foo.bar.baz")
            }
        }

        modules.compileToKlibsViaCli(produceUnpackedKlibs = produceUnpackedKlib) { _, successKlib ->
            assertNoEmptyPackageFragmentsInKlib(
                successKlib.resultingArtifact.klibFile.toPath(),
                expectedPackageFqNames = setOf("foo.bar.baz")
            )
        }
    }
}
