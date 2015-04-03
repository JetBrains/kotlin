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

import com.google.dart.compiler.backend.js.ast.JsProgram
import com.google.dart.compiler.util.TextOutput
import com.google.dart.compiler.util.TextOutputImpl
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFile
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.config.Config
import org.jetbrains.kotlin.js.sourceMap.JsSourceGenerationVisitor
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilder
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.fileUtils.readTextOrEmpty
import java.io.File
import java.util.ArrayList

public abstract class TranslationResult protected (public val diagnostics: Diagnostics) {

    public class Fail(diagnostics: Diagnostics) : TranslationResult(diagnostics)

    public class Success(
            private val config: Config,
            private val files: List<JetFile>,
            public val program: JsProgram,
            diagnostics: Diagnostics,
            private val moduleDescriptor: ModuleDescriptor
    ) : TranslationResult(diagnostics) {
        public fun getCode(): String = getCode(TextOutputImpl(), sourceMapBuilder = null)

        public fun getOutputFiles(outputFile: File, outputPrefixFile: File?, outputPostfixFile: File?): OutputFileCollection {
            val output = TextOutputImpl()
            val sourceMapBuilder = when {
                config.isSourcemap() -> SourceMap3Builder(outputFile, output, SourceMapBuilderConsumer())
                else -> null
            }

            val code = getCode(output, sourceMapBuilder)
            val prefix = outputPrefixFile?.readTextOrEmpty() ?: ""
            val postfix = outputPostfixFile?.readTextOrEmpty() ?: ""
            val sourceFiles = files.map {
                val virtualFile = it.getOriginalFile().getVirtualFile()

                when {
                    virtualFile == null -> File(it.getName())
                    else -> VfsUtilCore.virtualToIoFile(virtualFile)
                }
            }

            val jsFile = SimpleOutputFile(sourceFiles, outputFile.getName(), prefix + code + postfix)
            val outputFiles = arrayListOf(jsFile)

            config.getMetaInfo()?.let {
                val metaFile = File(it)
                val metaFileContent = KotlinJavascriptSerializationUtil.metadataAsString(config.getModuleId(), moduleDescriptor)
                val sourceFilesForMetaFile = ArrayList<File>(sourceFiles)
                val jsMetaFile = SimpleOutputFile(sourceFilesForMetaFile, metaFile.getName(), metaFileContent)
                outputFiles.add(jsMetaFile)
            }

            if (sourceMapBuilder != null) {
                sourceMapBuilder.skipLinesAtBeginning(StringUtil.getLineBreakCount(prefix))
                val sourceMapFile = SimpleOutputFile(sourceFiles, sourceMapBuilder.getOutFile().getName(), sourceMapBuilder.build())
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
