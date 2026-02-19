/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.incremental

import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.fail

import java.io.File

abstract class AbstractIncrementalK2JVMWithAtomicfuRunnerTest : AbstractIncrementalK2JvmCompilerRunnerTest() {
    companion object {
        private const val PLUGIN_JAR_DIR = "plugins/atomicfu/atomicfu-compiler/build/libs/"
        private const val PLUGIN_JAR_NAME = "kotlin-atomicfu-compiler-plugin"

        private fun findJar(dir: String, name: String, taskName: String): String {
            val failMessage = { "Jar $name does not exist. Please run $taskName" }
            val libDir = File(dir)
            kotlin.test.assertTrue(libDir.exists() && libDir.isDirectory)
            val jar = libDir.listFiles()?.firstOrNull {
                it.name.startsWith(name) && it.extension == "jar"
            } ?: fail(failMessage)
            return jar.canonicalPath
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
            val pluginJar = findJar(PLUGIN_JAR_DIR, PLUGIN_JAR_NAME, ":kotlin-atomicfu-compiler-plugin:jar")
            val libraries = getLibrariesPaths()
            val librariesPath = libraries.map { it.canonicalPath }.joinToString(File.pathSeparator)
            classpath += "${File.pathSeparator}$librariesPath"
            pluginClasspaths = arrayOf(pluginJar)
        }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true, isFirEnabled = true)
}
