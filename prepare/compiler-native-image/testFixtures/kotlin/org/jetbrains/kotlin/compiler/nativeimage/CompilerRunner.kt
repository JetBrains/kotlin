/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import java.io.File

data class CompilerInvocationResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val output: String get() = "$stdout\n$stderr"
}

abstract class CompilerRunner(javaHome: String) {
    protected abstract val executable: String
    protected open val jvmArgs: List<String> get() = DEFAULT_JVM_ARGS

    private val environment: Map<String, String> = mapOf(
        "JAVA_HOME" to javaHome,
    )

    private val cmd: List<String> get() = listOf(executable) + jvmArgs

    fun run(
        workingDir: File,
        arguments: List<String>,
        classpath: List<File> = emptyList(),
    ): CompilerInvocationResult {
        val stdout = File(workingDir, "compiler.out")
        val stderr = File(workingDir, "compiler.err")
        val process = ProcessBuilder(cmd + classpath.asClasspath() + arguments)
            .directory(workingDir)
            .redirectOutput(stdout)
            .redirectError(stderr)
            .also {
                val processEnv = it.environment()
                for ((key, value) in environment) processEnv.putIfAbsent(key, value)
            }
            .start()
        return CompilerInvocationResult(process.waitFor(), stdout.readText(), stderr.readText())
    }

    companion object {
        val DEFAULT_JVM_ARGS = listOf("-Dkotlinc.test.allow.testonly.language.features=true")
    }
}

internal fun List<File>.asClasspath(): List<String> =
    if (isEmpty()) emptyList() else listOf("-cp", joinToString(File.pathSeparator) { it.absolutePath })
