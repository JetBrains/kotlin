/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import java.io.File

/**
 * Test plugins with `-Xcompiler-plugin` flag
 */
abstract class AbstractNativeImagePluginTest : AbstractNativeImageCodegenTest() {
    protected val pluginsRuntimeClasspath: List<File> by lazy { ForTestCompileRuntime.kotlinNativeImagePluginsRuntimeForTests() }

    private val kotlinHome: File by lazy { ForTestCompileRuntime.distKotlincForTests() }

    override fun buildCompilerArgs(
        boxFile: File,
        outDir: File,
        directives: RegisteredDirectives,
        withFullJdk: Boolean,
    ): List<String> = buildList {
        addAll(super.buildCompilerArgs(boxFile, outDir, directives, withFullJdk))
        if (directives.expectedPluginOrder().isNotEmpty()) add("-verbose")
        for ([_, jarName, options] in directives.pluginSpecs()) {
            val jar = kotlinHome.resolve("lib").resolve(jarName).absolutePath
            add(
                when {
                    options.isEmpty() -> "-Xcompiler-plugin=$jar"
                    else -> "-Xcompiler-plugin=$jar=${options.joinToString(",")}"
                }
            )
        }
        for (constraint in directives.pluginOrderConstraints()) {
            add("-Xcompiler-plugin-order=$constraint")
        }
    }

    override fun buildClasspath(withReflect: Boolean, withFullJdk: Boolean): List<File> =
        super.buildClasspath(withReflect, withFullJdk) + pluginsRuntimeClasspath

    override fun runtimeClasspath(withReflect: Boolean): List<File> = super.runtimeClasspath(withReflect) + pluginsRuntimeClasspath

}
