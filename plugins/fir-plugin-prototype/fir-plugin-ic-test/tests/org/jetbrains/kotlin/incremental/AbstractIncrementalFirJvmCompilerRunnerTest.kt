/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.junit.jupiter.api.fail
import java.io.File
import java.io.FilenameFilter

abstract class AbstractIncrementalFirJvmWithPluginCompilerRunnerTest : AbstractIncrementalFirJvmCompilerRunnerTest() {
    companion object {
        private const val ANNOTATIONS_JAR_DIR = "plugins/fir-plugin-prototype/plugin-annotations/build/libs/"
        private const val ANNOTATIONS_JAR_NAME = "plugin-annotations"

        private const val PLUGIN_JAR_DIR = "plugins/fir-plugin-prototype/build/libs/"
        private const val PLUGIN_JAR_NAME = "fir-plugin-prototype"

        private fun findJar(dir: String, name: String, taskName: String): String {
            val failMessage = { "Jar $name does not exist. Please run $taskName" }
            val libDir = File(dir)
            kotlin.test.assertTrue(libDir.exists() && libDir.isDirectory)
            val jar = libDir.listFiles()?.firstOrNull {
                it.name.startsWith(name) && it.extension == "jar"
            } ?: fail(failMessage)
            return jar.canonicalPath
        }
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            val annotationsJar = findJar(ANNOTATIONS_JAR_DIR, ANNOTATIONS_JAR_NAME, ":plugins:fir-plugin-prototype:plugin-annotations:jar")
            val pluginJar = findJar(PLUGIN_JAR_DIR, PLUGIN_JAR_NAME, ":plugins:fir-plugin-prototype:jar")

            classpath = classpath?.plus(annotationsJar)
            pluginClasspaths = arrayOf(pluginJar)
        }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true, isFirEnabled = true) // TODO: investigate cases that need isGradleEnabled - the combination looks fragile
}
