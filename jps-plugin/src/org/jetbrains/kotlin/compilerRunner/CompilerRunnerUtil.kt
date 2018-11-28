/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import org.jetbrains.kotlin.utils.KotlinPaths

import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.utils.SmartList

object CompilerRunnerUtil {
    private var ourClassLoaderRef = SoftReference<ClassLoader>(null)

    internal val jdkToolsJar: File?
        get() {
            val javaHomePath = System.getProperty("java.home")
            if (javaHomePath == null || javaHomePath.isEmpty()) {
                return null
            }
            val javaHome = File(javaHomePath)
            var toolsJar = File(javaHome, "lib/tools.jar")
            if (toolsJar.exists()) {
                return toolsJar.canonicalFile
            }

            // We might be inside jre.
            if (javaHome.name == "jre") {
                toolsJar = File(javaHome.parent, "lib/tools.jar")
                if (toolsJar.exists()) {
                    return toolsJar.canonicalFile
                }
            }

            return null
        }

    @Synchronized
    private fun getOrCreateClassLoader(
        environment: JpsCompilerEnvironment,
        paths: List<File>
    ): ClassLoader {
        var classLoader = ourClassLoaderRef.get()
        if (classLoader == null) {
            classLoader = ClassPreloadingUtils.preloadClasses(
                paths,
                Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                CompilerRunnerUtil::class.java.classLoader,
                environment.classesToLoadByParent
            )
            ourClassLoaderRef = SoftReference(classLoader)
        }
        return classLoader!!
    }

    fun getLibPath(paths: KotlinPaths, messageCollector: MessageCollector): File? {
        val libs = paths.libPath
        if (libs.exists() && !libs.isFile) return libs

        messageCollector.report(
            ERROR,
            "Broken compiler at '" + libs.absolutePath + "'. Make sure plugin is properly installed", null
        )

        return null
    }

    fun invokeExecMethod(
        compilerClassName: String,
        arguments: Array<String>,
        environment: JpsCompilerEnvironment,
        out: PrintStream
    ): Any? = withCompilerClassloader(environment) { classLoader ->
        val compiler = Class.forName(compilerClassName, true, classLoader)
        val exec = compiler.getMethod(
            "execAndOutputXml",
            PrintStream::class.java,
            Class.forName("org.jetbrains.kotlin.config.Services", true, classLoader),
            Array<String>::class.java
        )
        exec.invoke(compiler.newInstance(), out, environment.services, arguments)
    }

    fun invokeClassesFqNames(
        environment: JpsCompilerEnvironment,
        files: Set<File>
    ): Set<String> = withCompilerClassloader(environment) { classLoader ->
        val klass = Class.forName("org.jetbrains.kotlin.incremental.parsing.ParseFileUtilsKt", true, classLoader)
        val method = klass.getMethod("classesFqNames", Set::class.java)
        @Suppress("UNCHECKED_CAST")
        method.invoke(klass, files) as? Set<String>
    } ?: emptySet()

    private fun <T> withCompilerClassloader(
        environment: JpsCompilerEnvironment,
        fn: (ClassLoader) -> T
    ): T? {
        val paths = SmartList<File>()

        val libPath = getLibPath(environment.kotlinPaths, environment.messageCollector) ?: return null
        paths.add(File(libPath, "kotlin-compiler.jar"))
        jdkToolsJar?.let { paths.add(it) }

        val classLoader = getOrCreateClassLoader(environment, paths)
        return fn(classLoader)
    }
}
