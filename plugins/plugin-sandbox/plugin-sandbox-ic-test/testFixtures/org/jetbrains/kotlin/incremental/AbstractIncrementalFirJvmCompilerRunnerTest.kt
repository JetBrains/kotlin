/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.utils.findAnnotationsRuntimeJar
import org.jetbrains.kotlin.incremental.utils.findPluginJar
import java.io.File

abstract class AbstractIncrementalK2JvmWithPluginCompilerRunnerTest : AbstractIncrementalK2JvmCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JVMCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            val annotationsJar = findAnnotationsRuntimeJar()
            val pluginJar = findPluginJar()

            classpath += "${File.pathSeparator}$annotationsJar"
            pluginClasspaths = arrayOf(pluginJar)
        }

    override val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true, isFirEnabled = true) // TODO: investigate cases that need isGradleEnabled - the combination looks fragile
}
