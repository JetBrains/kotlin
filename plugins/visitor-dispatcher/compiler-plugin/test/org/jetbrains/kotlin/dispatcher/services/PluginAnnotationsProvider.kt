/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import java.io.FilenameFilter

class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private const val ANNOTATIONS_JAR_DIR = "plugins/visitor-dispatcher/plugin-annotations/build/libs"
        private val ANNOTATIONS_JAR_FILTER = FilenameFilter { _, name -> name.startsWith("plugin-annotations") && name.endsWith(".jar") }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val pluginAnnotationsJar = findJarByPath()
            ?: error("Jar with annotations does not exist. Please run :plugins:fir-plugin-prototype:plugin-annotations:jar or specify firPluginAnnotations.path system property")
        configuration.addJvmClasspathRoot(pluginAnnotationsJar)
    }

    private fun findJarByPath(): File? {
        val libDir = File(ANNOTATIONS_JAR_DIR)
        if (!libDir.exists() || !libDir.isDirectory) return null
        return libDir.listFiles(ANNOTATIONS_JAR_FILTER)?.firstOrNull()
    }
}
