/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

/**
 * Generates cross-compilation wrapper scripts (clang/clang++) for all targets
 * defined in konan.properties.
 *
 * These scripts wrap run_konan to invoke clang with the appropriate sysroot
 * for each target, enabling cross-compilation of C libraries that will be
 * linked with Kotlin/Native.
 */
abstract class GenerateCrossCompilationWrappers : DefaultTask() {

    @get:InputFile
    abstract val konanProperties: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val props = Properties().apply {
            konanProperties.get().asFile.inputStream().use { load(it) }
        }

        val outputDir = outputDirectory.get().asFile
        outputDir.mkdirs()

        // Find all targetTriple.* entries
        props.stringPropertyNames()
            .filter { it.startsWith("targetTriple.") }
            .forEach { key ->
                val target = key.removePrefix("targetTriple.")
                val triple = props.getProperty(key).trim()

                generateBashScript(outputDir, triple, "clang", target)
                generateBashScript(outputDir, triple, "clang++", target)
                generateBatchScript(outputDir, triple, "clang", target)
                generateBatchScript(outputDir, triple, "clang++", target)
            }
    }

    private fun generateBashScript(outputDir: File, triple: String, compiler: String, target: String) {
        val file = File(outputDir, "$triple-$compiler")
        file.writeText(bashTemplate(compiler, target))
        file.setExecutable(true)
    }

    private fun generateBatchScript(outputDir: File, triple: String, compiler: String, target: String) {
        val file = File(outputDir, "$triple-$compiler.bat")
        file.writeText(batchTemplate(compiler, target))
    }

    private fun bashTemplate(compiler: String, target: String) = """
        |#!/usr/bin/env bash
        |
        |# Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
        |# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
        |
        |# Wrapper for running $compiler with Kotlin/Native's $target sysroot.
        |
        |DIR="${'$'}{BASH_SOURCE[0]%/*}"
        |: ${'$'}{DIR:="."}
        |
        |"${'$'}{DIR}"/../run_konan clang $compiler $target "${'$'}@"
    """.trimMargin() + "\n"

    private fun batchTemplate(compiler: String, target: String) = """
        |@echo off
        |
        |rem Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
        |rem Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
        |
        |rem Wrapper for running $compiler with Kotlin/Native's $target sysroot.
        |
        |call %~dps0..\run_konan.bat clang $compiler $target %*
    """.trimMargin() + "\n"
}
