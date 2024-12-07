/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.incremental.getProtoData
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.File
import kotlin.test.assertNotEquals

abstract class AbstractFirJsProtoComparisonTest: AbstractJsProtoComparisonTest(null)

abstract class AbstractJsProtoComparisonTest(val languageVersionOverride: String? = "1.9") : AbstractProtoComparisonTest<ProtoData>() {
    protected open val jsStdlibFile: File
        get() = PathUtil.kotlinPathsForDistDirectoryForTests.jsStdLibKlibPath

    override fun expectedOutputFile(testDir: File): File {
        val k1Out = File(testDir, "result-js.out")
        val k2Out = File(testDir, "result-js.fir.out")
        val resultFile = when {
            languageVersionOverride?.startsWith("1.") == true -> k1Out
            !k2Out.exists() -> k1Out
            else -> {
                assertNotEquals(
                    k1Out.readText(),
                    k2Out.readText(),
                    "Please remove ${k2Out.absolutePath}, since its contents is equal to ${k1Out.absolutePath}."
                )
                k2Out
            }
        }
        return resultFile.takeIf { it.exists() } ?: super.expectedOutputFile(testDir)
    }

    override fun compileAndGetClasses(sourceDir: File, outputDir: File): Map<ClassId, ProtoData> {
        val incrementalResults = IncrementalResultsConsumerImpl()
        val services = Services.Builder().run {
            register(IncrementalResultsConsumer::class.java, incrementalResults)
            build()
        }

        val ktFiles = sourceDir.walkMatching { it.name.endsWith(".kt") }.map { it.canonicalPath }.toList()
        val messageCollector = MessageCollectorImpl()
        val outputItemsCollector = OutputItemsCollectorImpl()
        val args = K2JSCompilerArguments().apply {
            this.outputDir = outputDir.normalize().absolutePath
            moduleName = "out"
            libraries = jsStdlibFile.absolutePath
            irProduceKlibDir = true
            main = K2JsArgumentConstants.NO_CALL
            freeArgs = ktFiles
            languageVersionOverride?.let { languageVersion = it }
        }

        val env = createTestingCompilerEnvironment(messageCollector, outputItemsCollector, services)
        runJSCompiler(args, env).let { exitCode ->
            val expectedOutput = "OK"
            val actualOutput = (listOf(exitCode?.name) + messageCollector.errors.map { it.message }).joinToString("\n")
            Assert.assertEquals(expectedOutput, actualOutput)
        }

        val classes = hashMapOf<ClassId, ProtoData>()

        for ((sourceFile, translationResult) in incrementalResults.packageParts) {
            classes.putAll(getProtoData(sourceFile, translationResult.metadata))
        }

        return classes
    }

    override fun ProtoData.toProtoData(): ProtoData? = this
}
