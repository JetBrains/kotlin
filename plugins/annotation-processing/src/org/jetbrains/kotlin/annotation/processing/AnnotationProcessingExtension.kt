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

package org.jetbrains.kotlin.annotation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.annotation.processing.RoundAnnotations
import org.jetbrains.kotlin.annotation.processing.diagnostic.ErrorsAnnotationProcessing
import org.jetbrains.kotlin.annotation.processing.impl.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLClassLoader
import java.util.*
import javax.annotation.processing.Processor
import javax.tools.Diagnostic

class ClasspathBasedAnnotationProcessingExtension(
        val annotationProcessingClasspath: List<File>,
        override val options: Map<String, String>,
        generatedSourcesOutputDir: File,
        classesOutputDir: File,
        javaSourceRoots: List<File>,
        verboseOutput: Boolean,
        incrementalDataFile: File?,
        messageCollector: MessageCollector?
) : AbstractAnnotationProcessingExtension(generatedSourcesOutputDir,
                                          classesOutputDir, javaSourceRoots, verboseOutput,
                                          incrementalDataFile, messageCollector) {
    override fun loadAnnotationProcessors(): List<Processor> {
        val classLoader = URLClassLoader(annotationProcessingClasspath.map { it.toURI().toURL() }.toTypedArray())
        return ServiceLoader.load(Processor::class.java, classLoader).toList()
    }
}

abstract class AbstractAnnotationProcessingExtension(
        val generatedSourcesOutputDir: File,
        val classesOutputDir: File,
        val javaSourceRoots: List<File>,
        val verboseOutput: Boolean,
        val incrementalDataFile: File? = null,
        messageCollector: MessageCollector? = null
) : AnalysisHandlerExtension {
    private companion object {
        val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"
    }

    private var annotationProcessingComplete = false
    private val messager = run {
        val collector = messageCollector ?: PrintingMessageCollector(System.err, MessageRenderer.WITHOUT_PATHS, verboseOutput)
        KotlinMessager(collector)
    }

    private inline fun log(message: () -> String) {
        if (verboseOutput) {
            messager.printMessage(Diagnostic.Kind.OTHER, "Kapt: " + message())
        }
    }
    
    private fun Int.count(noun: String) = if (this == 1) "$this $noun" else "$this ${noun}s"

    private inline fun <T, R> T.runIf(condition: Boolean, block: T.() -> R) {
        if (condition) block()
    }

    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>
    ): AnalysisResult? {
        if (annotationProcessingComplete) {
            return null
        }

        // Clean the generated source directory even if we don't run any annotation processors
        generatedSourcesOutputDir.deleteRecursively()
        generatedSourcesOutputDir.mkdirs()

        val processors = loadAnnotationProcessors()
        if (processors.isEmpty()) {
            log { "No annotation processors detected, exiting" }
            return null
        }

        val appendJavaSourceRootsHandler = project.getUserData(APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY)
        if (appendJavaSourceRootsHandler == null) {
            log { "Java source root handler is not set, exiting" }
            return null
        }

        val startTime = System.currentTimeMillis()

        val psiManager = PsiManager.getInstance(project)
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)

        val options = this.options
        log { "Options: $options" }

        val filer = KotlinFiler(generatedSourcesOutputDir, classesOutputDir)
        val types = KotlinTypes(javaPsiFacade, PsiManager.getInstance(project), projectScope)
        val elements = KotlinElements(javaPsiFacade, projectScope)

        val processingEnvironment = KotlinProcessingEnvironment(
                elements, types, messager, options, filer, processors,
                project, psiManager, javaPsiFacade, projectScope, bindingTrace.bindingContext, appendJavaSourceRootsHandler)

        var processingResult: ProcessingResult
        try {
            processingResult = processingEnvironment.doAnnotationProcessing(files)
            processingEnvironment.dispose()
        }
        catch (thr: Throwable) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "An exception occurred during annotation processing. Stacktrace: \n" + thr.getStackTraceAsString())

            processingResult = ProcessingResult(errorCount = 1, warningCount = 0, wasAnythingGenerated = false)
        }

        annotationProcessingComplete = true
        log {
            "Annotation processing complete in ${System.currentTimeMillis() - startTime} ms, " +
                    processingResult.errorCount.count("error") + ", " +
                    processingResult.warningCount.count("warning")
        }

        if (processingResult.errorCount != 0) {
            val reportFile = files.firstOrNull()
            if (reportFile != null) {
                bindingTrace.report(ErrorsAnnotationProcessing.ANNOTATION_PROCESSING_ERROR.on(reportFile))
            }

            // Do not restart Kotlin file analysis
            return null
        }
        
        if (!processingResult.wasAnythingGenerated) {
            // Nothing was generated, do not need to restart analysis
            return null
        }

        return AnalysisResult.RetryWithAdditionalJavaRoots(
                bindingTrace.bindingContext,
                module,
                listOf(generatedSourcesOutputDir),
                addToEnvironment = false)
    }

    private fun Throwable.getStackTraceAsString(): String {
        val out = StringWriter(1024)
        val printWriter = PrintWriter(out)
        try {
            printStackTrace(printWriter)
            return out.toString().replace("\r", "")
        }
        finally {
            printWriter.close()
        }
    }
    
    private fun KotlinProcessingEnvironment.createTypeMapper(): KotlinTypeMapper {
        return KotlinTypeMapper(bindingContext(), ClassBuilderMode.full(false), NoResolveFileClassesProvider,
                                IncompatibleClassTracker.DoNothing, JvmAbi.DEFAULT_MODULE_NAME)
    }

    private fun KotlinProcessingEnvironment.doAnnotationProcessing(files: Collection<KtFile>): ProcessingResult {
        run initializeProcessors@ {
            processors().forEach { it.init(this) }
            log { "Initialized processors: " + processors().joinToString { it.javaClass.name } }
        }

        val firstRoundAnnotations = RoundAnnotations(
                bindingContext(),
                createTypeMapper())
        
        run analyzeFilesForFirstRound@ {
            log { "Analysing Kotlin files: " + files.map { it.virtualFile.path } }
            firstRoundAnnotations.analyzeFiles(files)

            log { "Analysing Java source roots: $javaSourceRoots" }
            for (javaSourceRoot in javaSourceRoots) {
                javaSourceRoot.walk().filter { it.isFile && it.extension == "java" }.forEach {
                    val vFile = StandardFileSystems.local().findFileByPath(it.absolutePath)
                    if (vFile != null) {
                        val javaFile = psiManager().findFile(vFile) as? PsiJavaFile
                        if (javaFile != null) {
                            firstRoundAnnotations.analyzeFile(javaFile)
                        }
                    }
                }
            }
        }
        
        runIf(incrementalDataFile != null) processIncrementalData@ {
            val incrementalDataFile = incrementalDataFile!!
            
            
            runIf(incrementalDataFile.exists()) analyzeFilesFromPreviousIncrementalData@ {
                val incrementalData = try {
                    incrementalDataFile.readText()
                } catch (e: IOException) {
                    log { "An exception occurred while processing incremental data file: $incrementalDataFile" }
                    null 
                }

                if (incrementalData != null) {
                    val analyzedClasses = mutableListOf<String>()
                    
                    for (line in incrementalData.lines()) {
                        if (line.length < 3 || !line.startsWith("i ")) continue
                        val fqName = line.drop(2) 
                        val psiClass = javaPsiFacade().findClass(fqName, projectScope()) ?: continue
                        if (firstRoundAnnotations.analyzeDeclaration(psiClass)) {
                            analyzedClasses += fqName
                        }
                    }
                    
                    log { "Analysing files from incremental data: $analyzedClasses" }
                }
            }
            
            run saveNewIncrementalData@ {
                val analyzedClasses = firstRoundAnnotations.analyzedClasses
                log { "Saving incremental data: ${analyzedClasses.size} class names" }
                try {
                    incrementalDataFile.parentFile.mkdirs()
                    incrementalDataFile.writeText(analyzedClasses.map { "i $it" }.joinToString(LINE_SEPARATOR))
                }
                catch (e: IOException) {
                    log { "Unable to write $incrementalDataFile" }
                }
            }
        }
        
        val finalRoundNumber = run annotationProcessing@ {
            val firstRoundEnvironment = KotlinRoundEnvironment(firstRoundAnnotations, false, 1)
            process(firstRoundEnvironment) // Dispose for firstRoundEnvironment is called inside process
        } + 1
        
        log { "Starting round $finalRoundNumber (final)" }
        val finalRoundEnvironment = KotlinRoundEnvironment(firstRoundAnnotations.copy(), true, finalRoundNumber)
        for (processor in processors()) {
            processor.process(emptySet(), finalRoundEnvironment)
        }
        finalRoundEnvironment.dispose()

        return ProcessingResult(messager.errorCount, messager.warningCount, filer.wasAnythingGenerated)
    }
    
    private tailrec fun KotlinProcessingEnvironment.process(roundEnvironment: KotlinRoundEnvironment): Int {
        val newFiles = doRound(roundEnvironment)
        val newJavaFiles = newFiles.filter { it.extension.toLowerCase() == "java" }
        if (newJavaFiles.isEmpty() || messager.errorCount != 0) {
            return roundEnvironment.roundNumber
        }
        
        // Add new Java source roots after the first round
        if (roundEnvironment.roundNumber == 1) {
            appendJavaSourceRootsHandler()(listOf(generatedSourcesOutputDir))
        }
        
        // Update the platform caches
        ApplicationManager.getApplication().runWriteAction {
            (psiManager().modificationTracker as? PsiModificationTrackerImpl)?.incCounter()
        }
        
        // Find generated files
        val localFileSystem = StandardFileSystems.local()
        val psiFiles = newJavaFiles
                .map { localFileSystem.findFileByPath(it.absolutePath)?.let { psiManager().findFile(it) } }
                .filterIsInstance<PsiJavaFile>()

        if (psiFiles.isEmpty()) {
            log { "Something is strange, files were generated but not found by PsiManager" }
            return roundEnvironment.roundNumber
        }
        
        // Start the next round
        val nextRoundAnnotations = roundEnvironment.roundAnnotations().copy().apply { analyzeFiles(psiFiles) }
        val nextRoundEnvironment = KotlinRoundEnvironment(nextRoundAnnotations, false, roundEnvironment.roundNumber + 1)
        roundEnvironment.dispose()
        return process(nextRoundEnvironment)
    }
    
    private fun KotlinProcessingEnvironment.doRound(roundEnvironment: KotlinRoundEnvironment): List<File> {
        log { "Starting round ${roundEnvironment.roundNumber}" }
        
        val newFiles = mutableListOf<File>()
        filer.onFileCreatedHandler = { newFiles += it }

        for (processor in processors()) {
            val supportedAnnotationNames = processor.supportedAnnotationTypes
            val acceptsAnyAnnotation = supportedAnnotationNames.contains("*")

            val applicableAnnotationNames = when (acceptsAnyAnnotation) {
                true -> roundEnvironment.roundAnnotations().annotationsMap.keys
                false -> processor.supportedAnnotationTypes.filter { it in roundEnvironment.roundAnnotations().annotationsMap }
            }

            if (applicableAnnotationNames.isEmpty()) {
                log { "Skipping processor " + processor.javaClass.name + ": no relevant annotations" }
                continue
            }

            val applicableAnnotations = applicableAnnotationNames
                    .map { javaPsiFacade().findClass(it, projectScope())?.let { JeTypeElement(it) } }
                    .filterNotNullTo(hashSetOf())

            log {
                val annotationNames = applicableAnnotations.joinToString { it.qualifiedName.toString() }
                "Processing with " + processor.javaClass.name + " (annotations: " + annotationNames + ")"
            }

            processor.process(applicableAnnotations, roundEnvironment)
        }
        
        log { "Round ${roundEnvironment.roundNumber} finished, ${newFiles.size.count("file")} generated (${newFiles.joinToString()})" }
        return newFiles
    }

    protected abstract fun loadAnnotationProcessors(): List<Processor>
    protected abstract val options: Map<String, String>
}

private class ProcessingResult(val errorCount: Int, val warningCount: Int, val wasAnythingGenerated: Boolean)
