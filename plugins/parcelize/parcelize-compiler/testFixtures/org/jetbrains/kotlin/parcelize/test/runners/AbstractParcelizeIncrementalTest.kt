/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.AbstractIncrementalK2JvmCompilerRunnerTest
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class AbstractParcelizeIncrementalTest : AbstractIncrementalK2JvmCompilerRunnerTest() {
    companion object {
        private fun getLibrariesPaths(): List<File> {
            val runtimeLibraries = ForTestCompileRuntime.parcelizeRuntimeForTests()
            val androidApiJar = KtTestUtil.findAndroidApiJar()
            return runtimeLibraries + androidApiJar
        }
    }

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            val pluginJar = System.getProperty("parcelizePlugin.jar")
            val libraries = getLibrariesPaths()
            val librariesPath = libraries.joinToString(File.pathSeparator) { it.canonicalPath }
            classpath += "${File.pathSeparator}$librariesPath"
            pluginClasspaths = arrayOf(pluginJar)
        }
}
