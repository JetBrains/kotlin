/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.compiler.embeddable

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.test.assertEquals


private val COMPILER_CLASS_FQN = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"

public class CompilerSmokeTest {

    public val _workingDir: TemporaryFolder = TemporaryFolder()

    @Rule
    public fun getWorkingDir(): TemporaryFolder = _workingDir

    private val javaExecutable = File( File(System.getProperty("java.home"), "bin"), "java")

    private val compilerClasspath: List<File> by lazy {
        filesFromProp("compilerClasspath", "kotlin-compiler-embeddable.jar")
    }

    private val compilationClasspath: List<File> by lazy {
        filesFromProp("compilationClasspath", "kotlin-stdlib.jar", "kotlin-script-runtime.jar")
    }

    private fun filesFromProp(propName: String, vararg defaultPaths: String): List<File> =
        (System.getProperty(propName)?.split(File.pathSeparator) ?: defaultPaths.asList()).map {
            File(it).takeIf(File::exists)
                ?: throw FileNotFoundException("cannot find ($it)")
        }

    @Test
    fun testSmoke() {
        val (out, code) = runCompiler(File("testData/projects/smoke/Smoke.kt").absolutePath)
        assertEquals(0, code, "compilation failed:\n" + out)
    }


    private fun createProcess(cmd: List<String>, projectDir: File): Process {
        val builder = ProcessBuilder(cmd)
        builder.directory(projectDir)
        builder.redirectErrorStream(true)
        return builder.start()
    }

    private fun runCompiler(vararg arguments: String): Pair<String, Int> {
        val cmd = listOf(
            javaExecutable.absolutePath,
            "-Djava.awt.headless=true",
            "-cp",
            compilerClasspath.joinToString(File.pathSeparator),
            COMPILER_CLASS_FQN,
            "-cp",
            compilationClasspath.joinToString(File.pathSeparator)
        ) + arguments
        val proc = createProcess(cmd, _workingDir.root)
        return readOutput(proc)
    }

    private fun readOutput(process: Process): Pair<String, Int> {
        fun InputStream.readFully(): String {
            val text = reader().readText()
            close()
            return text
        }

        val stdout = process.inputStream!!.readFully()
        System.out.println(stdout)
        val stderr = process.errorStream!!.readFully()
        System.err.println(stderr)

        val result = process.waitFor()
        return stdout to result
    }
}
