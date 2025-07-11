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
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

class WasmWasiBoxTestHelperSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val boxTestRunFile = File("wasm/wasm.tests/wasiBoxTestRun.kt")
        return listOf(boxTestRunFile.toTestFile())
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
        return getAdditionalKotlinFiles(module.files.first().originalFile.parent).map { it.toTestFile() }
    }

    companion object {
        private const val COMMON_FILES_NAME = "_common"
        private const val COMMON_FILES_DIR = "_commonFiles/"
        private val COMMON_FILES_DIR_PATH = Path("wasm/wasm.tests/$COMMON_FILES_DIR")

        private fun getFilesInDirectoryByExtension(directory: Path, extension: String): List<Path> {
            if (!directory.isDirectory()) return emptyList()
            return directory.listDirectoryEntries("*.$extension")
        }

        private fun getAdditionalFiles(directory: Path, extension: String): List<Path> {
            val globalCommonFiles = getFilesInDirectoryByExtension(COMMON_FILES_DIR_PATH, extension)
            val localCommonFilePath = "$directory/${COMMON_FILES_NAME}.$extension"
            val localCommonFile = Path(localCommonFilePath).takeIf { it.exists() } ?: return globalCommonFiles
            return globalCommonFiles.plusElement(localCommonFile)
        }

        fun getAdditionalKotlinFiles(directory: Path): List<Path> {
            return getAdditionalFiles(directory, KotlinFileType.EXTENSION)
        }
    }
}
