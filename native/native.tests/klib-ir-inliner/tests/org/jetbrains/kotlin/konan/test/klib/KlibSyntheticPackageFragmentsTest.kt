/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.library.assertPackageFragmentsInKlib
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("klib")
class KlibSyntheticPackageFragmentsTest : AbstractNativeSimpleTest() {
    @Test
    fun `Regular KLIB`() {
        val sourcesDir = buildDir.resolve("sources").apply { mkdirs() }
        sourcesDir.resolve("a.kt").writeText(
            """
                package foo
            """.trimIndent()
        )
        sourcesDir.resolve("b.kt").writeText(
            """
                package foo.bar.baz
            """.trimIndent()
        )
        sourcesDir.resolve("c.kt").writeText("")
        val klib = compileToLibrary(sourcesDir)

        assertPackageFragmentsInKlib(klib.klibFile.toPath(), expectedPackageFqNames = setOf("", "foo", "foo.bar.baz"))

        val importsEmptyPackagesFile = buildDir.resolve("importsEmptyPackages.kt").apply {
            writeText(
                """
                    import foo.*
                    import foo.bar.baz.*

                    fun importsEmptyPackages() {}
                """.trimIndent()
            )
        }
        compileToLibrary(importsEmptyPackagesFile, klib)
    }

    @Test
    fun `C-interop unpacked KLIB`() = testCInteropKlib(produceUnpackedKlib = true)

    @Test
    fun `C-interop packed KLIB`() = testCInteropKlib(produceUnpackedKlib = false)

    private fun testCInteropKlib(produceUnpackedKlib: Boolean) {
        val modules = newSourceModules {
            addCInteropModule("cinterop") {
                defFileAddend("package = foo.bar.baz")
            }
        }

        modules.compileToKlibsViaCli(produceUnpackedKlibs = produceUnpackedKlib) { _, successKlib ->
            val klib = successKlib.resultingArtifact
            assertPackageFragmentsInKlib(klib.klibFile.toPath(), expectedPackageFqNames = setOf("foo.bar.baz"))

            val importsCInteropPackageFileName =
                if (produceUnpackedKlib) "importsUnpackedCInteropPackage.kt" else "importsPackedCInteropPackage.kt"
            val importsCInteropPackageFile = buildDir.resolve(importsCInteropPackageFileName).apply {
                writeText(
                    """
                        import foo.bar.baz.cinterop

                        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                        fun callsCInterop() = cinterop(0)
                    """.trimIndent()
                )
            }
            compileToLibrary(importsCInteropPackageFile, klib)
        }
    }
}
