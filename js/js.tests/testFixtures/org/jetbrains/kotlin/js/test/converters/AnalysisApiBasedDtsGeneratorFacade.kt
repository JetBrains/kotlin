/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.tsexport.TypeScriptExportConfig
import org.jetbrains.kotlin.js.tsexport.TypeScriptModuleConfig
import org.jetbrains.kotlin.js.tsexport.createTypeScriptExportInputModule
import org.jetbrains.kotlin.js.tsexport.runTypeScriptExport
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.TS_COMPILATION_STRATEGY
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import kotlin.io.path.Path

class AnalysisApiBasedDtsGeneratorFacade(
    private val testServices: TestServices,
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>() {
    companion object {
        fun createExportConfig(
            targetPlatform: TargetPlatform,
            artifactConfiguration: WebArtifactConfiguration,
            configuration: CompilerConfiguration,
        ): TypeScriptExportConfig = TypeScriptExportConfig(
            targetPlatform = targetPlatform,
            artifactConfiguration = artifactConfiguration,
            compileLongAsBigInt = configuration.compileLongAsBigint,
            implementableInterfaces = configuration.languageVersionSettings.supportsFeature(LanguageFeature.JsExportInterfacesInImplementableWay),
            exportableSuspendLambdas = configuration.languageVersionSettings.supportsFeature(LanguageFeature.JsExportingSuspendLambdas),
            useUnknownInsteadAny = configuration.exportUntypedAsUnknown,
            dataClassCopyRespectsConstructorVisibility = configuration.languageVersionSettings.supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility),
            additionalExportedDeclarationNames = configuration.additionalExportedDeclarationNames,
        )
    }

    override val inputKind: TestArtifactKind<BinaryArtifacts.KLib>
        get() = ArtifactKinds.KLib

    override val outputKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    override fun shouldTransform(module: TestModule): Boolean =
        JsEnvironmentConfigurator.isMainModule(module, testServices)

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Js {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.SECOND)
        val moduleKind = configuration.moduleKind ?: ModuleKind.PLAIN

        val tsCompilationStrategy = testServices.moduleStructure.allDirectives[TS_COMPILATION_STRATEGY].last()
        val translationModes = JsEnvironmentConfigurator.getTypeScriptExportTranslationModes(testServices, module)

        val result = translationModes.associateWith { mode ->
            val config = createExportConfig(
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
                configuration = configuration,
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

        // TODO(KT-88562): This result is not used down the pipeline, so it doesn't matter what we put here. We need to refactor this
        //   so `JsTypeScriptArtifact` stores a list of files for each translation mode.
        return JsTypeScriptArtifact(result[TranslationMode.FULL_DEV]?.first() ?: result.values.first().first())
    }

    private fun createInputModule(libraryPath: String): KlibInputModule<TypeScriptModuleConfig> =
        createTypeScriptExportInputModule(Path(libraryPath)) { _, message -> testServices.assertions.fail { message } }

    private fun createInputModule(testModule: TestModule): KlibInputModule<TypeScriptModuleConfig> {
        val klib = testServices.artifactsProvider.getArtifact(testModule, ArtifactKinds.KLib).outputFile
        return createInputModule(klib.absolutePath)
    }
}
