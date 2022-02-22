/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.test.utils.LineCollector
import org.jetbrains.kotlin.js.test.utils.LineOutputToStringVisitor
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File

class JsLineNumberHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {

    companion object {
        private val LINES_PATTERN = Regex("^ *// *LINES: *(.*)$", RegexOption.MULTILINE)
    }

    private val defaultTranslationMode = TranslationMode.PER_MODULE

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        val translationResult = when (val artifact = info.unwrap()) {
            is BinaryArtifacts.Js.OldJsArtifact -> artifact.translationResult as TranslationResult.Success
            // TODO: Support JS IR
//            is BinaryArtifacts.Js.JsIrArtifact -> artifact.compilerResult.outputs[defaultTranslationMode]!!.jsProgram!!
            else -> error("This artifact is not supported")
        }

        val jsProgram = translationResult.program

        val baseOutputPath = JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name, defaultTranslationMode)

        val lineCollector = LineCollector()
        lineCollector.accept(jsProgram)

        val programOutput = TextOutputImpl()
        jsProgram.globalBlock.accept(LineOutputToStringVisitor(programOutput, lineCollector))
        val generatedCode = programOutput.toString()

        with(File("$baseOutputPath-lines.js")) {
            parentFile.mkdirs()
            writeText(generatedCode)
        }

        val linesMatcher = module.files
            .firstNotNullOfOrNull { LINES_PATTERN.find(it.originalContent) }
            ?: error("'// LINES: ' comment was not found in source file. Generated code is:\n$generatedCode")

        val expectedLines = linesMatcher.groups[1]!!.value
        val actualLines = lineCollector.lines
            .dropLastWhile { it == null }
            .joinToString(" ") { if (it == null) "*" else (it + 1).toString() }

        testServices.assertions.assertEquals(expectedLines, actualLines) { generatedCode }
    }
}
