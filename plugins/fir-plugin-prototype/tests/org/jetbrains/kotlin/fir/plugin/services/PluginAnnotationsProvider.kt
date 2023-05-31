/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import java.io.FilenameFilter

class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        // TODO: handle property
        val platform = module.targetPlatform
        when {
            platform.isJvm() -> {
                val jar = findJvmLib()
                configuration.addJvmClasspathRoot(jar)
            }
            platform.isJs() -> {
                val jar = findJsLib()
                val libraries = configuration.getList(JSConfigurationKeys.LIBRARIES)
                configuration.put(JSConfigurationKeys.LIBRARIES, libraries + jar.absolutePath)
            }
        }
    }
}

class PluginRuntimeAnnotationsProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        if (!module.targetPlatform.isJs()) return emptyList()
        val jar = findJsLib()
        return listOf(jar)
    }
}


private const val ANNOTATIONS_JAR_DIR = "plugins/fir-plugin-prototype/plugin-annotations/build/libs/"
private val JVM_ANNOTATIONS_JAR_FILTER = createFilter("plugin-annotations-jvm", ".jar")
private val JS_ANNOTATIONS_KLIB_FILTER = createFilter("plugin-annotations-js", ".klib")

private fun findJvmLib(): File {
    return findLib("jvm", ".jar", JVM_ANNOTATIONS_JAR_FILTER)
}

private fun findJsLib(): File {
    return findLib("js", ".klib", JS_ANNOTATIONS_KLIB_FILTER)
}

@Suppress("warnings") // TODO
private fun findLib(platform: String, extension: String, filter: FilenameFilter): File {
    return findLibFromProperty(platform, extension)
        ?: findLibByPath(filter)
        ?: error("Lib with annotations does not exist. Please run :plugins:fir-plugin-prototype:plugin-annotations:distAnnotations or specify firPluginAnnotations.path system property")
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
 *   - firPluginAnnotations.jvm.path
 *   - firPluginAnnotations.js.path
 */
private fun findLibFromProperty(platform: String, extension: String): File? {
    val firPluginAnnotationsPath = System.getProperty("firPluginAnnotations.${platform}.path") ?: return null
    return File(firPluginAnnotationsPath).takeIf {
        it.isFile &&
                it.name.startsWith("plugin-annotations") &&
                it.name.endsWith(extension)
    }
}
