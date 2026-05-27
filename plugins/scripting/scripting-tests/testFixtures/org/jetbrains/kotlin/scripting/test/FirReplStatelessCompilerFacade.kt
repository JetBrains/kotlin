/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.unregisterFinders
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplStatelessCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifact
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeSidecar
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.frontend.fir.FirModuleInfoProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl

/**
 * Stateless-variant of [FirReplCompilerFacade].
 *
 * Drives each REPL snippet through [K2ReplStatelessCompiler] instead of the stateful
 * [org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler]. Across snippets in the
 * same multi-snippet test file, the facade accumulates the produced [SnippetArtifact]s and feeds
 * them as `priorSnippets` to subsequent calls, so each snippet sees a *reconstructed* history that
 * round-tripped through the JSON sidecar + classfile temp-dir indirection.
 *
 * This mirrors the production stateless-REPL flow that an out-of-process JSR-223 host would use,
 * and is the read-side proof for Q10b (history tagging via `ArtifactBackedFirReplHistoryProvider`).
 *
 * Compilation result shape is identical to [FirReplCompilerFacade] ([ReplCompilationArtifact]) —
 * but the per-snippet diagnostics are wrapped: the `compilationResult`'s value is a
 * [LinkedSnippet] of [CompiledSnippet] synthesised on-the-fly from the produced [SnippetArtifact].
 *
 * The stateless artifact itself is not exposed through [ReplCompilationArtifact] — diagnostics
 * tests need only the report stream + the source file. Tests that care about the artifact can read
 * [accumulatedArtifacts] directly via [retrieveAccumulatedArtifacts] (used by the post-run
 * sidecar-shape handler).
 */
class FirReplStatelessCompilerFacade(
    val testServices: TestServices,
) : AbstractTestFacade<ResultingArtifact.Source, ReplCompilationArtifact>() {
    private val testModulesByName by lazy { testServices.moduleStructure.modules.associateBy { it.name } }

    /** Single stateless compiler reused across snippets within one test file (cheap to create). */
    private val statelessCompiler = K2ReplStatelessCompiler()

    /** Per-test-run accumulation of produced [SnippetArtifact]s, in history order. */
    private val accumulatedArtifacts: MutableList<SnippetArtifact> = mutableListOf()

    /** Base compilation configuration — lazily initialised from the first module's directives. */
    private var baseCompilationConfiguration: ScriptCompilationConfiguration? = null

    /**
     * Accessor for in-process inspection by handlers (e.g. asserting sidecar fields after the
     * run). Returns a defensive copy so handlers can't mutate the accumulator mid-run.
     */
    fun retrieveAccumulatedArtifacts(): List<SnippetArtifact> = accumulatedArtifacts.toList()

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
        val snippetSource = KtFileScriptSource(ktFiles.values.single())

        val baseConfig = baseCompilationConfiguration ?: ScriptCompilationConfiguration {
            updateClasspath(
                listOf(testServices.standardLibrariesPathProvider.runtimeJarForTests())
            )
            compilerOptions("-Xrender-internal-diagnostic-names=true")
            for (languageFeature in module.directives[LanguageSettingsDirectives.LANGUAGE]) {
                compilerOptions("-XXLanguage:$languageFeature")
            }
        }.also { baseCompilationConfiguration = it }

        // Snapshot the priors *before* this snippet's compile so the artifact-backed provider sees
        // only N-1 snippets when compiling snippet N.
        val priorsSnapshot = accumulatedArtifacts.toList()

        @Suppress("DEPRECATION_ERROR")
        val artifactResult: ResultWithDiagnostics<SnippetArtifact> = internalScriptingRunSuspend {
            statelessCompiler.compile(
                priorSnippets = priorsSnapshot,
                snippet = snippetSource,
                scriptCompilationConfiguration = baseConfig,
                // Reuse the test infrastructure's project disposable so the stateless compiler does
                // not allocate (and try to dispose) its own — that path violates IntelliJ-platform
                // threading inside a hosted test fixture.
                parentDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
            )
        }

        // On success, append to the accumulator so the next snippet's compile sees this artifact.
        // On failure, intentionally do *not* append — a failed snippet wouldn't have produced
        // usable classfiles + sidecar, and downstream snippets would otherwise be polluted.
        if (artifactResult is ResultWithDiagnostics.Success) {
            accumulatedArtifacts.add(artifactResult.value)
        }

        // Repackage as a ReplCompilationArtifact so the existing ReplCompilerDiagnosticsHandler
        // can consume the report stream without changes. The `LinkedSnippet<CompiledSnippet>`
        // value is a never-dereferenced stub — the diagnostics handler reads only
        // `compilationResult.reports`.
        val repackaged: ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> = when (artifactResult) {
            is ResultWithDiagnostics.Success -> ResultWithDiagnostics.Success(
                value = LinkedSnippetImpl(STATELESS_STUB_COMPILED_SNIPPET, previous = null),
                reports = artifactResult.reports,
            )
            is ResultWithDiagnostics.Failure -> ResultWithDiagnostics.Failure(artifactResult.reports)
        }

        return ReplCompilationArtifact(snippetSource, repackaged)
    }

    companion object {
        /**
         * Stub [CompiledSnippet] supplied on the success path.
         *
         * The stateless prototype produces a [SnippetArtifact] rather than a [CompiledSnippet]; the
         * diagnostics handler reads only `compilationResult.reports`, so this stub is never
         * dereferenced. If a future handler attempts to read [getClass], it gets a clear failure.
         */
        private val STATELESS_STUB_COMPILED_SNIPPET: CompiledSnippet = object : CompiledSnippet {
            override val compilationConfiguration: ScriptCompilationConfiguration =
                ScriptCompilationConfiguration.Default

            override suspend fun getClass(
                scriptEvaluationConfiguration: ScriptEvaluationConfiguration?,
            ): ResultWithDiagnostics<KClass<*>> = ResultWithDiagnostics.Failure(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    "stateless REPL diagnostics facade: CompiledSnippet.getClass is not supported — " +
                            "the real artifact is the SnippetArtifact retained on the facade.",
                )
            )
        }
    }
}

/**
 * Convenience accessor: returns the latest sidecar from the accumulator, decoded. Useful for
 * post-run assertions in dedicated handlers.
 */
@Suppress("unused")
internal fun FirReplStatelessCompilerFacade.latestDecodedSidecar() =
    retrieveAccumulatedArtifacts().lastOrNull()?.decodeSidecar()
