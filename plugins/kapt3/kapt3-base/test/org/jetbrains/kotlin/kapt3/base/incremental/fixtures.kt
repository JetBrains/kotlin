/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test.org.jetbrains.kotlin.kapt3.base.incremental

import com.sun.source.util.TaskListener
import com.sun.tools.javac.api.JavacTaskImpl
import org.jetbrains.kotlin.kapt3.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt3.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt3.base.incremental.RuntimeProcType
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.StandardLocation
import javax.tools.ToolProvider

val TEST_DATA_DIR = File("plugins/kapt3/kapt3-base/testData/runner/incremental")

fun runAnnotationProcessing(
    srcFiles: List<File>,
    processor: List<IncrementalProcessor>,
    generatedSources: File,
    listener: (Elements) -> TaskListener? = { null }
) {
    val compiler = ToolProvider.getSystemJavaCompiler()
    compiler.getStandardFileManager(null, null, null).use { fileManager ->
        val javaSrcs = fileManager.getJavaFileObjectsFromFiles(srcFiles)
        val compilationTask =
            compiler.getTask(
                null,
                fileManager,
                null,
                listOf("-proc:only", "-s", generatedSources.absolutePath, "-d", generatedSources.absolutePath),
                null,
                javaSrcs
            ) as JavacTaskImpl

        val taskListener = listener(compilationTask.elements)
        taskListener?.let { compilationTask.addTaskListener(it) }

        compilationTask.setProcessors(processor)
        compilationTask.call()
    }
}

fun compileSources(srcFiles: Iterable<File>, outputDir: File) {
    val compiler = ToolProvider.getSystemJavaCompiler()
    compiler.getStandardFileManager(null, null, null).use { fileManager ->
        val compilationTask =
            compiler.getTask(
                null,
                fileManager,
                null,
                listOf("-d", outputDir.absolutePath),
                null,
                fileManager.getJavaFileObjectsFromFiles(srcFiles)
            ) as JavacTaskImpl

        compilationTask.call()
    }
}

fun SimpleProcessor.toAggregating() = IncrementalProcessor(this, DeclaredProcType.AGGREGATING)
fun SimpleProcessor.toIsolating() = IncrementalProcessor(this, DeclaredProcType.ISOLATING)
fun SimpleProcessor.toNonIncremental() = IncrementalProcessor(this, DeclaredProcType.NON_INCREMENTAL)
fun DynamicProcessor.toDynamic() = IncrementalProcessor(this, DeclaredProcType.DYNAMIC)

open class SimpleProcessor(private val wrongOrigin: Boolean = false, private val generatedSuffix: String = "") : AbstractProcessor() {
    lateinit var filer: Filer

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        filer = processingEnv!!.filer
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf("test.Observable")

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty()) return false

        roundEnv.getElementsAnnotatedWith(annotations.single()).forEach {
            it as TypeElement

            val generatedName = "${it.qualifiedName}Generated$generatedSuffix"
            filer.createSourceFile(generatedName, it.takeUnless { wrongOrigin }).openWriter().use { it.write("") }
        }

        return false
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }
}

class DynamicProcessor(private val kind: RuntimeProcType) : SimpleProcessor() {
    override fun getSupportedOptions(): MutableSet<String> {
        return mutableSetOf("org.gradle.annotation.processing.${kind.name}")
    }
}

class SimpleCreatingClassFilesAndResources : SimpleProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        super.process(annotations, roundEnv)

        if (annotations.isEmpty()) return false
        roundEnv.getElementsAnnotatedWith(annotations.single()).forEach {
            it as TypeElement

            val generatedName = "${it.qualifiedName}Generated"
            filer.createClassFile("${generatedName}Class", it).openWriter().use { it.write("") }
            filer.createResource(StandardLocation.SOURCE_OUTPUT, "test", "${it.simpleName}GeneratedResource", it).openWriter().use { it.write("") }
        }

        return false
    }
}

class SimpleGeneratingIfTypeDoesNotExist: SimpleProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty()) return false

        roundEnv.getElementsAnnotatedWith(annotations.single()).forEach { element ->
            element as TypeElement
            val generatedName = "${element.qualifiedName}Generated"

            if (processingEnv.elementUtils.getTypeElement(generatedName) == null) {
                filer.createSourceFile(generatedName, element).openWriter().use {
                    it.write(
                        """
                package test;
                public class ${element.simpleName}Generated {}
            """.trimIndent()
                    )
                }
            }
        }

        return false
    }
}