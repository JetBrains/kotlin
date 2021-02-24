/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

abstract class BaseJvmAbiTest : TestCase() {
    private lateinit var workingDir: File

    @OptIn(ExperimentalPathApi::class)
    override fun setUp() {
        super.setUp()
        workingDir = createTempDirectory(javaClass.simpleName).toFile().apply { deleteOnExit() }
    }

    override fun tearDown() {
        workingDir.deleteRecursively()
        super.tearDown()
    }

    private val abiPluginJar = File("dist/kotlinc/lib/jvm-abi-gen.jar")
    private fun abiOption(option: String, value: String): String =
        "plugin:${JvmAbiCommandLineProcessor.COMPILER_PLUGIN_ID}:$option=$value"

    inner class Compilation(
        private val projectDir: File,
        val name: String?,
        val dependencies: Collection<Compilation> = emptyList()
    ) {
        val srcDir: File
            get() = if (name == null) projectDir else projectDir.resolve(name)

        val destinationDir: File
            get() = if (name == null) workingDir.resolve("out") else workingDir.resolve("$name/out")

        val abiDir: File
            get() = if (name == null) workingDir.resolve("abi") else workingDir.resolve("$name/abi")

        override fun toString(): String =
            "compilation '$name'"
    }

    fun make(compilation: Compilation) {
        check(abiPluginJar.exists()) { "Plugin jar '$abiPluginJar' does not exist" }
        check(compilation.srcDir.exists()) { "Source dir '${compilation.srcDir}' does not exist" }

        val abiDependencies = compilation.dependencies.map { dep ->
            check(dep.abiDir.exists()) { "Dependency '${dep.name}' of '${compilation.name}' was not built" }
            dep.abiDir
        }

        val messageCollector = LocationReportingTestMessageCollector()
        val compiler = K2JVMCompiler()
        val args = compiler.createArguments().apply {
            freeArgs = listOf(compilation.srcDir.canonicalPath)
            classpath = (abiDependencies + kotlinJvmStdlib).joinToString(File.pathSeparator) { it.canonicalPath }
            pluginClasspaths = arrayOf(abiPluginJar.canonicalPath)
            pluginOptions = arrayOf(abiOption("outputDir", compilation.abiDir.canonicalPath))
            destination = compilation.destinationDir.canonicalPath
        }
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)
        if (exitCode != ExitCode.OK || messageCollector.errors.isNotEmpty()) {
            val errorLines = listOf("Could not compile $compilation", "Exit code: $exitCode", "Errors:") + messageCollector.errors
            error(errorLines.joinToString("\n"))
        }
    }

    protected val kotlinJvmStdlib = File("dist/kotlinc/lib/kotlin-stdlib.jar").also {
        check(it.exists()) { "Stdlib file '$it' does not exist" }
    }
}