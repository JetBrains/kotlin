/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.jsOutputName
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.TsCompilationStrategy
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.config.moduleKind
import org.jetbrains.kotlin.js.tsexport.TypeScriptExportConfig
import org.jetbrains.kotlin.js.tsexport.TypeScriptModuleConfig
import org.jetbrains.kotlin.js.tsexport.runTypeScriptExport
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.TS_COMPILATION_STRATEGY
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.testInfraError
import kotlin.io.path.Path

class AnalysisApiBasedDtsGeneratorFacade(
    private val testServices: TestServices,
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>() {
    override val inputKind: TestArtifactKind<BinaryArtifacts.KLib>
        get() = ArtifactKinds.KLib

    override val outputKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    override fun shouldTransform(module: TestModule): Boolean =
        JsEnvironmentConfigurator.isMainModule(module, testServices)

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Js {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
        val moduleKind = configuration.moduleKind ?: ModuleKind.PLAIN

        val tsCompilationStrategy = testServices.moduleStructure.allDirectives[TS_COMPILATION_STRATEGY].last()
        val translationModes = when (tsCompilationStrategy) {
            TsCompilationStrategy.MERGED -> listOf(TranslationMode.FULL_DEV)
            TsCompilationStrategy.EACH_FILE -> JsEnvironmentConfigurator
                .getTranslationModesForTest(testServices, module)
                .filter { !it.production }
            TsCompilationStrategy.NONE -> testInfraError("${this::class} does not support TsCompilationStrategy.NONE")
        }

        val result = translationModes.associateWith { mode ->
            val config = TypeScriptExportConfig(
                targetPlatform = testServices.targetPlatformProvider.getTargetPlatform(module),
                artifactConfiguration = WebArtifactConfiguration(
                    moduleKind = moduleKind,
                    moduleName = configuration.moduleName!!,
                    outputDirectory = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, mode).absoluteFile,
                    outputName = JsEnvironmentConfigurator.getJsModuleArtifactName(testServices, module.name),
                    granularity = mode.granularity,
                    tsCompilationStrategy = tsCompilationStrategy,
                    production = false, // irrelevant
                    minimizedMemberNames = false, // irrelevant
                ),
                compileLongAsBigInt = JsEnvironmentConfigurationDirectives.ES6_MODE in module.directives,
                implementableInterfaces = configuration.languageVersionSettings.supportsFeature(LanguageFeature.JsExportInterfacesInImplementableWay),
                exportableSuspendLambdas = configuration.languageVersionSettings.supportsFeature(LanguageFeature.JsExportingSuspendLambdas),
                dataClassCopyRespectsConstructorVisibility = configuration.languageVersionSettings.supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility),
            )
            val runtimeKlibs = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
            val regularDependencies = module.transitiveRegularDependencies(reverseOrder = true)
            val klibFriendDependencies = module.transitiveFriendDependencies(reverseOrder = true)
            val inputModules: List<KlibInputModule<TypeScriptModuleConfig>> = buildList {
                for (runtimeKlib in runtimeKlibs) {
                    add(createInputModule(runtimeKlib))
                }
                for (dependency in regularDependencies) {
                    add(createInputModule(dependency))
                }
                for (dependency in klibFriendDependencies) {
                    add(createInputModule(dependency))
                }
                add(createInputModule(module))
            }

            runTypeScriptExport(inputModules, config)
        }

        return JsTypeScriptArtifact(result[TranslationMode.FULL_DEV]!!.single())
    }

    private fun createInputModule(libraryPath: String): KlibInputModule<TypeScriptModuleConfig> {
        val result = KlibLoader { libraryPaths(libraryPath) }.load()
        result.reportLoadingProblemsIfAny { _, message -> testServices.assertions.fail { message } }
        val library = result.librariesStdlibFirst[0]
        return KlibInputModule(library.uniqueName, Path(libraryPath), TypeScriptModuleConfig(outputName = library.jsOutputName))
    }

    private fun createInputModule(testModule: TestModule): KlibInputModule<TypeScriptModuleConfig> {
        val klib = testServices.artifactsProvider.getArtifact(testModule, ArtifactKinds.KLib).outputFile
        return createInputModule(klib.absolutePath)
    }
}
