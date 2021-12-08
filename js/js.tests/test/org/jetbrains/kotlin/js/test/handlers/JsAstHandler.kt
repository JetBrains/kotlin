/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsNullLiteral
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.testOld.utils.DirectiveTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.isKtFile

class JsAstHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        val ktFiles = module.files.filter { it.isKtFile }.map { it.originalContent }
        val jsProgram = when (val artifact = info.unwrap()) {
            is BinaryArtifacts.Js.OldJsArtifact -> (artifact.translationResult as TranslationResult.Success).program
            is BinaryArtifacts.Js.JsIrArtifact -> artifact.compilerResult.outputs[TranslationMode.FULL]?.jsProgram ?: return
            else -> return
        }
        processJsProgram(jsProgram, ktFiles, module.targetBackend!!)
    }

    private fun processJsProgram(program: JsProgram, psiFiles: List<String>, targetBackend: TargetBackend) {
        psiFiles.forEach { DirectiveTestUtils.processDirectives(program, it, targetBackend) }

        // TODO: For now the IR backend generates JS code that doesn't pass verification,
        // TODO: so we temporarily disabled AST verification.
        if (targetBackend == TargetBackend.JS) {
            program.verifyAst()
        }
    }

    private fun JsProgram.verifyAst() {
        accept(object : RecursiveJsVisitor() {
            override fun visitExpressionStatement(x: JsExpressionStatement) {
                when (x.expression) {
                    is JsNullLiteral -> testServices.assertions.fail { "Expression statement contains `null` literal" }
                    else -> super.visitExpressionStatement(x)
                }
            }
        })
    }
}
