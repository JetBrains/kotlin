/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import com.sun.source.util.Trees
import com.sun.tools.javac.comp.CompileStates.CompileState
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.processing.AnnotationProcessingError
import com.sun.tools.javac.processing.JavacFiler
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.base.incremental.*
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaFileManager
import org.jetbrains.kotlin.kapt3.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.isJava9OrLater
import org.jetbrains.kotlin.kapt3.base.util.measureTimeMillisWithResult
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileObject
import kotlin.collections.List
import kotlin.system.measureTimeMillis
import com.sun.tools.javac.util.List as JavacList

fun KaptContext.doAnnotationProcessing(
    javaSourceFiles: List<File>,
    processors: List<IncrementalProcessor>,
    binaryTypesToReprocess: List<String> = emptyList()
) = doAnnotationProcessing(javaSourceFiles, processors, JavacList.nil(), binaryTypesToReprocess)

fun KaptContext.doAnnotationProcessing(
    javaSourceFiles: List<File>,
    processors: List<IncrementalProcessor>,
    additionalSources: JavacList<JCTree.JCCompilationUnit> = JavacList.nil(),
    binaryTypesToReprocess: List<String> = emptyList()
) {
    val processingEnvironment = JavacProcessingEnvironment.instance(context)

    val wrappedProcessors = processors.map { ProcessorWrapper(it) }

    val javaSourcesToProcess = run {
        //module descriptor should be in root package, but here we filter it from everywhere (bc we don't have knowledge about root here)
        val filtered = javaSourceFiles.filterNot { it.name == KaptContext.MODULE_INFO_FILE }
        if (filtered.size != javaSourceFiles.size) {
            logger.info("${KaptContext.MODULE_INFO_FILE} is removed from sources files to disable JPMS")
        }
        filtered
    }

    val compilerAfterAP: JavaCompiler
    try {
        if (javaSourcesToProcess.isEmpty() && binaryTypesToReprocess.isEmpty() && additionalSources.isEmpty()) {
            if (logger.isVerbose) {
                logger.info("Skipping annotation processing as all sources are up-to-date.")
            }
            return
        }

        val initProcessAnnotationsMethod = JavaCompiler::class.java.declaredMethods.single { it.name == "initProcessAnnotations" }
        if (isJava9OrLater()) {
            initProcessAnnotationsMethod.invoke(compiler, wrappedProcessors, emptyList<JavaFileObject>(), emptyList<String>())
        } else {
            initProcessAnnotationsMethod.invoke(compiler, wrappedProcessors)
        }

        if (logger.isVerbose) {
            logger.info("Processing java sources with annotation processors: ${javaSourcesToProcess.joinToString()}")
            logger.info("Processing types with annotation processors: ${binaryTypesToReprocess.joinToString()}")
        }
        val parsedJavaFiles = parseJavaFiles(javaSourcesToProcess)

        val sourcesStructureListener = cacheManager?.let {
            if (processors.any { it.isUnableToRunIncrementally() }) return@let null

            val recordTypesListener = MentionedTypesTaskListener(cacheManager.javaCache, processingEnvironment.elementUtils, Trees.instance(processingEnvironment))
            compiler.getTaskListeners().add(recordTypesListener)
            recordTypesListener
        }

        compilerAfterAP = try {
            javaLog.interceptorData.files = parsedJavaFiles.map { it.sourceFile to it }.toMap()
            val analyzedFiles = compiler.stopIfErrorOccurred(
                CompileState.PARSE, compiler.enterTrees(parsedJavaFiles + additionalSources)
            )

            val additionalClassNames = JavacList.from(binaryTypesToReprocess)
            if (isJava9OrLater()) {
                val processAnnotationsMethod =
                    compiler.javaClass.getMethod("processAnnotations", JavacList::class.java, java.util.Collection::class.java)
                processAnnotationsMethod.invoke(compiler, analyzedFiles, additionalClassNames)
                compiler
            } else {
                val processAnnotationsMethod =
                    compiler.javaClass.getMethod("processAnnotations", JavacList::class.java, JavacList::class.java)
                processAnnotationsMethod.invoke(compiler, analyzedFiles, additionalClassNames) as JavaCompiler
            }
        } catch (e: AnnotationProcessingError) {
            throw KaptBaseError(KaptBaseError.Kind.EXCEPTION, e.cause ?: e)
        }
        sourcesStructureListener?.let { compiler.getTaskListeners().remove(it) }

        cacheManager?.updateCache(processors, sourcesStructureListener?.failureReason != null)

        sourcesStructureListener?.let {
            if (logger.isVerbose) {
                logger.info("Analyzing sources structure took ${it.time}[ms].")
            }
        }
        reportIfRunningNonIncrementally(sourcesStructureListener, cacheManager, logger, processors)

        val log = compilerAfterAP.log

        val filer = processingEnvironment.filer as JavacFiler
        val errorCount = log.nerrors
        val warningCount = log.nwarnings

        if (logger.isVerbose) {
            logger.info("Annotation processing complete, errors: $errorCount, warnings: $warningCount")
        }

        val showProcessorStats = options[KaptFlag.SHOW_PROCESSOR_STATS]
        if (logger.isVerbose || showProcessorStats) {
            val loggerFun = if (showProcessorStats) logger::warn else logger::info
            showProcessorStats(wrappedProcessors, loggerFun)
        }

        options.processorsStatsReportFile?.let { dumpProcessorStats(wrappedProcessors, it, logger::info) }

        options.fileReadHistoryReportFile?.let {
            dumpFileReadHistory(fileManager, it, logger::info)
        }

        if (logger.isVerbose) {
            filer.displayState()
        }

        if (log.nerrors > 0) {
            throw KaptBaseError(KaptBaseError.Kind.ERROR_RAISED)
        }
    } finally {
        processingEnvironment.close()
        this@doAnnotationProcessing.close()
    }
}

private fun showProcessorStats(wrappedProcessors: List<ProcessorWrapper>, logger: (String) -> Unit) {
    logger("Annotation processor stats:")
    wrappedProcessors.forEach { processor ->
        logger(processor.renderSpentTime())
    }
    logger("Generated files report:")
    wrappedProcessors.forEach { processor ->
        logger(processor.renderGenerations())
    }
}

private fun dumpProcessorStats(wrappedProcessors: List<ProcessorWrapper>, apReportFile: File, logger: (String) -> Unit) {
    logger("Dumping Kapt Annotation Processing performance report to ${apReportFile.absolutePath}")

    apReportFile.writeText(buildString {
        appendLine("Kapt Annotation Processing performance report:")
        wrappedProcessors.forEach { processor ->
            appendLine(processor.renderSpentTime())
        }
        appendLine("Generated files report:")
        wrappedProcessors.forEach { processor ->
            appendLine(processor.renderGenerations())
        }
    })
}

private fun dumpFileReadHistory(fileManager: KaptJavaFileManager, reportFile: File, logger: (String) -> Unit) {
    logger("Dumping KAPT file read history to ${reportFile.absolutePath}")

    reportFile.writeText(fileManager.renderFileReadHistory())
}

private fun reportIfRunningNonIncrementally(
    listener: MentionedTypesTaskListener?,
    cacheManager: JavaClassCacheManager?,
    logger: KaptLogger,
    processors: List<IncrementalProcessor>
) {
    listener ?: return
    cacheManager ?: return

    listener.failureReason?.let { failure ->
        logger.warn("\n$failure")
        return
    }

    val missingIncrementalSupport = processors.filter { it.isMissingIncrementalSupport() }
    if (missingIncrementalSupport.isNotEmpty()) {
        val nonIncremental = missingIncrementalSupport.map { "${it.processorName} (${it.incrementalSupportType})" }
        logger.warn(
            "Incremental annotation processing requested, but support is disabled because the following " +
                    "processors are not incremental: ${nonIncremental.joinToString()}."
        )
    }
}

private class ProcessorWrapper(private val delegate: IncrementalProcessor) : Processor by delegate {
    private var initTime: Long = 0
    private val roundTime = mutableListOf<Long>()
    private val sourcesGenerated = mutableListOf<Int>()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val (time, result) = measureTimeMillisWithResult {
            delegate.process(annotations, roundEnv)
        }

        updateGenerationStats(roundEnv)
        roundTime += time
        return result
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        initTime += measureTimeMillis {
            delegate.init(processingEnv)
        }
    }

    override fun getSupportedOptions(): MutableSet<String> {
        val (time, result) = measureTimeMillisWithResult { delegate.supportedOptions }
        initTime += time
        return result
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        val (time, result) = measureTimeMillisWithResult { delegate.supportedSourceVersion }
        initTime += time
        return result
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        val (time, result) = measureTimeMillisWithResult { delegate.supportedAnnotationTypes }
        initTime += time
        return result
    }

    fun renderSpentTime(): String {
        val processorName = delegate.processorName
        val totalTime = initTime + roundTime.sum()

        return "$processorName: " +
                "total: $totalTime ms, " +
                "init: $initTime ms, " +
                "${roundTime.size} round(s): ${roundTime.joinToString { "$it ms" }}"
    }

    fun renderGenerations(): String {
        val processorName = delegate.processorName

        return "$processorName: " +
                "total sources: ${sourcesGenerated.sum()}, " +
                "sources per round: ${sourcesGenerated.joinToString()}"
    }

    private fun updateGenerationStats(roundEnv: RoundEnvironment) {
        var numSourcesGenerated = -1
        try {
            val procEnv = roundEnv::class.java.getDeclaredField("processingEnv")
            procEnv.isAccessible = true
            val proEnvObj = procEnv.get(roundEnv) as ProcessingEnvironment

            val filerField = JavacProcessingEnvironment::class.java.getDeclaredField("filer")
            filerField.isAccessible = true
            val filerObj = filerField.get(proEnvObj)

            val genSourceNameField = JavacFiler::class.java.getDeclaredField("generatedSourceNames")
            genSourceNameField.isAccessible = true
            val genSourceNameObj = genSourceNameField.get(filerObj)

            @Suppress("UNCHECKED_CAST")
            val sources: Set<String>? = genSourceNameObj as? Set<String>
            numSourcesGenerated = sources?.size ?: -1
        } catch (e: Exception) {
            // Not much we can do
        } finally {
            sourcesGenerated.add(numSourcesGenerated)
        }
    }
}

fun KaptContext.parseJavaFiles(javaSourceFiles: List<File>): JavacList<JCTree.JCCompilationUnit> {
    val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)

    return compiler.stopIfErrorOccurred(
        CompileState.PARSE,
        initModulesIfNeeded(
            compiler.stopIfErrorOccurred(
                CompileState.PARSE,
                compiler.parseFiles(javaFileObjects)
            )
        )
    )
}

private fun KaptContext.initModulesIfNeeded(files: JavacList<JCTree.JCCompilationUnit>): JavacList<JCTree.JCCompilationUnit> {
    if (files.isNotEmpty() && isJava9OrLater()) {
        val initModulesMethod = compiler.javaClass.getMethod("initModules", JavacList::class.java)

        @Suppress("UNCHECKED_CAST")
        return compiler.stopIfErrorOccurred(
            CompileState.PARSE,
            initModulesMethod.invoke(compiler, files) as JavacList<JCTree.JCCompilationUnit>
        )
    }

    return files
}