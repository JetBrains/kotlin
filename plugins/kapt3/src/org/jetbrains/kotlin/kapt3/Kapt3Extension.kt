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
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAll
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.kapt3.diagnostic.ErrorsKapt3
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import java.io.File
import java.net.URLClassLoader
import java.util.*
import javax.annotation.processing.Processor
import com.sun.tools.javac.util.List as JavacList

class ClasspathBasedKapt3Extension(
        compileClasspath: List<File>,
        annotationProcessingClasspath: List<File>,
        javaSourceRoots: List<File>,
        sourcesOutputDir: File,
        classFilesOutputDir: File,
        stubsOutputDir: File,
        incrementalDataOutputDir: File?,
        options: Map<String, String>,
        aptOnly: Boolean,
        val useLightAnalysis: Boolean,
        correctErrorTypes: Boolean,
        pluginInitializedTime: Long,
        logger: KaptLogger
) : AbstractKapt3Extension(compileClasspath, annotationProcessingClasspath, javaSourceRoots, sourcesOutputDir,
                           classFilesOutputDir, stubsOutputDir, incrementalDataOutputDir, options,
                           aptOnly, pluginInitializedTime, logger, correctErrorTypes) {
    override val analyzePartially: Boolean
        get() = useLightAnalysis

    private var annotationProcessingClassLoader: URLClassLoader? = null

    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>
    ): AnalysisResult? {
        try {
            return super.analysisCompleted(project, module, bindingTrace, files)
        } finally {
            annotationProcessingClassLoader?.close()
        }
    }

    override fun loadProcessors(): List<Processor> {
        val classpath = annotationProcessingClasspath + compileClasspath
        val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray())
        this.annotationProcessingClassLoader = classLoader
        val processors = ServiceLoader.load(Processor::class.java, classLoader).toList()

        if (processors.isEmpty()) {
            logger.info("No annotation processors available, aborting")
        } else {
            logger.info { "Annotation processors: " + processors.joinToString { it.javaClass.canonicalName } }
        }

        return processors
    }
}

abstract class AbstractKapt3Extension(
        compileClasspath: List<File>,
        annotationProcessingClasspath: List<File>,
        val javaSourceRoots: List<File>,
        val sourcesOutputDir: File,
        val classFilesOutputDir: File,
        val stubsOutputDir: File,
        val incrementalDataOutputDir: File?,
        val options: Map<String, String>,
        val aptOnly: Boolean,
        val pluginInitializedTime: Long,
        val logger: KaptLogger,
        val correctErrorTypes: Boolean
) : PartialAnalysisHandlerExtension() {
    val compileClasspath = compileClasspath.distinct()
    val annotationProcessingClasspath = annotationProcessingClasspath.distinct()

    private var annotationProcessingComplete = false

    private fun setAnnotationProcessingComplete(): Boolean {
        if (annotationProcessingComplete) return true

        annotationProcessingComplete = true
        return false
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
        logger.info { "Kotlin files to compile: " + files.map { it.virtualFile?.name ?: "<in memory ${it.hashCode()}>" } }

        val processors = loadProcessors()
        if (processors.isEmpty()) return if (aptOnly) doNotGenerateCode() else null

        val (kaptContext, generationState) = compileStubs(project, module, bindingTrace.bindingContext, files.toList())

        try {
            generateKotlinSourceStubs(kaptContext, generationState)
            val javaSourceFiles = collectJavaSourceFiles()

            val (annotationProcessingTime) = measureTimeMillis {
                kaptContext.doAnnotationProcessing(
                        javaSourceFiles, processors, compileClasspath, annotationProcessingClasspath,
                        sourcesOutputDir, classFilesOutputDir)
            }

            logger.info { "Annotation processing took $annotationProcessingTime ms" }
        } catch (thr: Throwable) {
            if (thr !is KaptError || thr.kind != KaptError.Kind.ERROR_RAISED) {
                logger.exception(thr)
            }

            // We don't have any Kotlin files, so there isn't anything we can report diagnostic on
            if (files.isEmpty()) {
                throw thr
            }

            bindingTrace.report(ErrorsKapt3.KAPT3_PROCESSING_ERROR.on(files.first()))
            return null // Compilation will be aborted anyway because of the error above
        } finally {
            generationState.destroy()
            kaptContext.close()
        }

        return if (aptOnly) {
            doNotGenerateCode()
        } else {
            AnalysisResult.RetryWithAdditionalJavaRoots(
                    bindingTrace.bindingContext,
                    module,
                    listOf(sourcesOutputDir),
                    addToEnvironment = true)
        }
    }

    private fun compileStubs(
            project: Project,
            module: ModuleDescriptor,
            bindingContext: BindingContext,
            files: List<KtFile>
    ): Pair<KaptContext, GenerationState> {
        val builderFactory = Kapt3BuilderFactory()

        val generationState = GenerationState(
                project,
                builderFactory,
                module,
                bindingContext,
                files,
                CompilerConfiguration.EMPTY)

        val (classFilesCompilationTime) = measureTimeMillis {
            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)
        }

        val compiledClasses = builderFactory.compiledClasses
        val origins = builderFactory.origins

        logger.info { "Stubs compilation took $classFilesCompilationTime ms" }
        logger.info { "Compiled classes: " + compiledClasses.joinToString { it.name } }

        return Pair(KaptContext(logger, bindingContext, compiledClasses, origins, options), generationState)
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContext, generationState: GenerationState) {
        val converter = ClassFileToSourceStubConverter(kaptContext, generationState.typeMapper,
                                                       generateNonExistentClass = true, correctErrorTypes = correctErrorTypes)

        val (stubGenerationTime, kotlinSourceStubs) = measureTimeMillis {
            converter.convert()
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        logger.info { "Stubs for Kotlin classes: " + kotlinSourceStubs.joinToString { it.sourcefile.name } }

        saveStubs(kotlinSourceStubs)
        saveIncrementalData(generationState, logger.messageCollector, converter)
    }

    private fun collectJavaSourceFiles(): List<File> {
        val javaFilesFromJavaSourceRoots = (javaSourceRoots + stubsOutputDir).flatMap {
            root -> root.walk().filter { it.isFile && it.extension == "java" }.toList()
        }
        logger.info { "Java source files: " + javaFilesFromJavaSourceRoots.joinToString { it.canonicalPath } }

        return javaFilesFromJavaSourceRoots
    }

    protected open fun saveStubs(stubs: JavacList<JCTree.JCCompilationUnit>) {
        for (stub in stubs) {
            val className = (stub.defs.first { it is JCTree.JCClassDecl } as JCTree.JCClassDecl).simpleName.toString()

            val packageName = stub.packageName?.toString() ?: ""
            val packageDir = if (packageName.isEmpty()) stubsOutputDir else File(stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()
            File(packageDir, className + ".java").writeText(stub.toString())
        }
    }

    protected open fun saveIncrementalData(
            generationState: GenerationState,
            messageCollector: MessageCollector,
            converter: ClassFileToSourceStubConverter) {
        val incrementalDataOutputDir = this.incrementalDataOutputDir ?: return

        generationState.factory.writeAll(incrementalDataOutputDir) { file, sources, output ->
            val stubFileObject = converter.bindings[file.relativePath.substringBeforeLast(".class", missingDelimiterValue = "")]
            if (stubFileObject != null) {
                val stubFile = File(stubsOutputDir, stubFileObject.name)
                if (stubFile.exists()) {
                    messageCollector.report(CompilerMessageSeverity.OUTPUT,
                                            OutputMessageUtil.formatOutputMessage(sources, stubFile), CompilerMessageLocation.NO_LOCATION)
                }
            }

            messageCollector.report(CompilerMessageSeverity.OUTPUT,
                                    OutputMessageUtil.formatOutputMessage(sources, output), CompilerMessageLocation.NO_LOCATION)
        }

    }

    protected abstract fun loadProcessors(): List<Processor>
}

private inline fun <T> measureTimeMillis(block: () -> T) : Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}