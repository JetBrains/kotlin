/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.errortolerance

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URLClassLoader
import kotlin.io.path.createTempDirectory

/**
 * Compiles a test file with errors via the CLI compiler with the error tolerance plugin, checks the reported tolerated errors
 * against the `.txt` file next to the test file, and runs its `box()` function which is expected to return "OK".
 */
abstract class AbstractErrorToleranceBoxTest {
    private lateinit var workingDir: File

    @BeforeEach
    fun setUp() {
        workingDir = createTempDirectory(javaClass.simpleName).toFile()
    }

    @AfterEach
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    fun runTest(path: String) {
        val testFile = ForTestCompileRuntime.transformTestDataPath(path)
        val outputDir = workingDir.resolve("out")

        val messageCollector = MessageCollectorImpl()
        val compiler = K2JVMCompiler()
        val arguments = compiler.createArguments().apply {
            noStdlib = true
            noReflect = true
            classpath = kotlinJvmStdlib.canonicalPath
            pluginClasspaths = arrayOf(pluginJar.canonicalPath)
            destination = outputDir.canonicalPath
            renderInternalDiagnosticNames = true
            freeArgs = listOf(testFile.canonicalPath)
        }
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, arguments)

        val messages = messageCollector.messages
            .filter { it.severity.isError || it.severity.isWarning }
            .joinToString(separator = "\n", postfix = "\n") { message ->
                val location = message.location?.let { "${it.line}:${it.column}: " }.orEmpty()
                "${message.severity.presentableName}: $location${message.message}"
            }
        JUnit5Assertions.assertEqualsToFile(File(testFile.path.removeSuffix(".kt") + ".txt"), messages)
        assertEquals(ExitCode.OK, exitCode, messages)

        val facadeClassName = PackagePartClassUtils.getFilePartShortName(testFile.name)
        URLClassLoader(arrayOf(outputDir.toURI().toURL(), kotlinJvmStdlib.toURI().toURL()), null).use { classLoader ->
            // Not via reflection: `Class.getMethod` resolves signatures of all methods, and erroneous declarations might mention
            // non-existent classes in their signatures
            val box = MethodHandles.publicLookup()
                .findStatic(classLoader.loadClass(facadeClassName), "box", MethodType.methodType(String::class.java))
            val result = box.invoke() as String
            assertEquals("OK", result)
        }
    }

    companion object {
        private val pluginJar: File = ForTestCompileRuntime.getFileFromProperty("errorTolerance.jar.path")
        private val kotlinJvmStdlib: File = ForTestCompileRuntime.runtimeJarForTests()
    }
}
