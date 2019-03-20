/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import com.sun.tools.javac.code.Symbol
import java.io.File
import java.net.URI
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject

private val ALLOWED_RUNTIME_TYPES = setOf(RuntimeProcType.AGGREGATING.name, RuntimeProcType.ISOLATING.name)

class IncrementalProcessor(private val processor: Processor, val kind: DeclaredProcType) : Processor by processor {

    private var dependencyCollector = lazy { createDependencyCollector() }

    override fun init(processingEnv: ProcessingEnvironment) {
        if (kind == DeclaredProcType.NON_INCREMENTAL) {
            processor.init(processingEnv)
        } else {
            val originalFiler = processingEnv.filer
            val incrementalFiler = IncrementalFiler(originalFiler)
            val incProcEnvironment = IncrementalProcessingEnvironment(processingEnv, incrementalFiler)
            processor.init(incProcEnvironment)
            incrementalFiler.dependencyCollector = dependencyCollector.value
        }
    }

    /** This has to invoked only once the processors has been initialized, because this accesses Processor.getSupportedOptions(). */
    private fun createDependencyCollector(): AnnotationProcessorDependencyCollector {
        val type = if (kind == DeclaredProcType.DYNAMIC) {
            val fromOptions = supportedOptions.singleOrNull { it.startsWith("org.gradle.annotation.processing.") }
            if (fromOptions == null) {
                RuntimeProcType.NON_INCREMENTAL
            } else {
                val declaredType = fromOptions.drop("org.gradle.annotation.processing.".length).toUpperCase()
                if (ALLOWED_RUNTIME_TYPES.contains(declaredType)) {
                    enumValueOf(declaredType)
                } else {
                    RuntimeProcType.NON_INCREMENTAL
                }
            }
        } else {
            kind.toRuntimeType()
        }

        return AnnotationProcessorDependencyCollector(type)
    }

    fun getGeneratedToSources() = dependencyCollector.value.getGeneratedToSources()
    fun getRuntimeType(): RuntimeProcType = dependencyCollector.value.getRuntimeType()
}

internal class IncrementalProcessingEnvironment(private val processingEnv: ProcessingEnvironment, private val incFiler: IncrementalFiler) :
    ProcessingEnvironment by processingEnv {
    override fun getFiler(): Filer = incFiler
}

internal class IncrementalFiler(private val filer: Filer) : Filer by filer {

    internal var dependencyCollector: AnnotationProcessorDependencyCollector? = null

    override fun createSourceFile(name: CharSequence?, vararg originatingElements: Element?): JavaFileObject {
        val createdSourceFile = filer.createSourceFile(name, *originatingElements)
        dependencyCollector!!.add(createdSourceFile.toUri(), originatingElements)
        return createdSourceFile
    }

    override fun createClassFile(name: CharSequence?, vararg originatingElements: Element?): JavaFileObject {
        val createdClassFile = filer.createClassFile(name, *originatingElements)
        dependencyCollector!!.add(createdClassFile.toUri(), originatingElements)
        return createdClassFile
    }

    override fun createResource(
        location: JavaFileManager.Location?,
        pkg: CharSequence?,
        relativeName: CharSequence?,
        vararg originatingElements: Element?
    ): FileObject {
        val createdResource = filer.createResource(location, pkg, relativeName, *originatingElements)
        dependencyCollector!!.add(createdResource.toUri(), originatingElements)

        return createdResource
    }
}

internal class AnnotationProcessorDependencyCollector(private val runtimeProcType: RuntimeProcType) {
    private val generatedToSource = mutableMapOf<File, File?>()
    private var isFullRebuild = !runtimeProcType.isIncremental

    internal fun add(createdFile: URI, originatingElements: Array<out Element?>) {
        if (isFullRebuild) return

        val generatedFile = File(createdFile)
        if (runtimeProcType == RuntimeProcType.AGGREGATING) {
            generatedToSource[generatedFile] = null
        } else {
            val srcFiles = getSrcFiles(originatingElements)
            if (srcFiles.size != 1) {
                isFullRebuild = true
            } else {
                generatedToSource[generatedFile] = srcFiles.single()
            }
        }
    }

    internal fun getGeneratedToSources(): Map<File, File?> = if (isFullRebuild) emptyMap() else generatedToSource
    internal fun getRuntimeType(): RuntimeProcType {
        return if (isFullRebuild) {
            RuntimeProcType.NON_INCREMENTAL
        } else {
            runtimeProcType
        }
    }
}

private fun getSrcFiles(elements: Array<out Element?>): List<File> {
    return elements.filterNotNull().mapNotNull { elem ->
        var origin = elem
        while (origin.enclosingElement != null && origin.enclosingElement !is PackageElement) {
            origin = origin.enclosingElement
        }
        val uri = (origin as? Symbol.ClassSymbol)?.sourcefile?.toUri()?.takeIf { it.isAbsolute }
        uri?.let { File(it) }
    }
}

enum class DeclaredProcType {
    AGGREGATING {
        override fun toRuntimeType() = RuntimeProcType.AGGREGATING
    },
    ISOLATING {
        override fun toRuntimeType() = RuntimeProcType.ISOLATING
    },
    DYNAMIC {
        override fun toRuntimeType() = throw IllegalStateException("This should not be used")
    },
    NON_INCREMENTAL {
        override fun toRuntimeType() = RuntimeProcType.NON_INCREMENTAL
    };

    abstract fun toRuntimeType(): RuntimeProcType
}

enum class RuntimeProcType(val isIncremental: Boolean) {
    AGGREGATING(true),
    ISOLATING(true),
    NON_INCREMENTAL(false),
}