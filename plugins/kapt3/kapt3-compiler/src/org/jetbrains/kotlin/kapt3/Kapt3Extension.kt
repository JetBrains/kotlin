/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3

import com.intellij.openapi.project.Project
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.Pretty
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.OUTPUT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.kapt3.AptMode.APT_ONLY
import org.jetbrains.kotlin.kapt3.AptMode.WITH_COMPILATION
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.KaptPaths
import org.jetbrains.kotlin.kapt3.base.ProcessorLoader
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation.Companion.KAPT_METADATA_EXTENSION
import org.jetbrains.kotlin.kapt3.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt3.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt3.base.util.info
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter.KaptStub
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import java.io.File
import java.io.StringWriter
import java.io.Writer
import javax.annotation.processing.Processor
import com.sun.tools.javac.util.List as JavacList

class ClasspathBasedKapt3Extension(
        paths: KaptPaths,
        options: Map<String, String>,
        javacOptions: Map<String, String>,
        annotationProcessorFqNames: List<String>,
        aptMode: AptMode,
        val useLightAnalysis: Boolean,
        correctErrorTypes: Boolean,
        mapDiagnosticLocations: Boolean,
        strictMode: Boolean,
        pluginInitializedTime: Long,
        logger: MessageCollectorBackedKaptLogger,
        compilerConfiguration: CompilerConfiguration
) : AbstractKapt3Extension(
    paths, options, javacOptions, annotationProcessorFqNames,
    aptMode, pluginInitializedTime, logger, correctErrorTypes, mapDiagnosticLocations, strictMode,
    compilerConfiguration
) {
    override val analyzePartially: Boolean
        get() = useLightAnalysis

    private var processorLoader: ProcessorLoader? = null

    override fun loadProcessors(): List<Processor> {
        return ProcessorLoader(paths, annotationProcessorFqNames, logger).also { this.processorLoader = it }.loadProcessors()
    }

    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>
    ): AnalysisResult? {
        try {
            return super.analysisCompleted(project, module, bindingTrace, files)
        } finally {
            processorLoader?.close()
        }
    }
}

abstract class AbstractKapt3Extension(
        val paths: KaptPaths,
        val options: Map<String, String>,
        val javacOptions: Map<String, String>,
        val annotationProcessorFqNames: List<String>,
        val aptMode: AptMode,
        val pluginInitializedTime: Long,
        val logger: MessageCollectorBackedKaptLogger,
        val correctErrorTypes: Boolean,
        val mapDiagnosticLocations: Boolean,
        val strictMode: Boolean,
        val compilerConfiguration: CompilerConfiguration
) : PartialAnalysisHandlerExtension() {
    private var annotationProcessingComplete = false

    private fun setAnnotationProcessingComplete(): Boolean {
        if (annotationProcessingComplete) return true

        annotationProcessingComplete = true
        return false
    }

    override fun doAnalysis(
            project: Project,
            module: ModuleDescriptor,
            projectContext: ProjectContext,
            files: Collection<KtFile>,
            bindingTrace: BindingTrace,
            componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (aptMode == APT_ONLY) {
            return AnalysisResult.EMPTY
        }

        return super.doAnalysis(project, module, projectContext, files, bindingTrace, componentProvider)
    }

    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>
    ): AnalysisResult? {
        if (setAnnotationProcessingComplete()) return null

        fun doNotGenerateCode() = AnalysisResult.Companion.success(BindingContext.EMPTY, module, shouldGenerateCode = false)

        logger.info { "Initial analysis took ${System.currentTimeMillis() - pluginInitializedTime} ms" }

        val bindingContext = bindingTrace.bindingContext
        if (aptMode.generateStubs) {
            logger.info { "Kotlin files to compile: " + files.map { it.virtualFile?.name ?: "<in memory ${it.hashCode()}>" } }

            contextForStubGeneration(project, module, bindingContext, files.toList()).use { context ->
                generateKotlinSourceStubs(context)
            }
        }

        if (!aptMode.runAnnotationProcessing) return doNotGenerateCode()

        val processors = loadProcessors()
        if (processors.isEmpty()) return if (aptMode != WITH_COMPILATION) doNotGenerateCode() else null

        val kaptContext = KaptContext(paths, false, logger, mapDiagnosticLocations, options, javacOptions)

        fun handleKaptError(error: KaptError): AnalysisResult {
            val cause = error.cause

            if (cause != null) {
                kaptContext.logger.exception(cause)
            }

            return AnalysisResult.compilationError(bindingTrace.bindingContext)
        }

        try {
            runAnnotationProcessing(kaptContext, processors)
        } catch (error: KaptBaseError) {
            val kind = when (error.kind) {
                KaptBaseError.Kind.EXCEPTION -> KaptError.Kind.EXCEPTION
                KaptBaseError.Kind.ERROR_RAISED -> KaptError.Kind.ERROR_RAISED
            }

            val cause = error.cause
            return handleKaptError(if (cause != null) KaptError(kind, cause) else KaptError(kind))
        } catch (error: KaptError) {
            return handleKaptError(error)
        } catch (thr: Throwable) {
            return AnalysisResult.internalError(bindingTrace.bindingContext, thr)
        } finally {
            kaptContext.close()
        }

        return if (aptMode != WITH_COMPILATION) {
            doNotGenerateCode()
        } else {
            AnalysisResult.RetryWithAdditionalJavaRoots(
                    bindingTrace.bindingContext,
                    module,
                    listOf(paths.sourcesOutputDir),
                    addToEnvironment = true)
        }
    }

    private fun runAnnotationProcessing(kaptContext: KaptContext, processors: List<Processor>) {
        if (!aptMode.runAnnotationProcessing) return

        val javaSourceFiles = paths.collectJavaSourceFiles()
        logger.info { "Java source files: " + javaSourceFiles.joinToString { it.canonicalPath } }

        val (annotationProcessingTime) = measureTimeMillis {
            kaptContext.doAnnotationProcessing(javaSourceFiles, processors)
        }

        logger.info { "Annotation processing took $annotationProcessingTime ms" }
    }

    private fun contextForStubGeneration(
            project: Project,
            module: ModuleDescriptor,
            bindingContext: BindingContext,
            files: List<KtFile>
    ): KaptContextForStubGeneration {
        val builderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)

        val targetId = TargetId(
                name = compilerConfiguration[CommonConfigurationKeys.MODULE_NAME] ?: module.name.asString(),
                type = "java-production")

        val generationState = GenerationState.Builder(
                project,
                builderFactory,
                module,
                bindingContext,
                files,
                compilerConfiguration
        ).targetId(targetId).build()

        val (classFilesCompilationTime) = measureTimeMillis {
            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)
        }

        val compiledClasses = builderFactory.compiledClasses
        val origins = builderFactory.origins

        logger.info { "Stubs compilation took $classFilesCompilationTime ms" }
        logger.info { "Compiled classes: " + compiledClasses.joinToString { it.name } }

        return KaptContextForStubGeneration(
            paths, false, logger, project, bindingContext, compiledClasses, origins, generationState,
            mapDiagnosticLocations, options, javacOptions
        )
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContextForStubGeneration) {
        val converter = ClassFileToSourceStubConverter(
            kaptContext,
            generateNonExistentClass = true,
            correctErrorTypes = correctErrorTypes,
            strictMode = strictMode
        )

        val (stubGenerationTime, kaptStubs) = measureTimeMillis {
            converter.convert()
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        logger.info { "Stubs for Kotlin classes: " + kaptStubs.joinToString { it.file.sourcefile.name } }

        saveStubs(kaptContext, kaptStubs)
        saveIncrementalData(kaptContext, logger.messageCollector, converter)
    }

    protected open fun saveStubs(kaptContext: KaptContext, stubs: List<KaptStub>) {
        for (kaptStub in stubs) {
            val stub = kaptStub.file
            val className = (stub.defs.first { it is JCTree.JCClassDecl } as JCTree.JCClassDecl).simpleName.toString()

            val packageName = stub.getPackageNameJava9Aware()?.toString() ?: ""
            val packageDir = if (packageName.isEmpty()) paths.stubsOutputDir else File(paths.stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val sourceFile = File(packageDir, "$className.java")
            sourceFile.writeText(stub.prettyPrint(kaptContext.context))

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile)
        }
    }

    protected open fun saveIncrementalData(
            kaptContext: KaptContextForStubGeneration,
            messageCollector: MessageCollector,
            converter: ClassFileToSourceStubConverter) {
        val incrementalDataOutputDir = paths.incrementalDataOutputDir ?: return

        val reportOutputFiles = kaptContext.generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        kaptContext.generationState.factory.writeAll(
                incrementalDataOutputDir,
                if (!reportOutputFiles) null else fun(file: OutputFile, sources: List<File>, output: File) {
                    val stubFileObject = converter.bindings[file.relativePath.substringBeforeLast(".class", missingDelimiterValue = "")]
                    if (stubFileObject != null) {
                        val stubFile = File(paths.stubsOutputDir, stubFileObject.name)
                        val lineMappingsFile = File(stubFile.parentFile, stubFile.nameWithoutExtension + KAPT_METADATA_EXTENSION)

                        for (file in listOf(stubFile, lineMappingsFile)) {
                            if (file.exists()) {
                                messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sources, file))
                            }
                        }
                    }

                    messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sources, output))
                }
        )
    }

    protected abstract fun loadProcessors(): List<Processor>
}

internal fun JCTree.prettyPrint(context: Context): String {
    return StringWriter().apply { PrettyWithWorkarounds(context, this, false).printStat(this@prettyPrint) }.toString()
}

private class PrettyWithWorkarounds(private val context: Context, val out: Writer, sourceOutput: Boolean) : Pretty(out, sourceOutput) {
    companion object {
        private const val ENUM = Flags.ENUM.toLong()
    }

    override fun print(s: Any) {
        out.write(s.toString())
    }

    override fun visitVarDef(tree: JCTree.JCVariableDecl) {
        if ((tree.mods.flags and ENUM) != 0L) {
            // Pretty does not print annotations for enum values for some reason
            printExpr(TreeMaker.instance(context).Modifiers(0, tree.mods.annotations))
        }

        super.visitVarDef(tree)
    }
}

private inline fun <T> measureTimeMillis(block: () -> T) : Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}
