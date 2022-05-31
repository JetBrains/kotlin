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
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.base.kapt3.AptMode.APT_ONLY
import org.jetbrains.kotlin.base.kapt3.AptMode.WITH_COMPILATION
import org.jetbrains.kotlin.base.kapt3.DetectMemoryLeaksMode
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.base.kapt3.collectJavaSourceFiles
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.OUTPUT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.LoadedProcessors
import org.jetbrains.kotlin.kapt3.base.ProcessorLoader
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation.Companion.KAPT_METADATA_EXTENSION
import org.jetbrains.kotlin.kapt3.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt3.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt3.base.util.info
import org.jetbrains.kotlin.kapt3.base.util.isJava11OrLater
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter.KaptStub
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.utils.kapt.MemoryLeakDetector
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.net.URLClassLoader
import javax.annotation.processing.Processor

class ClasspathBasedKapt3Extension(
    options: KaptOptions,
    logger: MessageCollectorBackedKaptLogger,
    compilerConfiguration: CompilerConfiguration
) : AbstractKapt3Extension(options, logger, compilerConfiguration) {
    override val analyzePartially: Boolean
        get() = options[KaptFlag.USE_LIGHT_ANALYSIS] && super.analyzePartially

    private var processorLoader: ProcessorLoader? = null

    override fun loadProcessors(): LoadedProcessors {
        val efficientProcessorLoader = object : ProcessorLoader(options, logger) {
            override fun doLoadProcessors(classpath: LinkedHashSet<File>, classLoader: ClassLoader): List<Processor> =
                when (classLoader) {
                    is URLClassLoader -> ServiceLoaderLite.loadImplementations(Processor::class.java, classLoader)
                    else -> super.doLoadProcessors(classpath, classLoader)
                }
        }

        this.processorLoader = efficientProcessorLoader
        return efficientProcessorLoader.loadProcessors()
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
            clearJavacZipCaches()
        }
    }

    private fun clearJavacZipCaches() {
        try {
            val zipFileIndexCacheClass = Class.forName("com.sun.tools.javac.file.ZipFileIndexCache")
            val zipFileIndexCacheInstance = zipFileIndexCacheClass.getMethod("getSharedInstance").invoke(null)
            zipFileIndexCacheClass.getMethod("clearCache").invoke(zipFileIndexCacheInstance)
        } catch (e: Throwable) {}
    }
}

abstract class AbstractKapt3Extension(
    val options: KaptOptions,
    val logger: MessageCollectorBackedKaptLogger,
    val compilerConfiguration: CompilerConfiguration
) : PartialAnalysisHandlerExtension() {
    private val pluginInitializedTime: Long = System.currentTimeMillis()

    private var annotationProcessingComplete = false

    private fun setAnnotationProcessingComplete(): Boolean {
        if (annotationProcessingComplete) return true

        annotationProcessingComplete = true
        return false
    }

    override val analyzePartially: Boolean
        get() = !annotationProcessingComplete

    override val analyzeDefaultParameterValues: Boolean
        get() = options[KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES]

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (options.mode == APT_ONLY) {
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

        fun doNotGenerateCode() = AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)

        logger.info { "Initial analysis took ${System.currentTimeMillis() - pluginInitializedTime} ms" }

        val bindingContext = bindingTrace.bindingContext
        if (options.mode.generateStubs) {
            logger.info { "Kotlin files to compile: " + files.map { it.virtualFile?.name ?: "<in memory ${it.hashCode()}>" } }

            contextForStubGeneration(project, module, bindingContext, files.toList()).use { context ->
                generateKotlinSourceStubs(context)
            }
        }

        if (!options.mode.runAnnotationProcessing) return doNotGenerateCode()

        val processors = loadProcessors()
        if (processors.processors.isEmpty()) return if (options.mode != WITH_COMPILATION) doNotGenerateCode() else null

        val kaptContext = KaptContext(options, false, logger)

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

        return if (options.mode != WITH_COMPILATION) {
            doNotGenerateCode()
        } else {
            AnalysisResult.RetryWithAdditionalRoots(
                bindingTrace.bindingContext,
                module,
                listOf(options.sourcesOutputDir),
                listOfNotNull(options.sourcesOutputDir, options.getKotlinGeneratedSourcesDirectory()),
                addToEnvironment = true
            )
        }
    }

    private fun runAnnotationProcessing(kaptContext: KaptContext, processors: LoadedProcessors) {
        if (!options.mode.runAnnotationProcessing) return

        val javaSourceFiles = options.collectJavaSourceFiles(kaptContext.sourcesToReprocess)
        logger.info { "Java source files: " + javaSourceFiles.joinToString { it.canonicalPath } }

        val (annotationProcessingTime) = measureTimeMillis {
            kaptContext.doAnnotationProcessing(javaSourceFiles, processors.processors)
        }

        logger.info { "Annotation processing took $annotationProcessingTime ms" }

        if (options.detectMemoryLeaks != DetectMemoryLeaksMode.NONE) {
            MemoryLeakDetector.add(processors.classLoader)

            val isParanoid = options.detectMemoryLeaks == DetectMemoryLeaksMode.PARANOID
            val (leakDetectionTime, leaks) = measureTimeMillis { MemoryLeakDetector.process(isParanoid) }
            logger.info { "Leak detection took $leakDetectionTime ms" }

            for (leak in leaks) {
                logger.warn(buildString {
                    appendLine("Memory leak detected!")
                    appendLine("Location: '${leak.className}', static field '${leak.fieldName}'")
                    append(leak.description)
                })
            }
        }
    }

    private fun contextForStubGeneration(
        project: Project,
        module: ModuleDescriptor,
        bindingContext: BindingContext,
        files: List<KtFile>
    ): KaptContextForStubGeneration {
        val builderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)

        val configuration = compilerConfiguration.copy().apply {
            put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)
        }

        val targetId = TargetId(
            name = configuration[CommonConfigurationKeys.MODULE_NAME] ?: module.name.asString(),
            type = "java-production"
        )

        val isIrBackend = options.flags[KaptFlag.USE_JVM_IR]
        val generationState = GenerationState.Builder(project, builderFactory, module, bindingContext, configuration)
            .targetId(targetId)
            .isIrBackend(isIrBackend)
            .build()

        val (classFilesCompilationTime) = measureTimeMillis {
            KotlinCodegenFacade.compileCorrectFiles(
                files,
                generationState,
                if (isIrBackend)
                    JvmIrCodegenFactory(configuration, configuration[CLIConfigurationKeys.PHASE_CONFIG])
                else DefaultCodegenFactory
            )
        }

        val compiledClasses = builderFactory.compiledClasses
        val origins = builderFactory.origins

        logger.info { "Stubs compilation took $classFilesCompilationTime ms" }
        logger.info { "Compiled classes: " + compiledClasses.joinToString { it.name } }

        return KaptContextForStubGeneration(
            options, false, logger, project, bindingContext,
            compiledClasses, origins, generationState
        )
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContextForStubGeneration) {
        val converter = ClassFileToSourceStubConverter(kaptContext, generateNonExistentClass = true)

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
            val packageDir = if (packageName.isEmpty()) options.stubsOutputDir else File(options.stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val sourceFile = File(packageDir, "$className.java")
            sourceFile.writeText(stub.prettyPrint(kaptContext.context))

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile)
        }
    }

    protected open fun saveIncrementalData(
        kaptContext: KaptContextForStubGeneration,
        messageCollector: MessageCollector,
        converter: ClassFileToSourceStubConverter
    ) {
        val incrementalDataOutputDir = options.incrementalDataOutputDir ?: return

        val reportOutputFiles = kaptContext.generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        kaptContext.generationState.factory.writeAll(
            incrementalDataOutputDir,
            if (!reportOutputFiles) null else fun(file: OutputFile, sources: List<File>, output: File) {
                val stubFileObject = converter.bindings[file.relativePath.substringBeforeLast(".class", missingDelimiterValue = "")]
                if (stubFileObject != null) {
                    val stubFile = File(options.stubsOutputDir, stubFileObject.name)
                    val lineMappingsFile = File(stubFile.parentFile, stubFile.nameWithoutExtension + KAPT_METADATA_EXTENSION)

                    for (outputFile in listOf(stubFile, lineMappingsFile)) {
                        if (outputFile.exists()) {
                            messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sources, outputFile))
                        }
                    }
                }

                messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sources, output))
            }
        )
    }

    protected abstract fun loadProcessors(): LoadedProcessors
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

            if (isJava11OrLater()) {
                // Print enums fully, there is an issue when using Pretty in JDK 11.
                // See https://youtrack.jetbrains.com/issue/KT-33052.
                print("/*public static final*/ ${tree.name}")
                tree.init?.let { print(" /* = $it */") }
                return
            }
        }
        super.visitVarDef(tree)
    }
}

private inline fun <T> measureTimeMillis(block: () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}
