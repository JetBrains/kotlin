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
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.detectPackage
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView
import org.jetbrains.kotlin.test.services.sourceProviders.getSourceContent
import org.jetbrains.kotlin.test.testInfraError
import java.io.File

private val WASI_BOX_METHOD_REGEX =
    Regex("""(^|\n)(?!(?:\w+\s+)*suspend\s)(?:\w+\s+)*\bfun\s+box\(\)\s*(?::\s*String|=)""")

/**
 * Returns whether [file] has the synchronous box shape supported by the WASI helper.
 *
 * Module-structure source providers use [SourceContentView.ORIGINAL] because they run before the complete module
 * structure is registered. Later artifact decisions can opt into [SourceContentView.TRANSFORMED] explicitly.
 */
internal fun containsWasiBoxMethod(
    file: TestFile,
    sourceContentView: SourceContentView = SourceContentView.ORIGINAL,
    sourceFileProvider: SourceFileProvider? = null,
): Boolean {
    if (!file.isKtFile) return false
    val content = getSourceContent(file, sourceContentView, sourceFileProvider)
    return WASI_BOX_METHOD_REGEX.containsMatchIn(content)
}

internal fun findWasiBoxMethodFile(
    files: List<TestFile>,
    sourceContentView: SourceContentView = SourceContentView.ORIGINAL,
    sourceFileProvider: SourceFileProvider? = null,
): TestFile? {
    val filesWithBoxMethod = files.filter {
        containsWasiBoxMethod(it, sourceContentView, sourceFileProvider)
    }
    return when (filesWithBoxMethod.size) {
        0 -> null
        1 -> filesWithBoxMethod.single()
        else -> testInfraError(
            "The WASI box helper requires exactly one source file with a synchronous `box()` function, but found " +
                    "${filesWithBoxMethod.joinToString { it.relativePath }}."
        )
    }
}

/**
 * Adds the `wasiBoxTestRun.kt` helper to every module that defines `box()`. The helper provides the `runBoxTest` and
 * `startTest` entry points used to run a standalone WASI box test.
 *
 * The helper is added unconditionally because this provider runs before the grouping stage decides whether a test is
 * linked on its own or as part of a batch. Grouped batches use their generated driver instead, so the helper is not
 * included in those binaries and its `startTest` export does not conflict with the driver's export.
 */
class WasmWasiBoxTestHelperSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        // ModuleStructureExtractor invokes this provider before it registers the structure in TestServices.
        val fileWithBoxFun = findWasiBoxMethodFile(module.files, SourceContentView.ORIGINAL) ?: return emptyList()

        val p = detectPackage(fileWithBoxFun, SourceContentView.ORIGINAL)

        val boxTestRunFile = this::class.java.classLoader.getResource("wasiAdditionalFiles/wasiBoxTestRun.kt")!!
        val boxTestRunTestFile = boxTestRunFile.toTestFile()

        // no package
        if (p == null) return listOf(boxTestRunTestFile)

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
        if (module.allDependencies.isNotEmpty() &&
            // This optimization (don't add additional files to modules with dependencies) should not be done
            // for tests which golden data depends on sourcemaps, for ex, stepping tests.
            WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP !in module.directives
        ) {
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
