/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import java.io.FilenameFilter

class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val jar = findJvmLib()
        configuration.addJvmClasspathRoot(jar)
    }
}

private const val ANNOTATIONS_JAR_DIR = "plugins/formal-verification/formver.annotations/build/libs/"
private val JVM_ANNOTATIONS_JAR_FILTER = createFilter("kotlin-formver-compiler-plugin.annotations-jvm", ".jar")

private fun findJvmLib(): File {
    return findLib("jvm", ".jar", JVM_ANNOTATIONS_JAR_FILTER)
}

@Suppress("warnings") // TODO
private fun findLib(platform: String, extension: String, filter: FilenameFilter): File {
    return findLibFromProperty(platform, extension)
        ?: findLibByPath(filter)
        ?: error("Lib with annotations does not exist. Please run :kotlin-formver-compiler-plugin.annotations:build or specify formverPluginAnnotations.path system property")
}

private fun createFilter(pattern: String, extension: String): FilenameFilter {
    return FilenameFilter { _, name -> name.startsWith(pattern) && name.endsWith(extension) }
}

private fun findLibByPath(filter: FilenameFilter): File? {
    val libDir = File(ANNOTATIONS_JAR_DIR)
    if (!libDir.exists() || !libDir.isDirectory) return null
    return libDir.listFiles(filter)?.firstOrNull()
}

/*
 * Possible properties:
 *   - formverPluginAnnotations.jvm.path
 */
private fun findLibFromProperty(platform: String, extension: String): File? {
    val formverPluginAnnotationsPath = System.getProperty("formverPluginAnnotations.${platform}.path") ?: return null
    return File(formverPluginAnnotationsPath).takeIf {
        it.isFile &&
                it.name.startsWith("kotlin-formver-compiler-plugin.annotations") &&
                it.name.endsWith(extension)
    }
}
