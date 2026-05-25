/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.ComposeConfiguration
import androidx.compose.compiler.plugins.kotlin.ComposeIrGenerationExtension
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.k2.ComposeFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.DISABLE_NEXT_PHASE_SUGGESTION
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import org.opentest4j.MultipleFailuresError
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

private const val TEST_DATA_PATH = "plugins/compose/compiler-hosted/integration-tests/testData/analysis"
private const val JAVA_CLASS_PATH = "java.class.path"
private const val UPDATE_TEST_DATA = "kotlin.test.update.test.data"

open class AbstractComposeAnalysisDiagnosticTest : AbstractPhasedJvmDiagnosticLightTreeTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +WITH_STDLIB
            +RENDER_DIAGNOSTICS_FULL_TEXT
            +DISABLE_GENERATED_FIR_TAGS
            DISABLE_NEXT_PHASE_SUGGESTION with "Compose analysis tests intentionally stop at frontend diagnostics"
        }
        builder.useConfigurators(
            ::ComposeAnalysisExtensionRegistrarConfigurator,
            ::ComposeAnalysisJvmClasspathConfigurator,
        )
    }
}

class ComposeAnalysisDiagnosticTests : AbstractComposeAnalysisDiagnosticTest() {
    @TestFactory
    fun testAnalysisData(): Stream<DynamicTest> {
        val root = File(TEST_DATA_PATH)
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.invariantSeparatorsPath }
            .map { file ->
                DynamicTest.dynamicTest(file.relativeTo(root).invariantSeparatorsPath) {
                    runTestWithUpdateSupport(file.invariantSeparatorsPath)
                }
            }
            .asStream()
    }

    private fun runTestWithUpdateSupport(testDataFilePath: String) {
        try {
            runTest(testDataFilePath)
        } catch (e: Throwable) {
            if (System.getProperty(UPDATE_TEST_DATA) == "true") {
                e.updateTestData()
            }
            throw e
        }
    }
}

private fun Throwable.updateTestData() {
    when (this) {
        is AssertionFailedError -> updateTestData()
        is MultipleFailuresError -> failures.forEach(Throwable::updateTestData)
    }
}

private fun AssertionFailedError.updateTestData() {
    val fileInfo = expected?.value as? FileInfo ?: return
    File(fileInfo.path).writeText(actual.stringRepresentation)
}

private class ComposeAnalysisExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(ComposeFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(
            ComposeIrGenerationExtension(
                useK2 = true,
                featureFlags = FeatureFlags(configuration[ComposeConfiguration.FEATURE_FLAGS, emptyList()]),
            )
        )
    }
}

private class ComposeAnalysisJvmClasspathConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform(testServices)
        check(platform.isJvm()) {
            "Compose analysis diagnostic tests support only JVM"
        }
        composeLibraries.forEach(configuration::addJvmClasspathRoot)
    }
}

private val composeLibraries by lazy {
    val classPath = System.getProperty(JAVA_CLASS_PATH) ?: error("System property \"$JAVA_CLASS_PATH\" is not found")
    classPath.split(File.pathSeparator).map(::File).filter { it.absolutePath.contains("androidx.compose") }
}
