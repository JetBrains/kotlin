/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

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
import org.jetbrains.kotlin.js.testOld.klib.AbstractJsKlibEvolutionTest.Companion.klib
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.KotlinBaseTest
import java.io.File

abstract class AbstractClassicJsKlibEvolutionTest : AbstractJsKlibEvolutionTest(CompilerType.K1)
abstract class AbstractFirJsKlibEvolutionTest : AbstractJsKlibEvolutionTest(CompilerType.K2) {
    // Const evaluation tests muted for FIR because FIR does const propagation.
    override fun isIgnoredTest(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast('/')
        return fileName == "addOrRemoveConst.kt" || fileName == "changeConstInitialization.kt"
    }
}

abstract class AbstractJsKlibEvolutionTest(val compilerType: CompilerType) : AbstractKlibBinaryCompatibilityTest() {
    enum class CompilerType {
        K1,
        K2 {
            override fun setup(args: K2JSCompilerArguments) {
                args.languageVersion = "2.0" // Activate FIR.
            }
        };

        open fun setup(args: K2JSCompilerArguments) = Unit
    }

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

    private fun runnerFunctionFile(): Pair<String, File> {
        val file = File(workingDir, RUNNER_FUNCTION_FILE)
        val text = runnerFileText
        file.writeText(runnerFileText)
        return text to file
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
            compilerType.setup(this)
        }
        K2JSCompiler().exec(TestMessageCollector(), Services.EMPTY, args)
    }

    override fun produceProgram(module: TestModule) {
        assert(!module.hasVersions)

        val (text, file) = runnerFunctionFile()
        TestFile(module, file.name, text, Directives())

        produceKlib(module, version = 2)

        val args = K2JSCompilerArguments().apply {
            libraries = module.dependenciesToLibrariesArg(version = 2)
            includes = File(workingDir, module.name(version = 2).klib).absolutePath
            outputDir = jsOutDir.normalize().absolutePath
            moduleName = module.name
            irProduceJs = true
            irOnly = true
            irModuleName = module.name
            compilerType.setup(this)
            partialLinkageMode = "disable" // Don't use partial linkage for KLIB evolution tests.
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

private class TestMessageCollector : MessageCollector {
    override fun clear() {}
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity.isError()) error(message)
    }
    override fun hasErrors(): Boolean = error("Unsupported operation")
}
