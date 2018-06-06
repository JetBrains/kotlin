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

import com.intellij.ide.ClassUtilCore
import com.intellij.openapi.project.Project
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.Pretty
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Convert
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.OUTPUT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAll
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.kapt3.AptMode.*
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter.KaptStub
import org.jetbrains.kotlin.kapt3.stubs.KaptLineMappingCollector.Companion.KAPT_METADATA_EXTENSION
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.kapt3.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
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
        javacOptions: Map<String, String>,
        annotationProcessorFqNames: List<String>,
        aptMode: AptMode,
        val useLightAnalysis: Boolean,
        correctErrorTypes: Boolean,
        mapDiagnosticLocations: Boolean,
        pluginInitializedTime: Long,
        logger: KaptLogger,
        compilerConfiguration: CompilerConfiguration
) : AbstractKapt3Extension(compileClasspath, annotationProcessingClasspath, javaSourceRoots, sourcesOutputDir,
                           classFilesOutputDir, stubsOutputDir, incrementalDataOutputDir, options, javacOptions, annotationProcessorFqNames,
                           aptMode, pluginInitializedTime, logger, correctErrorTypes, mapDiagnosticLocations, compilerConfiguration) {
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
            ClassUtilCore.clearJarURLCache()
        }
    }

    override fun loadProcessors(): List<Processor> {
        ClassUtilCore.clearJarURLCache()

        val classpath = (annotationProcessingClasspath + compileClasspath).distinct()
        val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray())
        this.annotationProcessingClassLoader = classLoader

        val processors = if (annotationProcessorFqNames.isNotEmpty()) {
            logger.info("Annotation processor class names are set, skip AP discovery")
            annotationProcessorFqNames.mapNotNull { tryLoadProcessor(it, classLoader) }
        } else {
            logger.info("Need to discovery annotation processors in the AP classpath")
            ServiceLoader.load(Processor::class.java, classLoader).toList()
        }

        if (processors.isEmpty()) {
            logger.info("No annotation processors available, aborting")
        } else {
            logger.info { "Annotation processors: " + processors.joinToString { it::class.java.canonicalName } }
        }

        return processors
    }

    private fun tryLoadProcessor(fqName: String, classLoader: ClassLoader): Processor? {
        val annotationProcessorClass = try {
            Class.forName(fqName, true, classLoader)
        } catch (e: Throwable) {
            logger.warn("Can't find annotation processor class $fqName: ${e.message}")
            return null
        }

        try {
            val annotationProcessorInstance = annotationProcessorClass.newInstance()
            if (annotationProcessorInstance !is Processor) {
                logger.warn("$fqName is not an instance of 'Processor'")
                return null
            }

            return annotationProcessorInstance
        } catch (e: Throwable) {
            logger.warn("Can't load annotation processor class $fqName: ${e.message}")
            return null
        }
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
        val javacOptions: Map<String, String>,
        val annotationProcessorFqNames: List<String>,
        val aptMode: AptMode,
        val pluginInitializedTime: Long,
        val logger: KaptLogger,
        val correctErrorTypes: Boolean,
        val mapDiagnosticLocations: Boolean,
        val compilerConfiguration: CompilerConfiguration
) : PartialAnalysisHandlerExtension() {
    val compileClasspath = compileClasspath.distinct()
    val annotationProcessingClasspath = annotationProcessingClasspath.distinct()

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

        val processors = loadProcessors()
        if (processors.isEmpty()) return if (aptMode != WITH_COMPILATION) doNotGenerateCode() else null

        val kaptContext = generateStubs(project, module, bindingTrace.bindingContext, files)

        try {
            runAnnotationProcessing(kaptContext, processors)
        } catch (error: KaptError) {
            val cause = error.cause

            if (cause != null) {
                kaptContext.logger.exception(cause)
            }

            return AnalysisResult.compilationError(bindingTrace.bindingContext)
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
                    listOf(sourcesOutputDir),
                    addToEnvironment = true)
        }
    }

    private fun generateStubs(project: Project, module: ModuleDescriptor, context: BindingContext, files: Collection<KtFile>): KaptContext<*> {
        if (!aptMode.generateStubs) {
            return KaptContext(logger, project, BindingContext.EMPTY, emptyList(), emptyMap(), null,
                               mapDiagnosticLocations, options, javacOptions)
        }

        logger.info { "Kotlin files to compile: " + files.map { it.virtualFile?.name ?: "<in memory ${it.hashCode()}>" } }

        return compileStubs(project, module, context, files.toList()).apply {
            generateKotlinSourceStubs(this)
        }
    }

    private fun runAnnotationProcessing(kaptContext: KaptContext<*>, processors: List<Processor>) {
        if (!aptMode.runAnnotationProcessing) return

        val javaSourceFiles = collectJavaSourceFiles()

        val (annotationProcessingTime) = measureTimeMillis {
            kaptContext.doAnnotationProcessing(
                javaSourceFiles, processors, compileClasspath, annotationProcessingClasspath, sourcesOutputDir, classFilesOutputDir)
        }

        logger.info { "Annotation processing took $annotationProcessingTime ms" }
    }

    private fun compileStubs(
            project: Project,
            module: ModuleDescriptor,
            bindingContext: BindingContext,
            files: List<KtFile>
    ): KaptContext<GenerationState> {
        val builderFactory = Kapt3BuilderFactory()

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

        return KaptContext(logger, project, bindingContext, compiledClasses, origins, generationState,
                           mapDiagnosticLocations, options, javacOptions)
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContext<GenerationState>) {
        val converter = ClassFileToSourceStubConverter(kaptContext, generateNonExistentClass = true, correctErrorTypes = correctErrorTypes)

        val (stubGenerationTime, kaptStubs) = measureTimeMillis {
            converter.convert()
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        logger.info { "Stubs for Kotlin classes: " + kaptStubs.joinToString { it.file.sourcefile.name } }

        saveStubs(kaptContext, kaptStubs)
        saveIncrementalData(kaptContext, logger.messageCollector, converter)
    }

    private fun collectJavaSourceFiles(): List<File> {
        val javaFilesFromJavaSourceRoots = (javaSourceRoots + stubsOutputDir).flatMap {
            root -> root.walk().filter { it.isFile && it.extension == "java" }.toList()
        }
        logger.info { "Java source files: " + javaFilesFromJavaSourceRoots.joinToString { it.canonicalPath } }

        return javaFilesFromJavaSourceRoots
    }

    protected open fun saveStubs(kaptContext: KaptContext<*>, stubs: List<KaptStub>) {
        for (kaptStub in stubs) {
            val stub = kaptStub.file
            val className = (stub.defs.first { it is JCTree.JCClassDecl } as JCTree.JCClassDecl).simpleName.toString()

            val packageName = stub.getPackageNameJava9Aware()?.toString() ?: ""
            val packageDir = if (packageName.isEmpty()) stubsOutputDir else File(stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val sourceFile = File(packageDir, className + ".java")
            sourceFile.writeText(stub.prettyPrint(kaptContext.context))

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile)
        }
    }

    protected open fun saveIncrementalData(
            kaptContext: KaptContext<GenerationState>,
            messageCollector: MessageCollector,
            converter: ClassFileToSourceStubConverter) {
        val incrementalDataOutputDir = this.incrementalDataOutputDir ?: return

        val reportOutputFiles = kaptContext.generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        kaptContext.generationState.factory.writeAll(
                incrementalDataOutputDir,
                if (!reportOutputFiles) null else fun(file: OutputFile, sources: List<File>, output: File) {
                    val stubFileObject = converter.bindings[file.relativePath.substringBeforeLast(".class", missingDelimiterValue = "")]
                    if (stubFileObject != null) {
                        val stubFile = File(stubsOutputDir, stubFileObject.name)
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
