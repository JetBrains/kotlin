/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.AbstractIncrementalK2JvmCompilerRunnerTest
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.fail
import java.io.File

abstract class AbstractParcelizeIncrementalTest : AbstractIncrementalK2JvmCompilerRunnerTest() {
    companion object {
        private const val PLUGIN_JAR_DIR = "build/libs/"
        private const val PLUGIN_JAR_NAME = "parcelize-compiler"
        private const val PLUGIN_JAR_TASK = ":plugins:parcelize:parcelize-compiler:jar"

        private fun findJar(): String {
            val libDir = File(PLUGIN_JAR_DIR)
            kotlin.test.assertTrue(libDir.exists() && libDir.isDirectory)
            val jar = libDir.listFiles()?.firstOrNull {
                it.name.startsWith(PLUGIN_JAR_NAME) && it.extension == "jar" &&
                        !it.name.contains("sources") &&
                        !it.name.contains("javadoc") &&
                        !it.name.contains("test-fixtures")
            } ?: fail { "Jar $PLUGIN_JAR_NAME does not exist. Please run $PLUGIN_JAR_TASK" }
            return jar.canonicalPath
        }

        private fun getLibrariesPaths(): List<File> {
            val runtimeLibraries = System.getProperty("parcelizeRuntime.classpath")
                ?.split(File.pathSeparator)
                ?.map { File(it) }
                ?: error("parcelizeRuntime.classpath is not set")
            val androidApiJar = KtTestUtil.findAndroidApiJar()
            return runtimeLibraries + androidApiJar
        }
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            val pluginJar = findJar()
            val libraries = getLibrariesPaths()
            val librariesPath = libraries.joinToString(File.pathSeparator) { it.canonicalPath }
            classpath += "${File.pathSeparator}$librariesPath"
            pluginClasspaths = arrayOf(pluginJar)
        }
}
