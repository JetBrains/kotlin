/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base

import org.jetbrains.kotlin.kapt.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt.base.incremental.INCREMENTAL_ANNOTATION_FLAG
import org.jetbrains.kotlin.kapt.base.incremental.parseIncrementalProcessorDeclarations
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import org.jetbrains.kotlin.kapt.base.util.info
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader
import java.util.zip.ZipFile
import javax.annotation.processing.Processor

class LoadedProcessors(val processors: List<IncrementalProcessor>, val classLoader: ClassLoader)

interface ProcessorLoader : Closeable {
    fun loadProcessors(parentClassLoader: ClassLoader = ClassLoader.getSystemClassLoader()): LoadedProcessors
}

open class ProcessorLoaderImpl(private val options: KaptOptions, private val logger: KaptLogger) : ProcessorLoader {
    private companion object {
        const val SERVICE_FILE = "META-INF/services/javax.annotation.processing.Processor"
    }

    private var annotationProcessingClassLoader: URLClassLoader? = null

    override fun loadProcessors(parentClassLoader: ClassLoader): LoadedProcessors {
        val classpath = LinkedHashSet<File>().apply {
            addAll(options.processingClasspath)
            if (options[KaptFlag.INCLUDE_COMPILE_CLASSPATH]) {
                addAll(options.compileClasspath)
            }
        }

        val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), parentClassLoader)
        this.annotationProcessingClassLoader = classLoader

        val classpathScan = scanClasspath(classpath)

        val processors = if (options.processors.isNotEmpty()) {
            logger.info("Annotation processor class names are set, skip AP discovery")
            options.processors.mapNotNull { tryLoadProcessor(it, classLoader) }
        } else {
            logger.info("Need to discovery annotation processors in the AP classpath")
            doLoadProcessors(classpath, classLoader, classpathScan)
        }

        if (processors.isEmpty()) {
            logger.info("No annotation processors available, aborting")
        } else {
            logger.info { "Annotation processors: " + processors.joinToString { it::class.java.canonicalName } }
        }

        return LoadedProcessors(wrapInIncrementalProcessor(processors, classpathScan), classLoader)
    }

    private fun wrapInIncrementalProcessor(processors: List<Processor>, classpathScan: ClasspathScan): List<IncrementalProcessor> {
        if (options.incrementalCache == null) {
            return processors.map { IncrementalProcessor(it, DeclaredProcType.NON_INCREMENTAL, logger) }
        }

        val processorNames = processors.map { it.javaClass.name }.toSet()

        val processorsInfo: Map<String, DeclaredProcType> =
            classpathScan.incrementalMarkers.filterKeys { it in processorNames }

        val nonIncremental = processorNames.filter { !processorsInfo.containsKey(it) }
        return processors.map { processor ->
            val procType = processorsInfo[processor.javaClass.name]?.let {
                if (nonIncremental.isEmpty()) {
                    it
                } else {
                    DeclaredProcType.INCREMENTAL_BUT_OTHER_APS_ARE_NOT
                }
            } ?: DeclaredProcType.NON_INCREMENTAL
            IncrementalProcessor(processor, procType, logger)
        }
    }

    /** Processor service declarations and incremental-processor markers, read in one pass. */
    class ClasspathScan(
        val processorNames: Set<String>,
        val incrementalMarkers: Map<String, DeclaredProcType>,
    )

    /**
     * Reads both META-INF descriptors kapt cares about, opening each classpath entry exactly once.
     *
     * Do not use `ServiceLoader` here: it uses the `JarFileFactory` cache, which is not cleared
     * properly and causes issues on Windows. Manually clearing those caches caused race conditions,
     * as `JarFileFactory` is shared between concurrent runs in the same class loader.
     * See https://youtrack.jetbrains.com/issue/KT-34604 and https://youtrack.jetbrains.com/issue/KT-22513.
     */
    protected fun scanClasspath(classpath: Iterable<File>): ClasspathScan {
        val processorNames = mutableSetOf<String>()
        val incrementalMarkers = mutableMapOf<String, DeclaredProcType>()

        fun addServiceNames(lines: Sequence<String>) {
            lines.forEach { line ->
                val processedLine = line.substringBefore("#").trim()
                if (processedLine.isNotEmpty()) {
                    processorNames.add(processedLine)
                }
            }
        }

        for (file in classpath) {
            when {
                file.isDirectory -> {
                    file.resolve(SERVICE_FILE).takeIf { it.isFile }?.let { serviceFileInDir ->
                        serviceFileInDir.inputStream().use { addServiceNames(it.bufferedReader().lineSequence()) }
                    }
                    file.resolve(INCREMENTAL_ANNOTATION_FLAG).takeIf { it.isFile }?.let { markerFile ->
                        incrementalMarkers += parseIncrementalProcessorDeclarations(markerFile.bufferedReader().readLines())
                    }
                }
                file.isFile && file.extension.equals("jar", ignoreCase = true) -> {
                    ZipFile(file).use { zipFile ->
                        zipFile.getEntry(SERVICE_FILE)?.let { zipEntry ->
                            zipFile.getInputStream(zipEntry).use { addServiceNames(it.bufferedReader().lineSequence()) }
                        }
                        zipFile.getEntry(INCREMENTAL_ANNOTATION_FLAG)?.let { zipEntry ->
                            zipFile.getInputStream(zipEntry).use {
                                incrementalMarkers += parseIncrementalProcessorDeclarations(it.bufferedReader().readLines())
                            }
                        }
                    }
                }
                else -> {
                    logger.info("$file cannot be used to locate $SERVICE_FILE file.")
                }
            }
        }

        return ClasspathScan(processorNames, incrementalMarkers)
    }

    open fun doLoadProcessors(
        classpath: LinkedHashSet<File>,
        classLoader: ClassLoader,
        classpathScan: ClasspathScan,
    ): List<Processor> {
        return classpathScan.processorNames.mapNotNull { tryLoadProcessor(it, classLoader) }
    }

    private fun tryLoadProcessor(fqName: String, classLoader: ClassLoader): Processor? {
        val providedClassloader = options.processingClassLoader?.takeIf { !options.separateClassloaderForProcessors.contains(fqName) }
        val classLoaderToUse = if (providedClassloader != null) {
            logger.info { "Use provided ClassLoader for processor '$fqName'" }
            providedClassloader
        } else {
            logger.info { "Use own ClassLoader for processor '$fqName'" }
            classLoader
        }

        val annotationProcessorClass = try {
            Class.forName(fqName, true, classLoaderToUse)
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

    override fun close() {
        annotationProcessingClassLoader?.close()
    }
}
