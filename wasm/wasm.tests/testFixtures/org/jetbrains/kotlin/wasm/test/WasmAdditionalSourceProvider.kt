/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

/**
 * Attaches `wasiBoxTestRun.kt` — the `runBoxTest`/`startTest` glue an isolated WASI box run is driven through — to
 * every module with a `box()`.
 *
 * The attachment is deliberately unconditional, batched tests included, and must stay that way: whether a test's
 * per-test KLIB ends up as the `-Xinclude` main module (needing this glue) or as an ordinary `-libraries` dependency
 * is only decided at the grouping stage — a non-isolated test that merely ends up alone in its batch goes through
 * `WasmInProcessSecondStageFacade.doIsolated`, where the runner calls this file's `runBoxTest()` — while this provider
 * runs at file-generation time, before batch sizes exist. Gating it on isolation would silently break exactly those
 * lone non-isolated tests.
 *
 * Nor does the unconditional `@WasmExport startTest` collide with the grouped driver's export of the same name: a
 * library KLIB's unreferenced declarations never enter the link, so a grouped binary carries no trace of this file
 * (verified at byte level, and enforced per run by `assertDriverOwnsStartTestExport`). See
 * `WasmWasiGroupedTestsExportedEntryPointGenerator` for the full account.
 */
class WasmWasiBoxTestHelperSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val fileWithBoxFun = module.files.singleOrNull {
            it.isKtFile && it.originalContent.contains(Regex("(^|\\n)(?:\\w+\\s+)*\\bfun\\s+box\\(\\)\\s*(?::\\s*String|=)"))
        }

        // no box function
        if (fileWithBoxFun == null) return emptyList()

        val matchResult = Regex("^package\\s+([\\w.]+)", RegexOption.MULTILINE).find(fileWithBoxFun.originalContent)

        val boxTestRunFile = this::class.java.classLoader.getResource("wasiAdditionalFiles/wasiBoxTestRun.kt")!!
        val boxTestRunTestFile = boxTestRunFile.toTestFile()

        // no package
        if (matchResult == null) return listOf(boxTestRunTestFile)

        val p = matchResult.groupValues[1]
        return listOf(
            TestFile(
                boxTestRunTestFile.name,
                boxTestRunFile.readText().replace("box()", "$p.box()"),
                originalFile = boxTestRunTestFile.originalFile,
                startLineNumberInOriginalFile = 0,
                isAdditional = true,
                directives = RegisteredDirectives.Empty
            )
        )
    }
}

class WasmAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (WasmEnvironmentConfigurationDirectives.NO_COMMON_FILES in module.directives) return emptyList()
        // Add the files only to modules with no dependencies to avoid duplicates in case of multiple `// MODULE` test directives.
        if (module.allDependencies.isNotEmpty()) {
            return emptyList()
        }
        return getAdditionalGlobalFiles() + getAdditionalLocalFiles(module.files.first().originalFile.parent)
    }

    private fun getAdditionalGlobalFiles(): List<TestFile> {
        return GLOBAL_COMMON_FILES.map { this::class.java.classLoader.getResource(it)!!.toTestFile() }
    }

    private fun getAdditionalLocalFiles(directory: String): List<TestFile> {
        val localCommonFilePath = "$directory/$COMMON_FILES_NAME.${KotlinFileType.EXTENSION}"
        val localCommonFile = File(localCommonFilePath).takeIf { it.exists() }
        return listOfNotNull(localCommonFile?.toTestFile())
    }

    companion object {
        private const val COMMON_FILES_NAME = "_common"
        private val GLOBAL_COMMON_FILES = listOf("arrayAsserts.kt", "asserts.kt", "fail.kt").map { "commonFiles/$it" }
    }
}
