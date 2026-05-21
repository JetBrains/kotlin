/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.io.File

class WasmWasiBoxTestHelperSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val fileWithBoxFun = module.files.singleOrNull {
            it.isKtFile && it.originalContent.contains(Regex("(^|\\n)\\bfun\\s+box\\(\\)\\s*(?::\\s*String|=)"))
        }

        // no box function
        if (fileWithBoxFun == null) return emptyList()

        val matchResult = Regex("^package\\s+([\\w.]+)", RegexOption.MULTILINE).find(fileWithBoxFun.originalContent)

        val boxTestRunFile = this::class.java.classLoader.getResource("wasiAdditionalFiles/wasiBoxTestRun.kt")!!
        val boxTestRunTestFile = boxTestRunFile.toTestFile()

        // no package
        if (matchResult == null) return listOf(boxTestRunTestFile)

        val p = matchResult.groupValues[1]
        return listOf(
            TestFile(
                boxTestRunTestFile.name,
                boxTestRunFile.readText().replace("box()", "$p.box()"),
                originalFile = boxTestRunTestFile.originalFile,
                startLineNumberInOriginalFile = 0,
                isAdditional = true,
                directives = RegisteredDirectives.Empty
            )
        )
    }
}
