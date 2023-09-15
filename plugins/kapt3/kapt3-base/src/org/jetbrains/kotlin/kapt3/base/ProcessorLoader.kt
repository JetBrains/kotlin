/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import org.jetbrains.kotlin.kapt3.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt3.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt3.base.incremental.getIncrementalProcessorsFromClasspath
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.info
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader
import java.util.zip.ZipFile
import javax.annotation.processing.Processor

class LoadedProcessors(val processors: List<IncrementalProcessor>, val classLoader: ClassLoader)

open class ProcessorLoader(private val options: KaptOptions, private val logger: KaptLogger) : Closeable {
    private var annotationProcessingClassLoader: URLClassLoader? = null

    fun loadProcessors(parentClassLoader: ClassLoader = ClassLoader.getSystemClassLoader()): LoadedProcessors {
        val classpath = LinkedHashSet<File>().apply {
            addAll(options.processingClasspath)
            if (options[KaptFlag.INCLUDE_COMPILE_CLASSPATH]) {
                addAll(options.compileClasspath)
            }
        }

        val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), parentClassLoader)
        this.annotationProcessingClassLoader = classLoader

        val processors = if (options.processors.isNotEmpty()) {
            logger.info("Annotation processor class names are set, skip AP discovery")
            options.processors.mapNotNull { tryLoadProcessor(it, classLoader) }
        } else {
            logger.info("Need to discovery annotation processors in the AP classpath")
            doLoadProcessors(classpath, classLoader)
        }

        if (processors.isEmpty()) {
            logger.info("No annotation processors available, aborting")
        } else {
            logger.info { "Annotation processors: " + processors.joinToString { it::class.java.canonicalName } }
        }

        return LoadedProcessors(wrapInIncrementalProcessor(processors, classpath), classLoader)
    }

    private fun wrapInIncrementalProcessor(processors: List<Processor>, classpath: Iterable<File>): List<IncrementalProcessor> {
        if (options.incrementalCache == null) {
            return processors.map { IncrementalProcessor(it, DeclaredProcType.NON_INCREMENTAL, logger) }
        }

        val processorNames = processors.map {it.javaClass.name}.toSet()

        val processorsInfo: Map<String, DeclaredProcType> = getIncrementalProcessorsFromClasspath(processorNames, classpath)

        val nonIncremental = processorNames.filter { !processorsInfo.containsKey(it) }
        return processors.map {
            val procType = processorsInfo[it.javaClass.name]?.let {
                if (nonIncremental.isEmpty()) {
                    it
                } else {
                    DeclaredProcType.INCREMENTAL_BUT_OTHER_APS_ARE_NOT
                }
            } ?: DeclaredProcType.NON_INCREMENTAL
            IncrementalProcessor(it, procType, logger)
        }
    }

    open fun doLoadProcessors(classpath: LinkedHashSet<File>, classLoader: ClassLoader): List<Processor> {
        val processorNames = mutableSetOf<String>()

        fun processSingleInput(input: InputStream) {
            val lines = input.bufferedReader().lineSequence()
            lines.forEach { line ->
                val processedLine = line.substringBefore("#").trim()
                if (processedLine.isNotEmpty()) {
                    processorNames.add(processedLine)
                }
            }
        }
        // Do not use ServiceLoader as it uses JarFileFactory cache which is not cleared
        // properly. This may cause issues on Windows.
        // Previously, JarFileFactory caches were manually cleaned, but that caused race conditions,
        // as JarFileFactory was shared between concurrent runs in the same class loader.
        // See https://youtrack.jetbrains.com/issue/KT-34604 for more details. Similar issue
        // is also https://youtrack.jetbrains.com/issue/KT-22513.
        val serviceFile = "META-INF/services/javax.annotation.processing.Processor"
        for (file in classpath) {
            when {
                file.isDirectory -> {
                    file.resolve(serviceFile).takeIf { it.isFile }?.let {
                        processSingleInput(it.inputStream())
                    }
                }
                file.isFile && file.extension.equals("jar", ignoreCase = true) -> {
                    ZipFile(file).use { zipFile ->
                        zipFile.getEntry(serviceFile)?.let { zipEntry ->
                            zipFile.getInputStream(zipEntry).use {
                                processSingleInput(it)
                            }
                        }
                    }
                }
                else -> {
                    logger.info("$file cannot be used to locate $serviceFile file.")
                }
            }
        }

        return processorNames.mapNotNull { tryLoadProcessor(it, classLoader) }
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
