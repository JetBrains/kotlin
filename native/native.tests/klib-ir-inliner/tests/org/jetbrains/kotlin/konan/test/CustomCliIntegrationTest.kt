/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.asLibraryDependency
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

/**
 * Test class for CLI integration tests which could manually invoke CLI compiler(s).
 */
class CustomCliIntegrationTest : AbstractNativeSimpleTest() {
    // ISSUE: KT-88548
    @Test
    fun testIncompatibleThrowsOverrideAcrossMixedMetadataCompilers() {
        val commonDir = buildDir.resolve("commonMain").apply { createDirectory() }
        val commonSource = commonDir.resolve("MyInterface.kt").apply {
            writeText(
                """
                interface MyInterface {
                    @Throws(Exception::class)
                    fun willThrow()
                }
                """.trimIndent()
            )
        }
        val commonKlib = compileInterfaceWithGenericMetadataCompiler(commonSource, commonDir)

        val nativeDir = buildDir.resolve("nativeMain").apply { createDirectory() }
        val nativeSource = nativeDir.resolve("MyImpl.kt").apply {
            writeText(
                """
                class MyImpl : MyInterface {
                    @Throws(Exception::class)
                    override fun willThrow() {}
                }
                """.trimIndent()
            )
        }

        val exception = assertThrows<CompilationToolException> {
            compileToLibrary(
                sourcesDir = nativeSource,
                outputDir = nativeDir,
                freeCompilerArgs = TestCompilerArgs(listOf("-Xmetadata-klib")),
                dependencies = listOf(commonKlib.asLibraryDependency())
            )
        }
        assertTrue(exception.reason.contains("member overrides different '@Throws' filter from 'interface MyInterface : Any'."))
    }

    private fun compileInterfaceWithGenericMetadataCompiler(sourceFile: File, outputDir: File): TestCompilationArtifact.KLIB {
        val commonStdlib = ForTestCompileRuntime.stdlibCommonForTests()
        val destination = outputDir.resolve("out")

        CompilerTestUtil.executeCompilerAssertSuccessful(
            KotlinMetadataCompiler(),
            listOf(
                sourceFile.path,
                "-classpath", commonStdlib.path,
                "-d", destination.path,
                "-Xmetadata-klib",
                "-Xtarget-platform=Native",
            )
        )
        return TestCompilationArtifact.KLIB(destination)
    }
}
