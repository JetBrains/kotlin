/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.native.compiler.embeddable

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.System.err
import kotlin.test.assertEquals

private val COMPILER_CLASS_FQN = "org.jetbrains.kotlin.cli.bc.K2Native"

class CompilerSmokeTest {

    private val _workingDir: TemporaryFolder = TemporaryFolder()

    @Rule
    fun getWorkingDir(): TemporaryFolder = _workingDir

    private val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")

    companion object {
        val compilerClasspath: List<File> by lazy {
            filesFromProp("compilerClasspath", "kotlin-native-compiler-embeddable.jar")
        }

        private fun filesFromProp(propName: String, vararg defaultPaths: String): List<File> =
                (System.getProperty(propName)?.split(File.pathSeparator) ?: defaultPaths.asList()).map {
                    File(it).takeIf(File::exists)
                            ?: throw FileNotFoundException("cannot find ($it)")
                }
    }

    @Test
    fun testSmoke() {
        val (out, code) = runCompiler("-e", "smoke.main", File("testData/projects/smoke/Smoke.kt").absolutePath)
        assertEquals(0, code, "compilation failed: $out\n")
    }

    private fun createProcess(vararg cmd: String, projectDir: File): Process {
        val builder = ProcessBuilder(*cmd)
        builder.directory(projectDir)
        builder.redirectErrorStream(true)
        return builder.start()
    }

    private fun runCompiler(vararg arguments: String): Pair<String, Int> {
        val cmd = listOf(
                javaExecutable.absolutePath,
                "-Djava.awt.headless=true",
                "-Dkotlin.native.home=${System.getProperty("kotlin.native.home")}",
                "-cp",
                compilerClasspath.joinToString(File.pathSeparator),
                COMPILER_CLASS_FQN
        ) + arguments
        val proc = createProcess(*cmd.toTypedArray(), projectDir = _workingDir.root)
        return readOutput(proc)
    }

    private fun readOutput(process: Process): Pair<String, Int> {
        fun InputStream.readFully(): String {
            val text = reader().readText()
            close()
            return text
        }

        val stdout = process.inputStream!!.readFully()
        println(stdout)
        val stderr = process.errorStream!!.readFully()
        err.println(stderr)

        val result = process.waitFor()
        return stdout to result
    }
}
