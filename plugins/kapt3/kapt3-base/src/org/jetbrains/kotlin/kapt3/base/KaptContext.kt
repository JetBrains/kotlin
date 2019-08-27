/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.kapt3.base.incremental.JavaClassCacheManager
import org.jetbrains.kotlin.kapt3.base.incremental.SourcesToReprocess
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaCompiler
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaFileManager
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.isJava9OrLater
import org.jetbrains.kotlin.kapt3.base.util.putJavacOption
import java.io.Closeable
import java.io.File
import javax.tools.JavaFileManager

open class KaptContext(val options: KaptOptions, val withJdk: Boolean, val logger: KaptLogger) : Closeable {
    val context = Context()
    val compiler: KaptJavaCompiler
    val fileManager: KaptJavaFileManager
    private val javacOptions: Options
    val javaLog: KaptJavaLog
    val cacheManager: JavaClassCacheManager?

    val sourcesToReprocess: SourcesToReprocess

    protected open fun preregisterTreeMaker(context: Context) {}

    private fun preregisterLog(context: Context) {
        val interceptorData = KaptJavaLog.DiagnosticInterceptorData()
        context.put(Log.logKey, Context.Factory<Log> { newContext ->
            KaptJavaLog(
                options.projectBaseDir, newContext, logger.errorWriter, logger.warnWriter, logger.infoWriter,
                interceptorData, options[KaptFlag.MAP_DIAGNOSTIC_LOCATIONS]
            )
        })
    }

    init {
        preregisterLog(context)
        KaptJavaFileManager.preRegister(context)

        @Suppress("LeakingThis")
        preregisterTreeMaker(context)

        KaptJavaCompiler.preRegister(context)

        cacheManager = options.incrementalCache?.let {
            JavaClassCacheManager(it)
        }
        if (options.flags[KaptFlag.INCREMENTAL_APT]) {
            sourcesToReprocess =
                cacheManager?.invalidateAndGetDirtyFiles(
                    options.changedFiles, options.classpathChanges
                ) ?: SourcesToReprocess.FullRebuild

            if (sourcesToReprocess == SourcesToReprocess.FullRebuild) {
                // remove all generated sources and classes
                fun deleteAndCreate(dir: File) {
                    if (!dir.deleteRecursively()) logger.warn("Unable to delete $dir.")
                    if (!dir.mkdir()) logger.warn("Unable to create $dir.")
                }
                deleteAndCreate(options.sourcesOutputDir)
                deleteAndCreate(options.classesOutputDir)
                options.getKotlinGeneratedSourcesDirectory()?.let {
                    deleteAndCreate(it)
                }
            }
        } else {
            sourcesToReprocess = SourcesToReprocess.FullRebuild
        }

        javacOptions = Options.instance(context).apply {
            for ((key, value) in options.processingOptions) {
                val option = if (value.isEmpty()) "-A$key" else "-A$key=$value"
                put(option, option) // key == value: it's intentional
            }

            for ((key, value) in options.javacOptions) {
                if (value.isNotEmpty()) {
                    put(key, value)
                } else {
                    put(key, key)
                }
            }

            put(Option.PROC, "only") // Only process annotations

            if (!withJdk && !isJava9OrLater()) {
                // No boot classpath for JDK 8 and below. When running on JDK9+ and specifying source level 8 and below,
                // boot classpath is not set to empty. This is to allow types to be resolved using boot classpath which defaults to
                // classes defined in java.base module. See https://youtrack.jetbrains.com/issue/KT-33028 for details.
                put(Option.valueOf("BOOTCLASSPATH"), "")
            }

            if (isJava9OrLater()) {
                put("accessInternalAPI", "true")
            }

            val compileClasspath = if (sourcesToReprocess is SourcesToReprocess.FullRebuild) {
                options.compileClasspath
            } else {
                options.compileClasspath + options.compiledSources
            }

            putJavacOption("CLASSPATH", "CLASS_PATH",
                           compileClasspath.joinToString(File.pathSeparator) { it.canonicalPath })

            @Suppress("SpellCheckingInspection")
            putJavacOption("PROCESSORPATH", "PROCESSOR_PATH",
                           options.processingClasspath.joinToString(File.pathSeparator) { it.canonicalPath })

            put(Option.S, options.sourcesOutputDir.canonicalPath)
            put(Option.D, options.classesOutputDir.canonicalPath)
            put(Option.ENCODING, "UTF-8")
        }

        if (logger.isVerbose) {
            logger.info("All Javac options: " + javacOptions.keySet().associateBy({ it }) { key -> javacOptions[key] ?: "" })
        }

        fileManager = context.get(JavaFileManager::class.java) as KaptJavaFileManager
        if (sourcesToReprocess is SourcesToReprocess.Incremental) {
            fileManager.typeToIgnore = sourcesToReprocess.dirtyTypes
            fileManager.rootsToFilter = options.compiledSources.toSet()
        }

        if (isJava9OrLater()) {
            for (option in Option.getJavacFileManagerOptions()) {
                val value = javacOptions.get(option) ?: continue
                fileManager.handleOptionJavac9(option, value)
            }
        }

        compiler = JavaCompiler.instance(context) as KaptJavaCompiler
        compiler.keepComments = true

        ClassReader.instance(context).saveParameterNames = true

        javaLog = compiler.log as KaptJavaLog
    }

    override fun close() {
        cacheManager?.close()
        compiler.close()
        fileManager.close()
    }
}