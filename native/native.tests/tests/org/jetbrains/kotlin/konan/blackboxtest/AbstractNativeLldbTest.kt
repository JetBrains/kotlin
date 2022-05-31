/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.ProcessLevelProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.callCompiler
import org.jetbrains.kotlin.konan.blackboxtest.support.lldb.LldbSessionSpecification
import org.jetbrains.kotlin.konan.blackboxtest.support.lldb.ToolDriver
import org.jetbrains.kotlin.konan.blackboxtest.support.lldb.lldbCheckLineNumbers
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeClassLoader
import java.io.File
import java.net.URLClassLoader

import java.nio.file.Files
import java.nio.file.Paths

abstract class AbstractNativeLldbTest {
    fun runTest(testFileName: String) {
        val testFilePath = Paths.get(testFileName)
        var content = mutableListOf<String>()
        var session: LldbSessionSpecification? = null
        var checkSteps: String? = null
        val nameToContent = mutableMapOf("main.kt" to content)
        var compilerArgs = arrayOf("-g")
        val tmpdir = Files.createTempDirectory("debugger_test")
        tmpdir.toFile().deleteOnExit()

        Files.lines(testFilePath).forEach { line ->
            when {
                line.startsWith("// FILE: ") -> {
                    val fileName = line.split(' ', limit = 3).last().trim()
                    content = nameToContent.getOrPut(fileName, ::mutableListOf)
                }
                line.startsWith("// LLDB_SESSION: ") -> {
                    val sessionFileName = line.split(' ', limit = 3).last().trim()
                    val spec = Files.readString(testFilePath.resolveSibling(sessionFileName))
                    session = LldbSessionSpecification.parse(spec)
                }
                line.startsWith("// LLDB_CHECK_STEPS: ") -> {
                    checkSteps = line.split(' ', limit = 3).last().trim()
                }
                line.startsWith("// TEST_RUNNER: LLDB") -> {}
                line.startsWith("// FREE_COMPILER_ARGS: ") -> {
                    compilerArgs = line.split(' ').drop(2).toTypedArray()
                }
                else -> content.add(line)
            }
        }
        val source = nameToContent
            .filter { (_, content) -> content.isNotEmpty() }
            .map { (filename, content) ->
                val path = tmpdir.resolve(filename)
                Files.write(path, content.joinToString(System.lineSeparator()).toByteArray())
                path
        }.toTypedArray()

        val output = tmpdir.resolve("program.kexe")

        val driver = ToolDriver()
        driver.compile(output, source, *compilerArgs)

//        val compileRes = callCompiler(compilerArgs + arrayOf("-o", output.toString()) + source.map { it.toString() }, KotlinNativeClassLoader(
//            lazy {
//                val nativeClassPath = ProcessLevelProperty.COMPILER_CLASSPATH.readValue()
//                    .split(':', ';')
//                    .map { File(it).toURI().toURL() }
//                    .toTypedArray()
//                URLClassLoader(nativeClassPath, /* no parent class loader */ null).apply { setDefaultAssertionStatus(true) }
//            }).classLoader)

        session?.let { sess ->
            val result = driver.runLldb(output, sess.commands)
            sess.match(result)
        }

        checkSteps?.let { cmd ->
            val (breakPoint, steps) = cmd.split(' ')
            lldbCheckLineNumbers(nameToContent, breakPoint, steps.toInt())
        }
    }
}