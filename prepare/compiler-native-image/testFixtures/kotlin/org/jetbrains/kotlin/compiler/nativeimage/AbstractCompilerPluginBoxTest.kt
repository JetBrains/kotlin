/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import java.io.File

abstract class AbstractNativeImagePluginBoxTest : AbstractCompilerPluginBoxTest(NativeImageCompilerRunner(), PluginLoadingMode.MODERN)

abstract class AbstractNativeImageLegacyPluginBoxTest : AbstractCompilerPluginBoxTest(NativeImageCompilerRunner(), PluginLoadingMode.LEGACY)

abstract class AbstractNativeImageDynamicPluginBoxTest : AbstractNativeImagePluginBoxTest()

abstract class AbstractNativeImageDynamicLegacyPluginBoxTest : AbstractNativeImageLegacyPluginBoxTest()

abstract class AbstractReachabilityMetadataPluginBoxTest :
    AbstractCompilerPluginBoxTest(TracingAgentCompilerRunner(), PluginLoadingMode.MODERN)

abstract class AbstractReachabilityMetadataLegacyPluginBoxTest :
    AbstractCompilerPluginBoxTest(TracingAgentCompilerRunner(), PluginLoadingMode.LEGACY)

abstract class AbstractCompilerPluginBoxTest(
    runner: CompilerRunner,
    private val pluginLoadingMode: PluginLoadingMode,
) : AbstractCompilerBoxTest(runner) {

    private val kotlinHome: File by lazy { ForTestCompileRuntime.distKotlincForTests() }
    private val pluginsBuildClasspath: List<File> by lazy { ForTestCompileRuntime.kotlinNativeImagePluginsClasspathForTests() }
    private val pluginsRuntimeClasspath: List<File> by lazy { ForTestCompileRuntime.kotlinNativeImagePluginsRuntimeForTests() }

    override fun buildCompilerArgs(
        testFile: File,
        outDir: File,
        directives: RegisteredDirectives,
        withFullJdk: Boolean,
    ): List<String> = super.buildCompilerArgs(testFile, outDir, directives, withFullJdk) +
            directives.pluginSpecs().flatMap { pluginLoadingMode.compilerArgs(it, resolveJarPath(it.jarName)) }

    private fun resolveJarPath(jarName: String): String {
        val bundledJar = kotlinHome.resolve("lib").resolve(jarName)
        if (bundledJar.exists()) return bundledJar.absolutePath
        val jarNameRegex = jarName.toRegex()
        return pluginsBuildClasspath.firstOrNull { it.name.contains(jarNameRegex) }?.absolutePath
            ?: error("Plugin jar $jarName not found")
    }

    override fun buildClasspath(withReflect: Boolean, withFullJdk: Boolean): List<File> =
        super.buildClasspath(withReflect, withFullJdk) + pluginsBuildClasspath + pluginsRuntimeClasspath

    override fun runtimeClasspath(withReflect: Boolean): List<File> =
        super.runtimeClasspath(withReflect) + pluginsRuntimeClasspath
}
