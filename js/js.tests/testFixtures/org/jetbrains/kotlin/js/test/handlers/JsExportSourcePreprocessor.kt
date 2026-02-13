/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * For "whole file" JS export tests (when [isWholeFile] is `true`), removes `@JsExport` on individual declarations and adds
 * `@file:JsExport` at the beginning of each test file.
 *
 * For regular JS export tests, removes `@JsExport.Ignore` on top-level declarations.
 */
internal class JsExportSourcePreprocessor(
    testServices: TestServices,
    private val isWholeFile: Boolean,
) : ReversibleSourceFilePreprocessor(testServices) {
    companion object {
        private val JS_EXPORT_REGEX = Regex("^@JsExport(?!\\.Ignore)")
        private val JS_EXPORT_DEFAULT_REGEX = Regex("^@JsExport.Default")
        private val JS_EXPORT_IGNORE_REGEX = Regex("^@JsExport.Ignore")
    }

    override fun process(file: TestFile, content: String): String {
        if (DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR in file.directives
            || DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR in testServices.moduleStructure.allDirectives
        ) {
            return content
        }
        val lines = content.lines()
        val result = lines.joinToString("\n") { line ->
            when {
                isWholeFile && line.startsWith("// FILE") ->
                    "$line\n/*JsExportSourcePreprocessor-file*/@file:JsExport"
                isWholeFile && JS_EXPORT_REGEX.containsMatchIn(line) && !JS_EXPORT_DEFAULT_REGEX.containsMatchIn(line) ->
                    line.replace(JS_EXPORT_REGEX, "/*JsExportSourcePreprocessor|@JsExport*/")
                !isWholeFile && JS_EXPORT_IGNORE_REGEX.containsMatchIn(line) ->
                    line.replace(JS_EXPORT_IGNORE_REGEX, "/*JsExportSourcePreprocessor|@JsExport.Ignore*/")
                else -> line
            }
        }
        return result
    }

    override fun revert(file: TestFile, actualContent: String): String {
        if (DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR in file.directives
            || DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR in testServices.moduleStructure.allDirectives
        ) {
            return actualContent
        }
        return actualContent
            .replace("/*JsExportSourcePreprocessor|@JsExport*/", "@JsExport")
            .replace("/*JsExportSourcePreprocessor|@JsExport.Ignore*/", "@JsExport.Ignore")
            .replace("\n/*JsExportSourcePreprocessor-file*/@file:JsExport", "")
    }
}
