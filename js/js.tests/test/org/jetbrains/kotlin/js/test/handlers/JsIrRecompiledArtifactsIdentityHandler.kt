/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.test.converters.ClassicJsBackendFacade
import org.jetbrains.kotlin.js.test.converters.augmentWithModuleName
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts.Js
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File

class JsIrRecompiledArtifactsIdentityHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: Js) {
        if (info !is Js.IncrementalJsArtifact) return
        val (originalArtifact, incrementalArtifact) = info
        when {
            originalArtifact is Js.JsIrArtifact && incrementalArtifact is Js.JsIrArtifact -> {
                compareIrArtifacts(originalArtifact, incrementalArtifact)
            }
            else -> assertions.fail {
                """
                    Incompatible types of original and incremental artifacts:
                      original: ${originalArtifact::class}
                      incremental: ${incrementalArtifact::class}
                """.trimIndent()
            }
        }

    }

    @Suppress("UNUSED_PARAMETER")
    private fun compareIrArtifacts(originalArtifact: Js.JsIrArtifact, incrementalArtifact: Js.JsIrArtifact) {
        // TODO: enable asserts when binary stability is achieved
        val oldBinaryAsts = originalArtifact.icCache!!
        val newBinaryAsts = incrementalArtifact.icCache!!

        for (file in newBinaryAsts.keys) {
            val oldBinaryAst = oldBinaryAsts[file]
            val newBinaryAst = newBinaryAsts[file]

            testServices.assertions.assertTrue(oldBinaryAst.contentEquals(newBinaryAst)) {
                "Binary AST changed after recompilation for file $file"
            }
        }

        val originalFilesToCheck = originalArtifact.allFiles()
        val recompiledFilesToCheck = incrementalArtifact.allFiles()

        testServices.assertions.assertEquals(originalFilesToCheck.size, recompiledFilesToCheck.size)

        for ((originalFile, recompiledFile) in originalFilesToCheck.zip(recompiledFilesToCheck)) {
            testServices.assertions.assertEquals(originalFile.name, recompiledFile.name)

            val originalOutput = FileUtil.loadFile(originalFile)
            val recompiledOutput = FileUtil.loadFile(recompiledFile)

            testServices.assertions.assertEquals(originalOutput, recompiledOutput) {
                "Output file changed after recompilation"
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

private fun Js.JsIrArtifact.allFiles(): Collection<File> {
    return listOf(outputFile) + compilerResult.outputs[TranslationMode.FULL_DEV]!!.dependencies.map { (moduleId, _) ->
        outputFile.augmentWithModuleName(moduleId)
    }.sortedBy { it.name }
}
