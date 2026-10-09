/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CopyJsFilesToOutputDirTest {
    @Test
    fun `given JS files in test modules then they are copied as written in the test data`(@TempDir outputDir: File) {
        val original = "export const backend = BACKEND_UNDER_TEST; // <!DIAGNOSTIC!>kept as written<!>"

        facade.copyJs(listOf(module("main", "helper.mjs" to original, "lib.js" to "var x = 1;")), outputDir)

        assertEquals(original, outputDir.resolve("helper.mjs").readText())
        assertEquals("var x = 1;", outputDir.resolve("lib.js").readText())
    }

    @Test
    fun `given two modules providing the same JS file with different content then the batch is rejected`(@TempDir outputDir: File) {
        val modules = listOf(module("first", "helper.mjs" to "one"), module("second", "helper.mjs" to "two"))

        val error = assertThrows(TestInfrastructureException::class.java) { facade.copyJs(modules, outputDir) }

        assertTrue("Conflicting JS companion file 'helper.mjs'" in error.message.orEmpty(), error.message)
    }

    private val facade = object : AbstractWasmSecondStageGroupingFacade(TestServices()) {
        fun copyJs(modules: List<TestModule>, outputDir: File) = copyJsFilesToOutputDir(modules, outputDir)

        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm = error("Not used in this test")

        override fun TestModule.collectDependencies(
            testServices: TestServices,
            compilationStage: CompilationStage,
        ): DependencyPaths = error("Not used in this test")
    }

    private fun module(name: String, vararg files: Pair<String, String>): TestModule = TestModule(
        name = name,
        files = files.map { [fileName, content] ->
            TestFile(
                relativePath = fileName,
                originalContent = content,
                originalFile = File(fileName),
                startLineNumberInOriginalFile = 0,
                isAdditional = false,
                directives = RegisteredDirectives.Empty,
            )
        },
        allDependencies = emptyList(),
        directives = RegisteredDirectives.Empty,
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    )
}
