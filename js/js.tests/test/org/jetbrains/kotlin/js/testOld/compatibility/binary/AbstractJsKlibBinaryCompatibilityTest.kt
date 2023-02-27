/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.compatibility.binary

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compatibility.binary.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.test.KotlinBaseTest
import java.io.File

abstract class AbstractJsKlibBinaryCompatibilityTest : AbstractKlibBinaryCompatibilityTest() {

    override fun createEnvironment() =
        KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    private fun TestModule.name(version: Int) = if (this.hasVersions) "version$version/${this.name}" else this.name

    private fun List<TestModule>.toLibrariesArg(version: Int): String {
        val fileNames = this.map { it.name(version) }
        val allDependencies = fileNames.map { File(workingDir, it.klib).absolutePath } + STDLIB_DEPENDENCY
        return allDependencies.joinToString(File.pathSeparator)
    }

    private fun TestModule.dependenciesToLibrariesArg(version: Int): String =
        this.dependencies
            .flatMap { it.transitiveDependencies() }
            .map { it as? TestModule ?: error("Unexpected dependency kind: $it") }
            .toLibrariesArg(version)

    private fun KotlinBaseTest.TestModule.transitiveDependencies(): Set<KotlinBaseTest.TestModule> {
        val uniqueDependencies = mutableSetOf(this)
        dependencies.forEach { testModule ->
            if (testModule !in uniqueDependencies) {
                val transitiveDependencies = testModule.transitiveDependencies()
                uniqueDependencies.addAll(transitiveDependencies)
            }
        }

        return uniqueDependencies
    }

    private val jsOutDir get() = workingDir.resolve("out")

    private val TestModule.jsPath get() = File(jsOutDir, "${this.name}.js").absolutePath

    private fun createFiles(files: List<TestFile>): List<String> =
        files.map {
            val file = File(workingDir, it.name)
            file.writeText(it.content)
            file.absolutePath
        }

    private fun runnerFunctionFile(): String {
        val file = File(workingDir, RUNNER_FUNCTION_FILE)
        file.writeText(runnerFileText)
        return file.absolutePath
    }

    override fun produceKlib(module: TestModule, version: Int) {
        val args = K2JSCompilerArguments().apply {
            freeArgs = createFiles(module.versionFiles(version))
            libraries = module.dependenciesToLibrariesArg(version = version)
            outputDir = workingDir.normalize().absolutePath
            moduleName = module.name(version)
            irProduceKlibFile = true
            irOnly = true
            irModuleName = module.name
        }
        K2JSCompiler().exec(TestMessageCollector(), Services.EMPTY, args)
    }

    override fun produceProgram(module: TestModule) {
        assert(!module.hasVersions)
        val args = K2JSCompilerArguments().apply {
            freeArgs = createFiles(module.files) + runnerFunctionFile()
            libraries = module.dependenciesToLibrariesArg(version = 2)
            outputDir = jsOutDir.normalize().absolutePath
            moduleName = module.name
            irProduceJs = true
            irOnly = true
            irModuleName = module.name
        }
        K2JSCompiler().exec(TestMessageCollector(), Services.EMPTY, args)
    }

    override fun runProgram(module: TestModule, expectedResult: String) {
        testChecker.check(listOf(module.jsPath), module.name, null, RUNNER_FUNCTION, expectedResult, false)
    }

    // TODO: ask js folks what to use here.
    protected open val testChecker get() = V8JsTestChecker

    companion object {
        private val String.klib: String get() = "$this.$KLIB_FILE_EXTENSION"
        private val String.js: String get() = "$this.js"

        private val STDLIB_DEPENDENCY = System.getProperty("kotlin.js.full.stdlib.path")

        // A @JsExport wrapper for box().
        // Otherwise box() is not available in js.
        private const val RUNNER_FUNCTION = "__js_exported_wrapper_function"
        private const val RUNNER_FUNCTION_FILE = "js_exported_wrapper_function.kt"
        private val runnerFileText = """
            @JsExport
            fun $RUNNER_FUNCTION() = $TEST_FUNCTION()
        """
    }
}

abstract class AbstractJsKlibBinaryCompatibilityErrorTest : AbstractJsKlibBinaryCompatibilityTest()

private class TestMessageCollector : MessageCollector {
    override fun clear() {}
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity.isError()) error(message)
    }
    override fun hasErrors(): Boolean = error("Unsupported operation")
}
