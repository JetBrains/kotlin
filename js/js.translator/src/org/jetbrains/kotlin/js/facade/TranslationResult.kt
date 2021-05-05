/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.facade

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.backend.common.output.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.NoOpSourceLocationConsumer
import org.jetbrains.kotlin.js.backend.SourceLocationConsumer
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.util.TextOutput
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import java.util.*

abstract class TranslationResult protected constructor(val diagnostics: Diagnostics) {
    class Fail(diagnostics: Diagnostics) : TranslationResult(diagnostics)

    abstract class SuccessBase(
        protected val config: JsConfig,
        protected val files: List<KtFile>,
        diagnostics: Diagnostics,
        protected val importedModules: List<String>,
        val moduleDescriptor: ModuleDescriptor,
        val bindingContext: BindingContext,
        val packageMetadata: Map<FqName, ByteArray>
    ) : TranslationResult(diagnostics) {

        abstract fun getOutputFiles(outputFile: File, outputPrefixFile: File?, outputPostfixFile: File?): OutputFileCollection

        protected val sourceFiles = files.map {
            val virtualFile = it.originalFile.virtualFile

            when {
                virtualFile == null -> File(it.name)
                else -> VfsUtilCore.virtualToIoFile(virtualFile)
            }
        }

        protected fun metadataFiles(outputFile: File): List<OutputFile> {
            if (config.configuration.getBoolean(JSConfigurationKeys.META_INFO)) {
                val metaFileName = KotlinJavascriptMetadataUtils.replaceSuffix(outputFile.name)
                val moduleDescription = JsModuleDescriptor(
                    name = config.moduleId,
                    data = moduleDescriptor,
                    kind = config.moduleKind,
                    imported = importedModules
                )
                val serializedMetadata = KotlinJavascriptSerializationUtil.SerializedMetadata(
                    packageMetadata,
                    moduleDescription,
                    config.configuration.languageVersionSettings,
                    config.configuration.get(CommonConfigurationKeys.METADATA_VERSION) as? JsMetadataVersion ?: JsMetadataVersion.INSTANCE
                )
                val metaFileContent = serializedMetadata.asString()
                val sourceFilesForMetaFile = ArrayList(sourceFiles)
                val jsMetaFile = SimpleOutputFile(sourceFilesForMetaFile, metaFileName, metaFileContent)

                return listOf(jsMetaFile) + serializedMetadata.serializedPackages().map { serializedPackage ->
                    kjsmFileForPackage(serializedPackage.fqName, serializedPackage.bytes)
                }
            } else {
                return emptyList()
            }
        }

        private fun kjsmFileForPackage(packageFqName: FqName, bytes: ByteArray): SimpleOutputBinaryFile {
            val ktFiles = (bindingContext.get(BindingContext.PACKAGE_TO_FILES, packageFqName) ?: emptyList())
            val sourceFiles = ktFiles.map { VfsUtilCore.virtualToIoFile(it.virtualFile) }
            val relativePath = config.moduleId +
                    VfsUtilCore.VFS_SEPARATOR_CHAR +
                    JsSerializerProtocol.getKjsmFilePath(packageFqName)
            return SimpleOutputBinaryFile(sourceFiles, relativePath, bytes)
        }
    }

    class Success(
        config: JsConfig,
        files: List<KtFile>,
        val program: JsProgram,
        diagnostics: Diagnostics,
        importedModules: List<String>,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        packageMetadata: Map<FqName, ByteArray>
    ) : SuccessBase(config, files, diagnostics, importedModules, moduleDescriptor, bindingContext, packageMetadata) {
        @Suppress("unused") // Used in kotlin-web-demo in WebDemoTranslatorFacade
        fun getCode(): String {
            val output = TextOutputImpl()
            getCode(output, sourceLocationConsumer = null)
            return output.toString()
        }

        override fun getOutputFiles(outputFile: File, outputPrefixFile: File?, outputPostfixFile: File?): OutputFileCollection {
            val output = TextOutputImpl()

            val sourceMapBuilder = SourceMap3Builder(outputFile, output, config.sourceMapPrefix)
            val sourceMapBuilderConsumer =
                if (config.configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)) {
                    val sourceMapContentEmbedding = config.sourceMapContentEmbedding
                    val pathResolver = SourceFilePathResolver.create(config)
                    SourceMapBuilderConsumer(
                        File("."),
                        sourceMapBuilder,
                        pathResolver,
                        sourceMapContentEmbedding == SourceMapSourceEmbedding.ALWAYS,
                        sourceMapContentEmbedding != SourceMapSourceEmbedding.NEVER
                    )
                } else {
                    null
                }

            getCode(output, sourceMapBuilderConsumer)
            if (sourceMapBuilderConsumer != null) {
                sourceMapBuilder.addLink()
            }
            val code = output.toString()

            val prefix = outputPrefixFile?.readText() ?: ""
            val postfix = outputPostfixFile?.readText() ?: ""

            val jsFile = SimpleOutputFile(sourceFiles, outputFile.name, prefix + code + postfix)
            val outputFiles = arrayListOf<OutputFile>(jsFile)

            outputFiles += metadataFiles(outputFile)

            if (sourceMapBuilderConsumer != null) {
                sourceMapBuilder.skipLinesAtBeginning(StringUtil.getLineBreakCount(prefix))
                val sourceMapFile = SimpleOutputFile(sourceFiles, sourceMapBuilder.outFile.name, sourceMapBuilder.build())
                outputFiles.add(sourceMapFile)
                sourceMapBuilder.addLink()
            }

            return SimpleOutputFileCollection(outputFiles)
        }

        private fun getCode(output: TextOutput, sourceLocationConsumer: SourceLocationConsumer?) {
            program.accept(JsToStringGenerationVisitor(output, sourceLocationConsumer ?: NoOpSourceLocationConsumer))
        }
    }

    class SuccessNoCode(
        config: JsConfig,
        files: List<KtFile>,
        diagnostics: Diagnostics,
        importedModules: List<String>,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        packageMetadata: Map<FqName, ByteArray>
    ) : SuccessBase(config, files, diagnostics, importedModules, moduleDescriptor, bindingContext, packageMetadata) {

        override fun getOutputFiles(outputFile: File, outputPrefixFile: File?, outputPostfixFile: File?): OutputFileCollection {
            return SimpleOutputFileCollection(metadataFiles(outputFile))
        }
    }
}
