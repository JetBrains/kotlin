/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.nio.file.Path
import kotlin.io.path.*

class JsAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (JsEnvironmentConfigurationDirectives.NO_COMMON_FILES in module.directives) return emptyList()
        // Add the files only to common modules with no dependencies, otherwise they'll produce "IrSymbol is already bound"
        if (module.allDependencies.isNotEmpty()) {
            return emptyList()
        }
        return getAdditionalKotlinFiles(module.files.first().originalFile.parent).map { it.toTestFile() }
    }

    companion object {
        private const val COMMON_FILES_NAME = "_common"
        private const val COMMON_FILES_DIR = "_commonFiles/"
        private val COMMON_FILES_DIR_PATH = Path(JsEnvironmentConfigurator.TEST_DATA_DIR_PATH + "/" + COMMON_FILES_DIR)

        private fun getFilesInDirectoryByExtension(directory: Path, extension: String): List<Path> {
            if (!directory.isDirectory()) return emptyList()
            return directory.listDirectoryEntries("*.$extension")
        }

        private fun getAdditionalFiles(directory: Path, extension: String): List<Path> {
            val globalCommonFiles = getFilesInDirectoryByExtension(COMMON_FILES_DIR_PATH, extension)
            val localCommonFilePath = "$directory/$COMMON_FILES_NAME.$extension"
            val localCommonFile = Path(localCommonFilePath).takeIf { it.exists() } ?: return globalCommonFiles
            return globalCommonFiles.plusElement(localCommonFile)
        }

        fun getAdditionalKotlinFiles(directory: Path): List<Path> {
            return getAdditionalFiles(directory, KotlinFileType.EXTENSION)
        }

        fun getAdditionalJsFiles(directory: Path): List<Path> {
            return getAdditionalFiles(directory, JavaScript.EXTENSION)
        }
    }
}
