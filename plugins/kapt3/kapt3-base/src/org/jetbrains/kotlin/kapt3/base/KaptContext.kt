/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaCompiler
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaFileManager
import org.jetbrains.kotlin.kapt3.base.javac.KaptJavaLog
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.isJava9OrLater
import org.jetbrains.kotlin.kapt3.base.util.putJavacOption
import java.io.File
import javax.tools.JavaFileManager

open class KaptContext(
    val paths: KaptPaths,
    val withJdk: Boolean,
    val logger: KaptLogger,
    private val mapDiagnosticLocations: Boolean,
    processorOptions: Map<String, String>,
    javacOptions: Map<String, String> = emptyMap()
) : AutoCloseable {
    val context = Context()
    val compiler: KaptJavaCompiler
    val fileManager: KaptJavaFileManager
    val options: Options
    val javaLog: KaptJavaLog

    protected open fun preregisterTreeMaker(context: Context) {}

    private fun preregisterLog(context: Context) {
        val interceptorData = KaptJavaLog.DiagnosticInterceptorData()
        context.put(Log.logKey, Context.Factory<Log> { newContext ->
            KaptJavaLog(
                paths.projectBaseDir, newContext, logger.errorWriter, logger.warnWriter, logger.infoWriter,
                interceptorData, mapDiagnosticLocations
            )
        })
    }

    init {
        preregisterLog(context)
        KaptJavaFileManager.preRegister(context)

        @Suppress("LeakingThis")
        preregisterTreeMaker(context)

        KaptJavaCompiler.preRegister(context)

        options = Options.instance(context).apply {
            for ((key, value) in processorOptions) {
                val option = if (value.isEmpty()) "-A$key" else "-A$key=$value"
                put(option, option) // key == value: it's intentional
            }

            for ((key, value) in javacOptions) {
                if (value.isNotEmpty()) {
                    put(key, value)
                } else {
                    put(key, key)
                }
            }

            put(Option.PROC, "only") // Only process annotations

            if (!withJdk) {
                putJavacOption("BOOTCLASSPATH", "BOOT_CLASS_PATH", "") // No boot classpath
            }

            if (isJava9OrLater()) {
                put("accessInternalAPI", "true")
            }

            putJavacOption("CLASSPATH", "CLASS_PATH",
                           paths.compileClasspath.joinToString(File.pathSeparator) { it.canonicalPath })
            putJavacOption("PROCESSORPATH", "PROCESSOR_PATH",
                           paths.annotationProcessingClasspath.joinToString(File.pathSeparator) { it.canonicalPath })

            put(Option.S, paths.sourcesOutputDir.canonicalPath)
            put(Option.D, paths.classFilesOutputDir.canonicalPath)
            put(Option.ENCODING, "UTF-8")
        }

        if (logger.isVerbose) {
            logger.info("All Javac options: " + options.keySet().associateBy({ it }) { key -> options[key] ?: "" })
        }

        fileManager = context.get(JavaFileManager::class.java) as KaptJavaFileManager

        if (isJava9OrLater()) {
            for (option in Option.getJavacFileManagerOptions()) {
                val value = options.get(option) ?: continue
                fileManager.handleOptionJavac9(option, value)
            }
        }

        compiler = JavaCompiler.instance(context) as KaptJavaCompiler
        compiler.keepComments = true

        ClassReader.instance(context).saveParameterNames = true

        javaLog = compiler.log as KaptJavaLog
    }

    override fun close() {
        compiler.close()
        fileManager.close()
    }
}