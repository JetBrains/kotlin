/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import com.sun.tools.javac.comp.CompileStates.*
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.processing.AnnotationProcessingError
import com.sun.tools.javac.processing.JavacFiler
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt3.base.util.isJava9OrLater
import org.jetbrains.kotlin.kapt3.base.util.measureTimeMillisWithResult
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileObject
import kotlin.system.measureTimeMillis
import com.sun.tools.javac.util.List as JavacList

fun KaptContext.doAnnotationProcessing(
        javaSourceFiles: List<File>,
        processors: List<Processor>,
        additionalSources: JavacList<JCTree.JCCompilationUnit> = JavacList.nil()
) {
    val processingEnvironment = JavacProcessingEnvironment.instance(context)
    val wrappedProcessors = processors.map { ProcessorWrapper(it) }

    val compilerAfterAP: JavaCompiler
    try {
        if (isJava9OrLater()) {
            val initProcessAnnotationsMethod = JavaCompiler::class.java.declaredMethods.single { it.name == "initProcessAnnotations" }
            initProcessAnnotationsMethod.invoke(compiler, wrappedProcessors, emptyList<JavaFileObject>(), emptyList<String>())
        }
        else {
            compiler.initProcessAnnotations(wrappedProcessors)
        }

        val parsedJavaFiles = parseJavaFiles(javaSourceFiles)

        compilerAfterAP = try {
            javaLog.interceptorData.files = parsedJavaFiles.map { it.sourceFile to it }.toMap()
            val analyzedFiles = compiler.stopIfErrorOccurred(
                    CompileState.PARSE, compiler.enterTrees(parsedJavaFiles + additionalSources))

            if (isJava9OrLater()) {
                val processAnnotationsMethod = compiler.javaClass.getMethod("processAnnotations", JavacList::class.java)
                processAnnotationsMethod.invoke(compiler, analyzedFiles)
                compiler
            }
            else {
                compiler.processAnnotations(analyzedFiles)
            }
        } catch (e: AnnotationProcessingError) {
            throw KaptBaseError(KaptBaseError.Kind.EXCEPTION, e.cause ?: e)
        }

        val log = compilerAfterAP.log

        val filer = processingEnvironment.filer as JavacFiler
        val errorCount = log.nerrors
        val warningCount = log.nwarnings

        if (logger.isVerbose) {
            logger.info("Annotation processing complete, errors: $errorCount, warnings: $warningCount")

            logger.info("Annotation processor stats:")
            wrappedProcessors.forEach { processor ->
                logger.info(processor.renderSpentTime())
            }

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

private class ProcessorWrapper(private val delegate: Processor) : Processor by delegate {
    private var initTime: Long = 0
    private val roundTime = mutableListOf<Long>()

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val (time, result) = measureTimeMillisWithResult {
            delegate.process(annotations, roundEnv)
        }

        roundTime += time
        return result
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
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
        val processorName = delegate.javaClass.simpleName
        val totalTime = initTime + roundTime.sum()

        return "$processorName: " +
                "total: $totalTime ms, " +
                "init: $initTime ms, " +
                "${roundTime.size} round(s): ${roundTime.joinToString { "$it ms" }}"
    }
}

fun KaptContext.parseJavaFiles(javaSourceFiles: List<File>): JavacList<JCTree.JCCompilationUnit> {
    val javaFileObjects = fileManager.getJavaFileObjectsFromFiles(javaSourceFiles)

    return compiler.stopIfErrorOccurred(CompileState.PARSE,
                    initModulesIfNeeded(
                            compiler.stopIfErrorOccurred(CompileState.PARSE,
                                    compiler.parseFiles(javaFileObjects))))
}

private fun KaptContext.initModulesIfNeeded(files: JavacList<JCTree.JCCompilationUnit>): JavacList<JCTree.JCCompilationUnit> {
    if (isJava9OrLater()) {
        val initModulesMethod = compiler.javaClass.getMethod("initModules", JavacList::class.java)

        @Suppress("UNCHECKED_CAST")
        return compiler.stopIfErrorOccurred(
                CompileState.PARSE,
                initModulesMethod.invoke(compiler, files) as JavacList<JCTree.JCCompilationUnit>)
    }

    return files
}