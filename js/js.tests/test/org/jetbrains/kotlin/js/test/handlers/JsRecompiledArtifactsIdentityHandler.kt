/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts.Js
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

class JsRecompiledArtifactsIdentityHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: Js) {
        if (info !is Js.IncrementalJsArtifact) return
        val (originalArtifact, incrementalArtifact) = info
        when {
            originalArtifact is Js.OldJsArtifact && incrementalArtifact is Js.OldJsArtifact -> {
                compareArtifacts(originalArtifact, incrementalArtifact)
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

    private fun compareArtifacts(originalArtifact: Js.OldJsArtifact, incrementalArtifact: Js.OldJsArtifact) {
        val originalSourceMap = FileUtil.loadFile(File(originalArtifact.outputFile.parentFile, originalArtifact.outputFile.name + ".map"))
        val recompiledSourceMap =
            removeRecompiledSuffix(
                FileUtil.loadFile(File(incrementalArtifact.outputFile.parentFile, incrementalArtifact.outputFile.name + ".map"))
            )

        if (originalSourceMap != recompiledSourceMap) {
            val originalSourceMapParse = SourceMapParser.parse(originalSourceMap)
            val recompiledSourceMapParse = SourceMapParser.parse(recompiledSourceMap)
            if (originalSourceMapParse is SourceMapSuccess && recompiledSourceMapParse is SourceMapSuccess) {
                testServices.assertions.assertEquals(
                    originalSourceMapParse.toDebugString(),
                    recompiledSourceMapParse.toDebugString(),
                ) { "Source map file changed after recompilation" }
            }
            testServices.assertions.assertEquals(originalSourceMap, recompiledSourceMap) { "Source map file changed after recompilation" }
        }
    }

    private fun SourceMapSuccess.toDebugString(): String {
        val out = ByteArrayOutputStream()
        PrintStream(out).use { value.debug(it) }
        return String(out.toByteArray(), Charset.forName("UTF-8"))
    }

    private fun removeRecompiledSuffix(text: String): String = text.replace("-recompiled.js", ".js")


    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
