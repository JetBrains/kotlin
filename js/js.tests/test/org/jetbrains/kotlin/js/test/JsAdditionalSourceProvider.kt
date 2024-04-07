/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File
import java.io.FileFilter

class JsAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
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
        private const val COMMON_FILES_DIR_PATH = JsEnvironmentConfigurator.TEST_DATA_DIR_PATH + "/" + COMMON_FILES_DIR

        private fun getFilesInDirectoryByExtension(directory: String, extension: String): List<String> {
            val dir = File(directory)
            if (!dir.isDirectory) return emptyList()

            return dir.listFiles(FileFilter { it.extension == extension })?.map { it.absolutePath } ?: emptyList()
        }

        private fun getAdditionalFiles(directory: String, extension: String): List<File> {
            val globalCommonFiles = getFilesInDirectoryByExtension(COMMON_FILES_DIR_PATH, extension).map { File(it) }
            val localCommonFilePath = "$directory/$COMMON_FILES_NAME.$extension"
            val localCommonFile = File(localCommonFilePath).takeIf { it.exists() } ?: return globalCommonFiles
            return globalCommonFiles + localCommonFile
        }

        fun getAdditionalKotlinFiles(directory: String): List<File> {
            return getAdditionalFiles(directory, KotlinFileType.EXTENSION)
        }

        fun getAdditionalJsFiles(directory: String): List<File> {
            return getAdditionalFiles(directory, JavaScript.EXTENSION)
        }
    }
}