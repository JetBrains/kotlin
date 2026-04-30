/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.abi.tools.tests.utils

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.plus
import kotlin.io.path.*
import kotlin.streams.toList


class BuildSession : AutoCloseable {
    val kotlinToolchain = loadToolchain()
    val session = kotlinToolchain.createBuildSession()
    val inProcessStrategy = kotlinToolchain.createInProcessExecutionPolicy()

    fun compileKotlinJvm(
        sources: List<Path>,
        outputDir: Path,
        compileClasspath: List<Path> = emptyList(),
        plugins: List<CompilerPlugin> = emptyList(),
        moduleName: String? = null,
    ) {
        val compilationOperation = kotlinToolchain.jvm.jvmCompilationOperation(
            sources,
            outputDir
        ) {
            compilerArguments[NO_REFLECT] = true
            compilerArguments[NO_STDLIB] = true
            compilerArguments[CLASSPATH] = compileClasspath + listOf(currentKotlinStdlibLocation)
            if (moduleName != null) {
                compilerArguments[MODULE_NAME] = moduleName
            }
            if (plugins.isNotEmpty()) {
                compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = plugins
            }
        }

        val result = session.executeOperation(compilationOperation, inProcessStrategy)
        if (result != CompilationResult.COMPILATION_SUCCESS) {
            throw IllegalStateException("Compilation failed, state $result")
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun close() {
        session.close()
    }
}

fun Path.collectKtFiles(): List<Path> = Files.walk(this).filter { it.isSourceFile }.toList()

val Path.isSourceFile: Boolean
    get() = isRegularFile() && (name.endsWith(".kt"))

