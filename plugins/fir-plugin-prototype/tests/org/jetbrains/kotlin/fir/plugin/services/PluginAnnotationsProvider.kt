/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File
import java.io.FilenameFilter

class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {

    private val firPluginAnnotationsJar: File by lazy {
        val firPluginAnnotationsPath = System.getProperty("firPluginAnnotations.path") ?: error("firPluginAnnotations.path system property is not set")
        val firPluginAnnotationsFile = File(firPluginAnnotationsPath)
        if (!firPluginAnnotationsFile.isFile &&
            firPluginAnnotationsFile.name.startsWith("plugin-annotations") &&
            firPluginAnnotationsFile.name.endsWith(".jar")
        ) {
            error("Can't find fir plugin annotations jar file in firPluginAnnotations.path system property")
        }
        firPluginAnnotationsFile
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoot(firPluginAnnotationsJar)
    }
}
