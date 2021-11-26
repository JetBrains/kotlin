/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileProgramTask
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import java.io.File
import java.nio.file.Path
import java.nio.file.Files

/**
 * Gradle task that wraps FileCheck LLVM utility.
 */
open class FileCheckTest : DefaultTask() {

    private val target = project.testTarget
    private val platform = project.platformManager.platform(target)
    private val configurables = platform.configurables

    private val llvmBin = "${configurables.absoluteLlvmHome}/bin"

    private val fileCheck = "$llvmBin/FileCheck"

    /**
     * File annotated with FileCheck directives.
     */
    @InputFile
    lateinit var annotatedSource: File

    /**
     * LLVM IR that should match [annotatedSource].
     */
    @get:Internal
    lateinit var llvmIr: File

    /**
     * Optional cinterop task dependency.
     */
    @Optional
    @Input
    var interop: String? = null

    @TaskAction
    fun run() {
        runFileCheck(annotatedSource.toPath(), llvmIr.toPath())
    }

    /**
     * What prefix should checked for pattern instead of default CHECK?
     */
    @Input
    @Optional
    var checkPrefix: String? = null

    /**
     * Should we generate framework instead of an executable?
     * This option is useful for, well, checking framework-specific code.
     */
    @Input
    var generateFramework: Boolean = false

    /**
     * Check that [inputFile] matches [annotatedFile] with FileCheck.
     */
    private fun runFileCheck(annotatedFile: Path, inputFile: Path): ProcessOutput {
        val args = mutableListOf(annotatedFile.toAbsolutePath().toString(), "--input-file", inputFile.toAbsolutePath().toString())
        checkPrefix?.let {
            args.addAll(listOf("--check-prefix", it))
        }
        return runProcess(localExecutor(project), fileCheck, *args.toTypedArray())
                .ensureSuccessful(fileCheck, *args.toTypedArray())
    }

    private fun ProcessOutput.ensureSuccessful(vararg command: String): ProcessOutput {
        if (exitCode != 0) {
            println("""
                    ${command.joinToString(separator = " ")} failed.
                    exitCode: $exitCode
                    stdout:
                    $stdOut
                    stderr:
                    $stdErr
                """.trimIndent())
            throw TestFailedException("${command.joinToString(separator = " ")} failed")
        }
        return this
    }
}