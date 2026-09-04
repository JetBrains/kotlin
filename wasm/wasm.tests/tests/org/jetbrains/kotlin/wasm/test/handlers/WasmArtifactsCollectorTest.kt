/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectivesImpl
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class WasmArtifactsCollectorTest {
    @Test
    fun `given transformed JavaScript module files when collecting artifacts then transformed content is used`() {
        val files = listOf(
            testFile("helper.js", "original js"),
            testFile("helper.mjs", "original mjs"),
        )
        val testDataRoot = File("/tmp/wasm-artifacts-collector-test")
        val originalTestFile = testDataRoot.resolve("testdata/test.kt")
        val directives = RegisteredDirectivesImpl(
            simpleDirectives = emptyList(),
            stringDirectives = mapOf(
                PATH_TO_TEST_DIR to listOf(testDataRoot.path),
                PATH_TO_ROOT_OUTPUT_DIR to listOf(testDataRoot.resolve("out").path),
                TEST_GROUP_OUTPUT_DIR_PREFIX to listOf("group"),
            ),
            valueDirectives = emptyMap(),
        )
        val module = TestModule(
            name = "main",
            files = files,
            allDependencies = emptyList(),
            directives = RegisteredDirectives.Empty,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        )
        val moduleStructure = object : TestModuleStructure() {
            override val modules: List<TestModule> = listOf(module)
            override val allDirectives: RegisteredDirectives = directives
            override val originalTestDataFiles: List<File> = listOf(originalTestFile)
        }
        val testServices = TestServices().apply {
            register(TestModuleStructure::class, moduleStructure)
            register(SourceFileProvider::class, TransformedSourceFileProvider())
        }
        val collector = object : WasmArtifactsCollector {
            override val testServices: TestServices = testServices
        }

        val artifacts = collector.collectJsArtifacts(originalTestFile, "dev")

        assertEquals(
            mapOf(
                "helper.js" to "transformed helper.js",
                "helper.mjs" to "transformed helper.mjs",
            ),
            (artifacts.jsFiles + artifacts.mjsFiles).associate { it.name to it.content },
        )
    }

    private fun testFile(name: String, content: String): TestFile = TestFile(
        relativePath = name,
        originalContent = content,
        originalFile = File(name),
        startLineNumberInOriginalFile = 0,
        isAdditional = false,
        directives = RegisteredDirectives.Empty,
    )

    private class TransformedSourceFileProvider : SourceFileProvider() {
        override val preprocessors: List<SourceFilePreprocessor> = emptyList()

        override fun getKotlinSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getJavaSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getAdditionalFilesDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getContentOfSourceFile(
            testFile: TestFile,
            preprocessorFilter: ((SourceFilePreprocessor) -> Boolean)?,
        ): String = "transformed ${testFile.name}"

        override fun getOrCreateRealFileForSourceFile(testFile: TestFile): File = error("Not used in this test")
    }
}
