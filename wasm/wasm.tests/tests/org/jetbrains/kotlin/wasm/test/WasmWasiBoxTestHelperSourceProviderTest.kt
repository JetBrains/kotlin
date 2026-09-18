/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File

class WasmWasiBoxTestHelperSourceProviderTest {
    @Test
    fun `given a Kotlin String box function then it is eligible for the WASI helper`() {
        assertTrue(containsWasiBoxMethod(testFile("fun box(): String = \"OK\"")))
    }

    @Test
    fun `given a non Kotlin file containing box then it is not eligible for the WASI helper`() {
        assertFalse(containsWasiBoxMethod(testFile("fun box() { return 'OK'; }", "test.mjs")))
    }

    @Test
    fun `given a Unit box function then it is not eligible for the WASI helper`() {
        assertFalse(containsWasiBoxMethod(testFile("fun box() {}")))
    }

    @Test
    fun `given a suspend box function then it is not eligible for the WASI helper`() {
        assertFalse(containsWasiBoxMethod(testFile("suspend fun box(): String = \"OK\"")))
    }

    @Test
    fun `given transformed source then WASI helper inspection uses the selected source view`() {
        val file = testFile("fun notBox(): String = \"OK\"")
        val sourceFileProvider = StubSourceFileProvider("fun box(): String = \"OK\"")

        assertFalse(containsWasiBoxMethod(file, SourceContentView.ORIGINAL, sourceFileProvider))
        assertTrue(containsWasiBoxMethod(file, SourceContentView.TRANSFORMED, sourceFileProvider))
    }

    @Test
    fun `given an inferred non String expression body then the helper enforces its String contract`() {
        assertTrue(containsWasiBoxMethod(testFile("fun box() = 42")))

        val helper = javaClass.classLoader
            .getResource("wasiAdditionalFiles/wasiBoxTestRun.kt")!!
            .readText()
        assertTrue("val boxResult: String = box()" in helper, helper)
    }

    @Test
    fun `given multiple eligible box functions then helper selection is rejected as ambiguous`() {
        val first = testFile("fun box(): String = \"OK\"", "first.kt")
        val second = testFile("fun box(): String = \"OK\"", "second.kt")

        val error = assertThrows(TestInfrastructureException::class.java) {
            findWasiBoxMethodFile(listOf(first, second))
        }

        assertTrue("first.kt" in error.message.orEmpty(), error.message)
        assertTrue("second.kt" in error.message.orEmpty(), error.message)
    }

    private fun testFile(content: String, name: String = "test.kt"): TestFile = TestFile(
        relativePath = name,
        originalContent = content,
        originalFile = File(name),
        startLineNumberInOriginalFile = 0,
        isAdditional = false,
        directives = RegisteredDirectives.Empty,
    )

    private class StubSourceFileProvider(
        private val transformedContent: String,
    ) : SourceFileProvider() {
        override val preprocessors: List<SourceFilePreprocessor> = emptyList()

        override fun getKotlinSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getJavaSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getAdditionalFilesDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getContentOfSourceFile(
            testFile: TestFile,
            preprocessorFilter: ((SourceFilePreprocessor) -> Boolean)?,
        ): String = transformedContent

        override fun getOrCreateRealFileForSourceFile(testFile: TestFile): File = error("Not used in this test")
    }
}
