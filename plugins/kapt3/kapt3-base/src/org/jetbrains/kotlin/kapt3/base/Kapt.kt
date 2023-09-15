/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.jetbrains.kotlin.kapt3.base.util.doOpenInternalPackagesIfRequired
import org.jetbrains.kotlin.kapt3.base.util.info
import kotlin.system.measureTimeMillis

object Kapt {
    private const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"

    @JvmStatic
    @Suppress("unused")
    fun kaptFlags(rawFlags: Set<String>): KaptFlags {
        return KaptFlags.fromSet(KaptFlag.values().filterTo(mutableSetOf()) { it.name in rawFlags })
    }

    @JvmStatic
    @Suppress("unused")
    fun kapt(options: KaptOptions): Boolean {
        doOpenInternalPackagesIfRequired()
        val logger = WriterBackedKaptLogger(options[KaptFlag.VERBOSE])

        if (!Kapt.checkJavacComponentsAccess(logger)) {
            return false
        }

        val kaptContext = KaptContext(options, false, logger)

        logger.info { options.logString("stand-alone mode") }

        val javaSourceFiles = options.collectJavaSourceFiles(kaptContext.sourcesToReprocess)

        val processorLoader = ProcessorLoader(options, logger)

        processorLoader.use {
            val processors = processorLoader.loadProcessors(findClassLoaderWithJavac())

            val annotationProcessingTime = measureTimeMillis {
                kaptContext.doAnnotationProcessing(
                    javaSourceFiles,
                    processors.processors,
                    binaryTypesToReprocess = collectAggregatedTypes(kaptContext.sourcesToReprocess)
                )
            }

            logger.info { "Annotation processing took $annotationProcessingTime ms" }
        }

        return true
    }

    fun checkJavacComponentsAccess(logger: KaptLogger): Boolean {
        try {
            Class.forName(JAVAC_CONTEXT_CLASS)
            return true
        } catch (e: ClassNotFoundException) {
            logger.error("'$JAVAC_CONTEXT_CLASS' class can't be found ('tools.jar' is absent in the plugin classpath). Kapt won't work.")
            return false
        }
    }

    private fun findClassLoaderWithJavac(): ClassLoader {
        // Class.getClassLoader() may return null if the class is defined in a bootstrap class loader
        return Context::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
    }
}