/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.codegen.extractUrls
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWasmFacade
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerTest
import org.jetbrains.kotlin.test.klib.CustomKlibCompiler
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.WasmWasiBoxTestHelperSourceProvider
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmFirstStageTest
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmSecondStageTest
import org.jetbrains.kotlin.wasm.test.configureCodegenFirHandlerSteps
import org.jetbrains.kotlin.wasm.test.handlers.WasmFolderBoxRunnerGroupingStage
import org.jetbrains.kotlin.wasm.test.handlers.WasmWasiFolderBoxRunnerGroupingStage
import org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.wasm.test.setupStepsForWasmFirstStageUpToSerialization
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor
import java.io.File
import java.net.URL
import kotlin.String

abstract class AbstractWasmCodegenBoxTest(
    val backend: TargetBackend,
    val platform: TargetPlatform,
    val wasmTarget: WasmTarget,
): AbstractTwoStageKotlinCompilerTest() {
    abstract val currentWebCompilerSettings: CustomWebCompilerSettings
    abstract val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>
    abstract val wasmFolderBoxRunner: Constructor<GroupingStageHandler<BinaryArtifacts.Wasm>>

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = backend
            }
            commonConfigurationForWasmFirstStageTest(
                targetFrontend = FrontendKinds.FIR,
                targetPlatform = platform,
                wasmTarget = wasmTarget,
                additionalSourceProvider = null,
                customIgnoreDirective = null,
                additionalIgnoreDirectives = null,
            )
            defaultDirectives {
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
                FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
                DIAGNOSTICS with listOf("-infos")
            }
            configureIgnoredTestSuppressor()
            useFailureSuppressors(
                ::FirMetaInfoDiffSuppressor,
            )
        }
        nonGroupingStage {
            enableMetaInfoHandler()
            useGroupingTestIsolators(::WasmGroupingTestIsolator)
            additionalSourceProviders.forEach { useAdditionalSourceProviders(it) }
            commonCodegenConfiguration()

            setupStepsForWasmFirstStageUpToSerialization(
                includeBasicFirHandlers = true,
                // Due to package escaping, various dumps for grouping mode would be different from the regular one,
                // so we don't want all the frontend handlers to be set up, only some specific ones.
                includeDumpFirHandlers = false,
            )
            configureCodegenFirHandlerSteps()

            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::WasmFirstStageEnvironmentConfigurator.bind(wasmTarget),
            )

            configureIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            configureLoweredIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            facadeStep(::FirKlibSerializerCliWasmFacade)
            klibArtifactsHandlersStep()

            commonConfigurationForWasmSecondStageTest(
                pathToTestDir = "compiler/testData/codegen/",
                testGroupOutputDirPrefix = this@AbstractWasmCodegenBoxTest::class.java.simpleName +
                        currentWebCompilerSettings.defaultLanguageVersion,
            )

            forTestsNotMatching(
                "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                        "compiler/testData/diagnostics/*"
            ) {
                defaultDirectives {
                    DIAGNOSTICS with "-warnings"
                }
            }
        }
        groupingStage {
            useConfigurators(::WasmSecondStageEnvironmentConfigurator.bind(wasmTarget))

            facadeStep(WasmCompilerSecondStageFacade::Grouping)
            handlersStep(ArtifactKinds.Wasm, CompilationStage.SECOND) {
                useHandlers(wasmFolderBoxRunner)
            }
        }
    }
}

abstract class AbstractWasmJsCodegenBoxTest : AbstractWasmCodegenBoxTest(TargetBackend.WASM_JS, WasmPlatforms.wasmJs, WasmTarget.JS) {
    companion object {
        val currentWasmJsCompilerSettings = currentWasmCompilerSettings(WasmTarget.JS)
    }

    override val currentWebCompilerSettings = currentWasmJsCompilerSettings
    override val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>> = listOf(::WasmJsLauncherAdditionalSourceProvider)
    override val wasmFolderBoxRunner: Constructor<GroupingStageHandler<BinaryArtifacts.Wasm>> = ::WasmFolderBoxRunnerGroupingStage
}

abstract class AbstractWasmWasiCodegenBoxTest : AbstractWasmCodegenBoxTest(TargetBackend.WASM_WASI, WasmPlatforms.wasmWasi, WasmTarget.WASI) {
    companion object {
        val currentWasmWasiCompilerSettings = currentWasmCompilerSettings(WasmTarget.WASI)
    }

    override val currentWebCompilerSettings = currentWasmWasiCompilerSettings
    override val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>> = listOf(
        ::WasmJsLauncherAdditionalSourceProvider,
        ::WasmWasiBoxTestHelperSourceProvider,
    )
    override val wasmFolderBoxRunner: Constructor<GroupingStageHandler<BinaryArtifacts.Wasm>> = ::WasmWasiFolderBoxRunnerGroupingStage
}

abstract class AbstractWasmJsCodegenBoxTestWithInlinedFunInKlibTest : AbstractWasmJsCodegenBoxTest() {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}",
                )
            }
        }
    }
}

abstract class AbstractWasmWasiCodegenBoxTestWithInlinedFunInKlibTest : AbstractWasmWasiCodegenBoxTest() {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}",
                )
            }
        }
    }
}

fun currentWasmCompilerSettings(wasmTarget: WasmTarget) = object : CustomWebCompilerSettings {
    override val version: String
        get() = LanguageVersion.LATEST_STABLE.versionString
    override val stdlib: File
        get() = File(WasmEnvironmentConfigurator.stdlibPath(wasmTarget))
    override val kotlinTest: File
        get() = File(WasmEnvironmentConfigurator.kotlinTestPath(wasmTarget))
    override val customKlibCompiler: CustomKlibCompiler by lazy {
        val compilerClassPath = this.javaClass.classLoader.extractUrls().ifEmpty {
            // For non-URLClassLoader (e.g. jdk.internal.loader.BuiltinClassLoader on JDK 9+), fall back to the java.class.path system property.
            urlsFromClassPath()
        }
        require(compilerClassPath.isNotEmpty()) { "Compiler classpath cannot be empty for class ${this.javaClass.name}" }
        CustomKlibCompiler(
            compilerClassPath,
            KotlinWasmCompiler::class.java.name,
            "execFullPathsInMessages"
        )
    }
}

private fun urlsFromClassPath(): List<URL> {
    val classPath = System.getProperty("java.class.path") ?: return emptyList()
    return classPath.split(File.pathSeparator)
        .filter { it.isNotEmpty() }
        .map { File(it).toURI().toURL() }
}
