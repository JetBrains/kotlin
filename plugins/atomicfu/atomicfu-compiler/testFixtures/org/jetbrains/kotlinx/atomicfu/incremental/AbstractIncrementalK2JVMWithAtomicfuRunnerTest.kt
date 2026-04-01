/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.incremental

import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

abstract class AbstractIncrementalK2JVMWithAtomicfuRunnerTest : AbstractIncrementalK2JvmCompilerRunnerTest() {
    companion object {
        private const val PLUGIN_JAR_NAME = "kotlin-atomicfu-compiler-plugin"

        private fun findPluginJarPath(name: String, taskName: String): String {
            val pluginJarClasspath = System.getProperty("atomicfu.compiler.plugin")
                ?.split(File.pathSeparator)
                .orEmpty()

            val pluginJar = pluginJarClasspath.firstOrNull {
                val file = File(it)
                file.name.startsWith(name) && file.extension == "jar"
            } ?: error("Jar $name does not exist. Please run $taskName")

            return File(pluginJar).canonicalPath
        }

        private fun getLibraryJar(classToDetect: String): File? = try {
            PathUtil.getResourcePathForClass(Class.forName(classToDetect))
        } catch (e: ClassNotFoundException) {
            null
        }

        private fun getLibrariesPaths(): List<File> {
            val coreLibraryPath = getLibraryJar("kotlinx.atomicfu.AtomicFU") ?: error("kotlinx.atomicfu library is not found")
            val kotlinTestPath = getLibraryJar("kotlin.test.AssertionsKt") ?: error("kotlin.test is not found")
            val kotlinJvm = getLibraryJar("kotlin.jvm.JvmField") ?: error("kotlin-stdlib is not found")
            return listOf(coreLibraryPath, kotlinTestPath, kotlinJvm)
        }
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            val pluginJar = findPluginJarPath(PLUGIN_JAR_NAME, ":kotlin-atomicfu-compiler-plugin:jar")
            val libraries = getLibrariesPaths()
            val librariesPath = libraries.map { it.canonicalPath }.joinToString(File.pathSeparator)
            classpath += "${File.pathSeparator}$librariesPath"
            pluginClasspaths = arrayOf(pluginJar)
        }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true, isFirEnabled = true)
}
