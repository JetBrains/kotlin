/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.providers

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceProviders.AbstractLauncherAdditionalSourceProvider

class WasmJsLauncherAdditionalSourceProvider(testServices: TestServices) : AbstractLauncherAdditionalSourceProvider(testServices) {
    override fun generateLauncherContent(boxFqName: String, expectedResult: String): String =
        error("Use overload with testFile")

    override fun generateLauncherContent(boxFqName: String, testFile: TestFile, expectedResult: String): String {
        val uniqueName = testFile.relativePath.replace('/', '_').replace('.', '_')
        val launcherClassName = "Launcher_$uniqueName"
        return """
            class $launcherClassName {
                @kotlin.test.Test
                fun runTest() {
                    val result = $boxFqName()
                    kotlin.test.assertEquals("$expectedResult", result, "Test failed with: ${'$'}result")
                }
            }
        """.trimIndent()
    }

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val launcherFiles = super.produceAdditionalFiles(globalDirectives, module, testModuleStructure)
        if (launcherFiles.isEmpty()) return emptyList()

        val launcherFile = launcherFiles.single()
        val file = launcherFile.originalFile
        file.appendText(
            """

                @kotlin.wasm.WasmExport
                fun hasTestFailures(): Boolean {
                    return kotlin.test.hasTestFailures()
                }
            """.trimIndent()
        )
        return launcherFiles
    }
}
