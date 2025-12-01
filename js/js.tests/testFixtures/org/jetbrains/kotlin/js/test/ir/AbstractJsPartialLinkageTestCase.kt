/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestStructureExtractor
import java.io.File

abstract class AbstractJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.NO_IC)
abstract class AbstractJsPartialLinkageNoICES6TestCase : AbstractJsPartialLinkageTestCase(CompilerType.NO_IC_WITH_ES6)
abstract class AbstractJsPartialLinkageWithICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.WITH_IC)

abstract class AbstractJsPartialLinkageTestCase(compilerType: CompilerType) : AbstractJsCompilerInvocationTest(compilerType) {
    // The entry point to generated test classes.
    fun runTest(@TestDataFile testDir: String) {
        val configuration = JsCompilerInvocationTestConfiguration(
            buildDir = buildDir,
            compilerType = compilerType,
        )

        KlibCompilerInvocationTestUtils.runTest(
            testStructure = JsPartialLinkageTestStructureExtractor(buildDir)
                .extractTestStructure(
                    ForTestCompileRuntime.transformTestDataPath(testDir)
                ),
            testConfiguration = configuration,
            artifactBuilder = JsCompilerInvocationTestArtifactBuilder(configuration),
            binaryRunner = JsCompilerInvocationTestBinaryRunner,
            compilerEditionChange = KlibCompilerChangeScenario.NoChange,
        )
    }
}

private class JsPartialLinkageTestStructureExtractor(
    override val buildDir: File,
) : PartialLinkageTestStructureExtractor() {
    override val testModeConstructorParameters = mapOf("isJs" to "true")

    override fun customizeModuleSources(moduleName: String, moduleSourceDir: File) {
        if (moduleName == KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME) {
            // Add the @JsExport annotation to make the box function visible to Node.
            moduleSourceDir.walkTopDown().forEach { file ->
                if (file.extension == "kt") {
                    var modified = false
                    val lines = file.readLines().map { line ->
                        if (line.startsWith("fun $BOX_FUN_FQN()")) {
                            modified = true
                            "@OptIn(ExperimentalJsExport::class) @JsExport $line"
                        } else
                            line
                    }
                    if (modified) file.writeText(lines.joinToString("\n"))
                }
            }
        }
    }

    companion object {
        private const val BOX_FUN_FQN = "box"
    }
}
