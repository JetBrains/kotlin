/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.unregisterFinders
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
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
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.util.LinkedSnippet

data class ReplCompilationArtifact(
    val snippetSource: KtFileScriptSource,
    val compilationResult: ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>>,
) : ResultingArtifact.Binary<ReplCompilationArtifact>() {
    object Kind : ArtifactKind<ReplCompilationArtifact>("ReplCompilationArtifact")

    override val kind: ArtifactKind<ReplCompilationArtifact> get() = Kind
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

    override fun shouldTransform(module: TestModule): Boolean {
        return if (module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            testServices.moduleStructure
                .modules.none { testModule -> testModule.dependsOnDependencies.any { testModulesByName[it.dependencyModule.name] == module } }
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

        require(parser == FirParser.Psi)

        val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project)

        val compiler =
            replCompiler ?: run {
                val messageCollector = ScriptDiagnosticsMessageCollector(null)
                val baseScriptCompilationConfiguration = ScriptCompilationConfiguration {
                    updateClasspath(
                        listOf(testServices.standardLibrariesPathProvider.runtimeJarForTests())
                    )
                    compilerOptions("-Xrender-internal-diagnostic-names=true")
                }
                K2ReplCompiler(
                    K2ReplCompiler.createCompilationState(
                        messageCollector,
                        testServices.compilerConfigurationProvider.testRootDisposable,
                        baseScriptCompilationConfiguration
                    )
                )
            }.also { replCompiler = it }

        val snippetSource = KtFileScriptSource(ktFiles.values.single())

        @Suppress("DEPRECATION_ERROR")
        val result = internalScriptingRunSuspend {
            compiler.compile(snippetSource)
        }

        return ReplCompilationArtifact(snippetSource, result)
    }
}

class ReplCompilerDiagnosticsHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : AnalysisHandler<ReplCompilationArtifact>(testServices, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures) {
    override val artifactKind: TestArtifactKind<ReplCompilationArtifact> = ReplCompilationArtifact.Kind

    private val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler

    override fun processModule(module: TestModule, info: ReplCompilationArtifact) {
        val file = module.files.single()
        val diagnostics = info.compilationResult.reports.filter {
            (it.severity == ScriptDiagnostic.Severity.ERROR || it.severity == ScriptDiagnostic.Severity.WARNING) &&
                    it.sourcePath?.let(::File)?.name == file.name
        }
        if (diagnostics.isEmpty()) return
        globalMetadataInfoHandler.addMetadataInfosForFile(
            file,
            diagnostics.map { ScriptDiagnosticCodeMetaInfo(it, info.snippetSource.text) }
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

internal class ScriptDiagnosticCodeMetaInfo(scriptDiagnostic: ScriptDiagnostic, text: String) : CodeMetaInfo {

    override var start = -1
    override var end = -1

    init {
        val location = scriptDiagnostic.location
        if (location != null) {
            var offset = 0
            var line = 1
            while (true) {
                if (start == -1 && location.start.line == line) start = offset + location.start.col - 1
                if (end == -1 && location.end?.line == line) end = offset + location.end!!.col - 1
                if (start != -1 && (end != -1 || location.end == null)) break
                offset = text.indexOf('\n', offset) + 1
                if (offset <= 0) break
                line++
            }
        }
    }

    override val tag: String = scriptDiagnostic.severity.name

    // relying on the "-Xrender-internal-diagnostic-names" flag
    val message = scriptDiagnostic.message.substringAfter('[').substringBefore(']')

    override val renderConfiguration = RenderConfiguration()
    override val attributes: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    class RenderConfiguration : AbstractCodeMetaInfoRenderConfiguration() {
        override fun asString(codeMetaInfo: CodeMetaInfo): String = (codeMetaInfo as ScriptDiagnosticCodeMetaInfo).message
    }
}