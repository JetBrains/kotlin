/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.unregisterFinders
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.frontend.fir.FirModuleInfoProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import kotlin.script.experimental.api.CompiledSnippet
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.util.LinkedSnippet

data class ReplCompilationArtifact(
    val compilationResult: ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>>,
) : ResultingArtifact.Binary<ReplCompilationArtifact>() {
    object Kind : BinaryKind<ReplCompilationArtifact>("ReplCompilationArtifact")

    override val kind: BinaryKind<ReplCompilationArtifact> get() = Kind
}


class FirReplCompilerFacade(
    val testServices: TestServices,
) : AbstractTestFacade<ResultingArtifact.Source, ReplCompilationArtifact>() {
    private val testModulesByName by lazy { testServices.moduleStructure.modules.associateBy { it.name } }

    private var replCompiler: K2ReplCompiler? = null

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<ReplCompilationArtifact>
        get() = ReplCompilationArtifact.Kind

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return if (module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            testServices.moduleStructure
                .modules.none { testModule -> testModule.dependsOnDependencies.any { testModulesByName[it.moduleName] == module } }
        } else {
            true
        }
    }

    override fun transform(
        module: TestModule,
        inputArtifact: ResultingArtifact.Source,
    ): ReplCompilationArtifact? {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterFinders<JavaElementFinder>()

        val parser = module.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER)

        val (ktFiles, _) = when (parser) {
            FirParser.LightTree -> {
                emptyMap<TestFile, KtFile>() to testServices.sourceFileProvider.getKtSourceFilesForSourceFiles(module.files)
            }
            FirParser.Psi -> testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project) to emptyMap()
        }

        val compiler =
            replCompiler ?: run {
                val messageCollector = ScriptDiagnosticsMessageCollector(null)
                val baseScriptCompilationConfiguration = ScriptCompilationConfiguration {
                    updateClasspath(
                        listOf(testServices.standardLibrariesPathProvider.runtimeJarForTests())
                    )
                }
                K2ReplCompiler(
                    K2ReplCompiler.createCompilationState(
                        messageCollector,
                        testServices.compilerConfigurationProvider.testRootDisposable,
                        baseScriptCompilationConfiguration
                    )
                )
            }.also { replCompiler = it }

        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compiler.compile(KtFileScriptSource(ktFiles.values.single()))
        }

        return ReplCompilationArtifact(result)
    }
}

//class ReplCompilerDiagnosticsHandler(
//    testServices: TestServices,
//    failureDisablesNextSteps: Boolean = false,
//    doNotRunIfThereWerePreviousFailures: Boolean = false
//) : AnalysisHandler<ReplCompilationArtifact>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures)
//{
//    override val artifactKind: TestArtifactKind<ReplCompilationArtifact> = ReplCompilationArtifact.Kind
//
//    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)
//
//    override fun processModule(module: TestModule, info: ReplCompilationArtifact) {
//        val diagnostics = info.compilationResult.reports
//        for (part in info.partsForDependsOnModules) {
//            val currentModule = part.module
//            for (file in currentModule.files) {
//
//            }
//            val metaInfo = FirDiagnosticCodeMetaInfo(this, FirMetaInfoUtils.renderDiagnosticNoArgs, range)
//
//        }
//    }
//
//    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
//}
