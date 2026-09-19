/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.library.assertNoEmptyPackageFragmentsInKlib
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.io.path.writeText

class JsKlibEmptyPackageFragmentsTest {
    @TempDir
    lateinit var tmpDir: Path

    @Test
    fun testUnpackedKlib() = doTest(produceUnpackedKlib = true)

    @Test
    fun testPackedKlib() = doTest(produceUnpackedKlib = false)

    private fun doTest(produceUnpackedKlib: Boolean) {
        val sourcesDir = tmpDir.resolve("sources").createDirectories()
        val withDeclarations = sourcesDir.resolve("withDeclarations.kt").apply {
            writeText(
                """
                    package foo.bar.baz

                    fun baz() {}
                """.trimIndent()
            )
        }
        val withoutDeclarations = sourcesDir.resolve("withoutDeclarations.kt").apply {
            writeText(
                """
                    package foo.bar.empty
                """.trimIndent()
            )
        }
        val commonWithExpectDeclarations = sourcesDir.resolve("common.kt").apply {
            writeText(
                """
                    package foo.bar.actualized

                    expect fun actualized()
                """.trimIndent()
            )
        }
        val platformWithActualDeclarations = sourcesDir.resolve("platform.kt").apply {
            writeText(
                """
                    package foo.bar.actualized

                    actual fun actualized() {}
                """.trimIndent()
            )
        }

        val klibPath = if (produceUnpackedKlib) tmpDir.resolve("lib") else tmpDir.resolve("lib.klib")
        runJsCompiler {
            if (produceUnpackedKlib) {
                nopack = true
                outputDir = klibPath.pathString
            } else {
                outputDir = tmpDir.pathString
            }
            libraries = StandardLibrariesPathProviderForKotlinProject.defaultJsStdlib().path
            moduleName = "lib"
            irModuleName = "lib"
            multiPlatform = true
            commonSources = arrayOf(commonWithExpectDeclarations.absolutePathString())
            freeArgs = listOf(withDeclarations, withoutDeclarations, commonWithExpectDeclarations, platformWithActualDeclarations)
                .map { it.absolutePathString() }
        }

        assertNoEmptyPackageFragmentsInKlib(klibPath, expectedPackageFqNames = setOf("foo.bar.baz", "foo.bar.actualized"))

        val mainFile = sourcesDir.resolve("main.kt").apply {
            writeText(
                """
                    import foo.bar.actualized.actualized
                    import foo.bar.baz.baz

                    fun main() {
                        baz()
                        actualized()
                    }
                """.trimIndent()
            )
        }
        val dependencies = listOf(StandardLibrariesPathProviderForKotlinProject.defaultJsStdlib().path, klibPath.pathString)
            .joinToString(File.pathSeparator)
        val mainKlibPath = tmpDir.resolve("main")
        runJsCompiler {
            nopack = true
            outputDir = mainKlibPath.pathString
            libraries = dependencies
            moduleName = "main"
            irModuleName = "main"
            freeArgs = listOf(mainFile.absolutePathString())
        }
        runJsCompiler {
            irProduceJs = true
            includes = mainKlibPath.pathString
            outputDir = tmpDir.resolve("js").pathString
            libraries = dependencies
            moduleName = "main"
        }

        val importsEmptyPackageFile = sourcesDir.resolve("importsEmptyPackage.kt").apply {
            writeText(
                """
                    import foo.bar.empty.*

                    fun unused() {}
                """.trimIndent()
            )
        }
        val messageCollector = MessageCollectorImpl()
        runJsCompiler(messageCollector, expectedExitCode = ExitCode.COMPILATION_ERROR) {
            nopack = true
            outputDir = tmpDir.resolve("importsEmptyPackage").pathString
            libraries = dependencies
            moduleName = "importsEmptyPackage"
            irModuleName = "importsEmptyPackage"
            freeArgs = listOf(importsEmptyPackageFile.absolutePathString())
        }
        assertTrue(messageCollector.messages.any { "unresolved reference 'empty'" in it.message.lowercase() }, messageCollector.toString())
    }
}
