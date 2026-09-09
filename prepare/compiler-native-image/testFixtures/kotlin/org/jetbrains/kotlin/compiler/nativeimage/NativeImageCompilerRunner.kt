/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import java.io.File

data class CompilerInvocationResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

class NativeImageCompilerRunner(private val javaHome: String) {
    private val nativeImageDist: File by lazy { ForTestCompileRuntime.kotlinNativeImageDistForTests() }

    private val executable: File by lazy {
        val launcher = when {
            System.getProperty("os.name").startsWith("windows", ignoreCase = true) -> "kotlinc-native-image.bat"
            else -> "kotlinc-native-image.sh"
        }
        nativeImageDist.resolve("bin").resolve(launcher)
    }

    fun run(
        workingDir: File,
        arguments: List<String>,
        classpath: List<File>,
        jvmArgs: List<String> = emptyList(),
    ): CompilerInvocationResult {
        val cmd = buildList {
            add(executable.absolutePath)
            addAll(jvmArgs)
            if (classpath.isNotEmpty()) {
                add("-cp")
                add(classpath.joinToString(File.pathSeparator))
            }
            addAll(arguments)
        }
        val process = ProcessBuilder(cmd)
            .directory(workingDir)
            .also { it.environment().putIfAbsent("JAVA_HOME", javaHome) }
            .start()
        val out = process.inputStream.reader().use { it.readText() }
        val err = process.errorStream.reader().use { it.readText() }
        return CompilerInvocationResult(process.waitFor(), out, err)
    }
}
