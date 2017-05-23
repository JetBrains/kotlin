/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.facade

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.backend.common.output.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.sourceMap.JsSourceGenerationVisitor
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilder
import org.jetbrains.kotlin.js.translate.general.FileTranslationResult
import org.jetbrains.kotlin.js.util.TextOutput
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import java.util.*

abstract class TranslationResult protected constructor(val diagnostics: Diagnostics) {
    class Fail(diagnostics: Diagnostics) : TranslationResult(diagnostics)

    class Success(
            private val config: JsConfig,
            private val files: List<KtFile>,
            val program: JsProgram,
            diagnostics: Diagnostics,
            private val importedModules: List<String>,
            val moduleDescriptor: ModuleDescriptor,
            val bindingContext: BindingContext,
            val metadataHeader: ByteArray?,
            val fileTranslationResults: Map<KtFile, FileTranslationResult>
    ) : TranslationResult(diagnostics) {
        @Suppress("unused") // Used in kotlin-web-demo in WebDemoTranslatorFacade
        fun getCode(): String = getCode(TextOutputImpl(), sourceMapBuilder = null)

        fun getOutputFiles(outputFile: File, outputPrefixFile: File?, outputPostfixFile: File?): OutputFileCollection {
            val output = TextOutputImpl()
            val sourceMapBuilder =
                    if (config.configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP))
                        SourceMap3Builder(outputFile, output, SourceMapBuilderConsumer())
                    else null

            val code = getCode(output, sourceMapBuilder)
            val prefix = outputPrefixFile?.readText() ?: ""
            val postfix = outputPostfixFile?.readText() ?: ""
            val sourceFiles = files.map {
                val virtualFile = it.originalFile.virtualFile

                when {
                    virtualFile == null -> File(it.name)
                    else -> VfsUtilCore.virtualToIoFile(virtualFile)
                }
            }

            val jsFile = SimpleOutputFile(sourceFiles, outputFile.name, prefix + code + postfix)
            val outputFiles = arrayListOf<OutputFile>(jsFile)

            if (config.configuration.getBoolean(JSConfigurationKeys.META_INFO)) {
                val metaFileName = KotlinJavascriptMetadataUtils.replaceSuffix(outputFile.name)
                val moduleDescription = JsModuleDescriptor(
                    name = config.moduleId,
                    data = moduleDescriptor,
                    kind = config.moduleKind,
                    imported = importedModules
                )
                val metaFileContent = KotlinJavascriptSerializationUtil.metadataAsString(bindingContext, moduleDescription)
                val sourceFilesForMetaFile = ArrayList(sourceFiles)
                val jsMetaFile = SimpleOutputFile(sourceFilesForMetaFile, metaFileName, metaFileContent)
                outputFiles.add(jsMetaFile)

                KotlinJavascriptSerializationUtil.toContentMap(bindingContext, moduleDescriptor).forEach {
                    // TODO Add correct source files
                    outputFiles.add(SimpleOutputBinaryFile(emptyList(), config.moduleId + VfsUtilCore.VFS_SEPARATOR_CHAR + it.key, it.value))
                }
            }

            if (sourceMapBuilder != null) {
                sourceMapBuilder.skipLinesAtBeginning(StringUtil.getLineBreakCount(prefix))
                val sourceMapFile = SimpleOutputFile(sourceFiles, sourceMapBuilder.outFile.name, sourceMapBuilder.build())
                outputFiles.add(sourceMapFile)
            }

            return SimpleOutputFileCollection(outputFiles)
        }

        private fun getCode(output: TextOutput, sourceMapBuilder: SourceMapBuilder?): String {
            program.accept(JsSourceGenerationVisitor(output, sourceMapBuilder))
            return output.toString()
        }
    }
}
