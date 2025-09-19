/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.compatibility.binary.AbstractKlibBinaryCompatibilityTest
import org.jetbrains.kotlin.compatibility.binary.TestFile
import org.jetbrains.kotlin.compatibility.binary.TestModule
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.test.Directives
import java.io.File

abstract class AbstractFirJsKlibEvolutionTest : AbstractKlibBinaryCompatibilityTest() {
    override val extensionConfigs: EnvironmentConfigFiles = EnvironmentConfigFiles.JS_CONFIG_FILES
    override val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
    override val stdlibDependency: String? = System.getProperty("kotlin.js.full.stdlib.path")
    private val TestModule.jsPath get() = File(jsOutDir, "${this.name}.js").absolutePath

    private fun runnerFunctionFile(): Pair<String, File> {
        val file = File(workingDir, RUNNER_FUNCTION_FILE)
        val text = runnerFileText
        file.writeText(runnerFileText)
        return text to file
    }

    override fun produceKlib(module: TestModule, version: Int) {
        runJsCompiler {
            freeArgs = createFiles(module.versionFiles(version))
            libraries = module.dependenciesToLibrariesArg(version = version)
            outputDir = workingDir.normalize().absolutePath
            moduleName = module.name(version)
            irProduceKlibFile = true
            irModuleName = module.name
        }
    }

    override fun produceProgram(module: TestModule) {
        assert(!module.hasVersions)

        val (text, file) = runnerFunctionFile()
        TestFile(module, file.name, text, Directives())

        produceKlib(module, version = 2)

        runJsCompiler {
            libraries = module.dependenciesToLibrariesArg(version = 2)
            includes = File(workingDir, module.name(version = 2).klib).absolutePath
            outputDir = jsOutDir.normalize().absolutePath
            moduleName = module.name
            irProduceJs = true
            irModuleName = module.name
            partialLinkageMode = "disable" // Don't use partial linkage for KLIB evolution tests.
        }
    }

    override fun runProgram(module: TestModule, expectedResult: String) {
        testChecker.check(listOf(module.jsPath), module.name, null, RUNNER_FUNCTION, expectedResult, false)
    }

    protected open val testChecker get() = V8JsTestChecker

    companion object {
        private val String.klib: String get() = "$this.$KLIB_FILE_EXTENSION"
        private val String.js: String get() = "$this.js"

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
