/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.test.utils.LineCollector
import org.jetbrains.kotlin.js.test.utils.LineOutputToStringVisitor
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Verifies the `// LINE` comments in lineNumber tests.
 *
 * The test file is expected to contain the `// LINE($linePattern)` directive,
 * followed by the line numbers that the corresponding JS statements are generated from.
 *
 * This handler traverses the JS AST and collects the actual line numbers using [LineCollector], and generates a JavaScript file
 * with those line numbers printed as comments for ease of debugging these tests.
 */
private class JsLineNumberHandler(private val frontend: FrontendKind<*>, testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    private val translationModeForIr = TranslationMode.PER_MODULE_DEV

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        when (val artifact = info.unwrap()) {
            is BinaryArtifacts.Js.JsIrArtifact -> {
                val testModules = testServices.moduleStructure.modules
                val moduleId2TestModule = testModules.associateBy { it.name.safeModuleName }

                var verifiedModuleCount = 0

                fun verifyModulesRecursively(
                    module: TestModule,
                    compilationOutputs: CompilationOutputs,
                ) {
                    for ((moduleId, dependencyOutputs) in compilationOutputs.dependencies) {
                        moduleId2TestModule[moduleId]?.let {
                            verifyModulesRecursively(it, dependencyOutputs)
                        }
                    }

                    verifyModule(module, translationModeForIr, compilationOutputs.jsProgram!!)
                    verifiedModuleCount += 1
                }

                verifyModulesRecursively(module, artifact.compilerResult.outputs[translationModeForIr]!!)

                // Just a sanity check to make sure we indeed verify all the needed modules.
                assert(verifiedModuleCount == testModules.size) {
                    "The number of verified modules ($verifiedModuleCount) must match " +
                            "the number of all the test modules (${testModules.size})"
                }
            }
            else -> error("This artifact is not supported")
        }
    }

    private fun verifyModule(
        module: TestModule,
        translationMode: TranslationMode,
        jsProgram: JsProgram
    ) {
        val baseOutputPath = JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, translationMode)

        val lineCollector = LineCollector()
        lineCollector.accept(jsProgram)

        val generatedCode = kotlin.run {
            val programOutput = TextOutputImpl()
            jsProgram.globalBlock.accept(LineOutputToStringVisitor(programOutput, lineCollector))
            programOutput.toString()
        }

        with(File("$baseOutputPath-lines.js")) {
            parentFile.mkdirs()
            writeText(generatedCode)
        }

        val linesPattern = Regex("^ *// *LINES\\((?:$frontend )? *JS_IR\\): *(.*)$", RegexOption.MULTILINE)

        val linesMatcher = module.files
            .firstNotNullOfOrNull { linesPattern.find(it.originalContent) }
            ?: testServices.assertions.fail {
                "'// LINES(${linesPattern.pattern}): ' comment was not found in source file. Generated code is:\n$generatedCode"
            }

        fun List<Int?>.render() = joinToString(" ") { it?.toString() ?: "*" }

        val expectedLines =
            linesMatcher.groups[1]!!.value.split(Regex("\\s+")).map { if (it == "*") null else it.toInt() }.render()

        val actualLines = lineCollector.lines
            .dropLastWhile { it == null }
            .map { lineNumber -> lineNumber?.let { it + 1 } }
            .render()

        testServices.assertions.assertEquals(expectedLines, actualLines) { generatedCode }
    }
}

fun createIrJsLineNumberHandler(testServices: TestServices): JsBinaryArtifactHandler {
    return JsLineNumberHandler(FrontendKinds.ClassicFrontend, testServices)
}

fun createFirJsLineNumberHandler(testServices: TestServices): JsBinaryArtifactHandler {
    return JsLineNumberHandler(FrontendKinds.FIR, testServices)
}
