/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File
import java.io.FileFilter

class WasmWasiBoxTestHelperSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val fileWithBoxFun = module.files.singleOrNull {
            it.isKtFile && it.originalContent.contains(Regex("\\bfun\\s+box\\(\\)\\s*(?::\\s*String|=)"))
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
        // For multiplatform projects, add the files only to common modules with no dependencies.
        if (module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) &&
            module.allDependencies.isNotEmpty()) {
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
