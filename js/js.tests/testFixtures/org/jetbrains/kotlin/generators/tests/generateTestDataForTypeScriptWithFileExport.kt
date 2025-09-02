/*
 * Copyright 2010-2025 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import java.io.File

private const val EXPORT_FILE_DIR_SUFIX = "-in-exported-file"

fun generateTypeScriptJsExportOnFiles(jsTestsDirPath: String) {
    val jsTestsDir = File(jsTestsDirPath)
    val directoriesToProcess = jsTestsDir.listFiles { file: File ->
        file.isDirectory &&
                !file.path.endsWith("selective-export") &&
                !file.path.endsWith("implicit-export") &&
                !file.path.endsWith("inheritance") &&
                !file.path.endsWith("strict-implicit-export") &&
                !file.path.endsWith("suspend-functions") &&
                !file.path.endsWith("js_export_default") &&
                !file.path.endsWith("private-primary-constructor") &&
                !file.path.endsWith(EXPORT_FILE_DIR_SUFIX)
    } ?: emptyArray()

    directoriesToProcess.forEach { dir ->
        generateJsExportOnFile(dir, jsTestsDir)
    }
}

private fun generateJsExportOnFile(sourceDir: File, targetDir: File): File {
    val outputDir = File(targetDir, "${sourceDir.name}$EXPORT_FILE_DIR_SUFIX")
    val filesToCopy = sourceDir.walkTopDown().filter { it.isFile }

    for (file in filesToCopy) {
        val relativePath = file.relativeTo(sourceDir).path
        val outputFile = File(outputDir, relativePath)

        if (file.name.endsWith(".kt")) {
            var isFirstLine = true
            val content = file.readLines()
            val processed = content.joinToString("\n") { line ->
                when {
                    isFirstLine -> {
                        isFirstLine = false
                        "${GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX} generateTestDataForTypeScriptWithFileExport.kt\n${GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX}\n\n$line"
                    }
                    line.contains("// FILE") -> "$line\n\n@file:JsExport"
                    else -> line.replace("@JsExport(?!.)".toRegex(), "")
                }
            }
            writeFile(outputFile, processed)
        } else {
            writeFile(outputFile, file.readText())
        }
    }

    return outputDir
}

fun writeFile(testSourceFile: File, generatedCode: String) {
    GeneratorsFileUtil.writeFileIfContentChanged(testSourceFile, generatedCode, false)
}
