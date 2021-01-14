/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compatibility.binary

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.test.V8JsTestChecker
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File

abstract class AbstractJsKlibBinaryCompatibilityTest : AbstractKlibBinaryCompatibilityTest() {

    private fun TestModule.name(version: Int) = if (this.hasVersions) "version$version/${this.name}" else this.name

    private fun List<TestModule>.toLibrariesArg(version: Int): String {
        val fileNames = this.map { it.name(version) }
        val allDependencies = fileNames.map { File(workingDir, it.klib).absolutePath } + STDLIB_DEPENDENCY
        return allDependencies.joinToString(File.pathSeparator)
    }

    private fun TestModule.dependenciesToLibrariesArg(version: Int): String =
        this.dependencies.map { it as? TestModule ?: error("Unexpected dependency kind: $it") }.toLibrariesArg(version)

    private val TestModule.jsPath get() = File(workingDir, "${this.name}.js").absolutePath

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

    val messageCollector = MessageCollector.NONE
    val lookupTracker = LookupTracker.DO_NOTHING
    val expectActualTracker = ExpectActualTracker.DoNothing
    val services = Services.Builder().apply {
        register(LookupTracker::class.java, lookupTracker)
        register(ExpectActualTracker::class.java, expectActualTracker)
        register(CompilationCanceledStatus::class.java,
                 object : CompilationCanceledStatus {
                     override fun checkCanceled() {}
                 }
        )
    }.build()

    override fun produceKlib(module: TestModule, version: Int) {
        val args = K2JSCompilerArguments().apply {
            freeArgs = createFiles(module.versionFiles(version))
            libraries = module.dependenciesToLibrariesArg(version = 1)
            outputFile = File(workingDir, "${module.name(version)}.$KLIB_FILE_EXTENSION").absolutePath
            irProduceKlibFile = true
            irOnly = true
            irModuleName = module.name
            repositries = "$workingDir${File.pathSeparator}$workingDir/version1"
        }
        K2JSCompiler().exec(messageCollector, services, args)
    }

    override fun produceProgram(module: TestModule) {
        assert(!module.hasVersions)
        val args = K2JSCompilerArguments().apply {
            freeArgs = createFiles(module.files) + runnerFunctionFile()
            libraries = module.dependenciesToLibrariesArg(version = 2)
            outputFile = File(workingDir, module.name.js).absolutePath
            irProduceJs = true
            irOnly = true
            irModuleName = module.name
            repositries = "$workingDir${File.pathSeparator}$workingDir/version2"
        }
        K2JSCompiler().exec(messageCollector, services, args)
    }

    override fun runProgram(module: TestModule, expectedResult: String) {
        testChecker.check(listOf(module.jsPath), module.name, null, RUNNER_FUNCTION, expectedResult, false)
    }

    // TODO: ask js folks what to use here.
    protected open val testChecker get() = V8JsTestChecker

    companion object {
        private val String.klib: String get() = "$this.$KLIB_FILE_EXTENSION"
        private val String.js: String get() = "$this.js"

        private const val STDLIB_DEPENDENCY = "build/js-ir-runtime/full-runtime.klib"

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
