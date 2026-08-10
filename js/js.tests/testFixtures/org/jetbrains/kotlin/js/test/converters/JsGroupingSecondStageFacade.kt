/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.additionalExportedDeclarationNames
import org.jetbrains.kotlin.js.config.artifactConfigurations
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.grouping.computeProxyLauncherName
import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getKlibDependencies
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError
import java.io.File

/** The per-test KLIB produced by the non-grouping stage, together with everything its link needs. */
data class PerTestJsOutput(
    val testServices: TestServices,
    /** The module whose KLIB is the test's entry point — the one the ungrouped pipeline would `-Xinclude`. */
    val mainModule: TestModule,
    val klib: BinaryArtifacts.KLib,
    /** Runtime KLIBs and the transitive KLIBs of the test's own auxiliary modules. */
    val regularDependencies: Set<String>,
    val friendDependencies: Set<String>,
)

/**
 * The K/JS grouping-stage (stage-2) facade: turns the per-test KLIBs of a batch into a single [BinaryArtifacts.Js].
 *
 * A batch of several tests is linked as **one** JS module. A `ProxyBatchLauncher.kt` is synthesized with one
 * `@JsExport`ed launcher function per test, calling that test's `box()` by its (package-renamed) FQN; it is compiled
 * into a small `launcher.klib` and passed as `-Xinclude`, with every per-test KLIB as an ordinary `-library`. The
 * launcher is what makes the tests reachable at all — libraries contribute only what something references — and its
 * exports are what both DCE keeps and [org.jetbrains.kotlin.js.test.handlers.JsGroupingStageBoxRunner] calls to run and
 * attribute each test individually.
 *
 * A batch of a single test is instead linked exactly the way the ungrouped pipeline links it: that test's own KLIB is
 * the `-Xinclude`d main module and its `box()` is reached through the ordinary `additionalExportedDeclarationNames`
 * export. That keeps everything an isolated test may rely on — its own module names, friend dependencies, companion
 * JS files and DCE roots — unchanged, which is the whole point of isolating it.
 */
class JsGroupingSecondStageFacade(
    val testServices: TestServices,
) : AbstractGroupingStageTestFacade<GroupingStageInputArtifact, BinaryArtifacts.Js>() {
    override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
        get() = GroupingStageInputArtifact.Kind

    override val outputKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Js? {
        val outputs = inputArtifact.nonGroupingStageOutputs.map(::perTestOutput)
        return if (outputs.size == 1) {
            linkSingleTest(outputs.single())
        } else {
            linkGroupedBatch(outputs)
        }
    }

    /**
     * Describes what linking the test behind [output] takes.
     *
     * Every test of the batch is described, none filtered out — not even one whose failures are ignored via
     * `IGNORE_BACKEND`. Such a test is expected to fail *somewhere*, so it still has to be compiled and run for its
     * suppressor to see that failure, and to report the test as unmutable when it does not happen. (It is also always
     * isolated, so it never affects the batch of its neighbours.) A test that failed at stage 1 does not reach this
     * point at all: the engine only runs the grouping stage for the tests whose non-grouping stage succeeded.
     */
    private fun perTestOutput(output: NonGroupingStageOutput): PerTestJsOutput {
        val services = output.testServices
        val mainModule = JsEnvironmentConfigurator.getMainModule(services)
        return PerTestJsOutput(
            testServices = services,
            mainModule = mainModule,
            klib = services.artifactsProvider.getArtifact(mainModule, ArtifactKinds.KLib),
            regularDependencies = buildSet {
                addAll(JsEnvironmentConfigurator.getRuntimePathsForModule(mainModule, services))
                getKlibDependencies(mainModule, services, DependencyRelation.RegularDependency).mapTo(this) { it.absolutePath }
            },
            friendDependencies = getKlibDependencies(mainModule, services, DependencyRelation.FriendDependency)
                .mapTo(mutableSetOf()) { it.absolutePath },
        )
    }

    /**
     * Links the one test of a single-test batch, leaving the compiler configuration exactly as
     * [org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator] built it for that test.
     */
    private fun linkSingleTest(output: PerTestJsOutput): BinaryArtifacts.Js {
        val services = output.testServices
        return deserializeAndLower(services, output.mainModule, output.klib)
            ?: testInfraError("JsGroupingSecondStageFacade: linking the isolated test ${services.testInfo} produced no JS artifact")
    }

    /**
     * Runs the ordinary stage-2 backend facade of the non-grouped pipeline on [module].
     *
     * That facade is instantiated here rather than declared as a step, so the services it declares have to be
     * registered by hand — and into [services], the per-test services it will consult, not into the batch-level ones a
     * grouping-stage step would register them in. They are taken from the facade itself so that a new requirement of it
     * cannot be forgotten here.
     */
    private fun deserializeAndLower(services: TestServices, module: TestModule, klib: BinaryArtifacts.KLib): BinaryArtifacts.Js? {
        val facade = JsUnifiedIrDeserializerAndLoweringFacade(services)
        services.register(facade.additionalServices, skipAlreadyRegistered = true)
        return facade.transform(module, klib)
    }

    private fun linkGroupedBatch(outputs: List<PerTestJsOutput>): BinaryArtifacts.Js {
        val someServices = outputs.first().testServices
        val someModule = outputs.first().mainModule
        val tempDir = someServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")

        val launcherFile = generateGroupedBatchLauncherSource(outputs, someModule, tempDir)
        val launcherModule = someModule.copy(files = listOf(launcherFile))

        val perTestKlibPaths = outputs.map { it.klib.outputFile.absolutePath }
        val regularDependencies = outputs.flatMapTo(mutableSetOf()) { it.regularDependencies }
        val friendDependencies = outputs.flatMapTo(mutableSetOf()) { it.friendDependencies }

        // Step 1: compile the launcher alone into a small KLIB, against the whole batch as libraries.
        val launcherKlibFile = tempDir.resolve("launcher.klib")
        JsFirstStageInvoker(someServices).compileSourcesToKlib(
            module = launcherModule,
            sources = listOf(launcherFile.originalFile),
            klibOutputFile = launcherKlibFile,
            // The batch is compiled with the highest language version any of its tests asks for, and with the union of
            // their opt-ins: settings that differ beyond that put the tests into separate batches (see
            // `JsGroupingTestIsolator.computeLanguageSettingsToken`).
            languageVersion = outputs.maxOf { it.mainModule.languageVersionSettings.languageVersion },
            customOptIns = outputs.flatMap { it.mainModule.directives[LanguageSettingsDirectives.OPT_IN] }.distinct(),
            allowKotlinPackage = outputs.any { LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in it.mainModule.directives },
            regularDependencies = regularDependencies + perTestKlibPaths,
            friendDependencies = friendDependencies,
        )

        // Step 2: link and lower the whole batch in-process, with the launcher as the main module.
        val configuration = someServices.compilerConfigurationProvider.getCompilerConfiguration(launcherModule, CompilationStage.SECOND)
        configuration.includes = launcherKlibFile.absolutePath
        configuration.friendLibraries = friendDependencies.toList()
        configuration.libraries =
            (regularDependencies + friendDependencies + perTestKlibPaths + launcherKlibFile.absolutePath).toList()
        // The launcher exports every test's entry point with `@JsExport`, which already makes the boxes both reachable
        // and DCE roots. The per-test `box` FQN the configurator picked belongs to whichever test happened to come
        // first, so keeping it would export one arbitrary test's `box` and nothing else.
        configuration.additionalExportedDeclarationNames = emptySet()
        // A batch is only ever linked as a whole program. The per-module and per-file granularities split the output
        // into one JS file per module, which then have to be loaded in dependency order — an order derived from the
        // test's own module structure, and a batch's module structure is the synthetic one built here. What those
        // granularities actually exercise is the compiler's output splitting, not the tests, and the tests that do
        // exercise it carry `SPLIT_PER_MODULE`/`SPLIT_PER_FILE` and are isolated for that reason.
        configuration.artifactConfigurations = configuration.artifactConfigurations.filter {
            it.granularity == JsGenerationGranularity.WHOLE_PROGRAM
        }

        val launcherKlibArtifact = BinaryArtifacts.KLib(launcherKlibFile, DiagnosticsCollectorStub())
        return withTemporarySingleModuleStructure(someServices, launcherModule) {
            deserializeAndLower(someServices, launcherModule, launcherKlibArtifact)
        } ?: testInfraError("JsGroupingSecondStageFacade: linking the grouped batch produced no JS artifact")
    }

    /**
     * Writes `ProxyBatchLauncher.kt` into [tempDir] — one `@JsExport`ed `ProxyLauncher_<hash>()` per test of the batch —
     * and returns it as an additional [TestFile] so that it can be handed to the compiler.
     *
     * Each launcher asserts `box() == "OK"` itself and returns [BATCH_TEST_PASSED_MARKER] on success. Doing the
     * comparison inside the launcher rather than on the JVM side is what makes the verdict independent of whatever the
     * test body wrote to stdout: the V8 REPL returns an expression's value together with everything printed while
     * evaluating it, so a `print` without a trailing newline in one test would otherwise be read as part of the next
     * test's result. A failure surfaces as a JS exception instead, which the REPL reports on stderr.
     */
    private fun generateGroupedBatchLauncherSource(
        outputs: List<PerTestJsOutput>,
        someModule: TestModule,
        tempDir: File,
    ): TestFile {
        val content = buildString {
            for (output in outputs) {
                val services = output.testServices
                val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                val fileWithBox = services.moduleStructure.modules.asReversed().firstNotNullOfOrNull { module ->
                    module.files.firstOrNull { file ->
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(
                            services.sourceFileProvider.getContentOfSourceFile(file)
                        )
                    }
                } ?: testInfraError("No file with box() function found in any module of the test ${services.testInfo}")

                val originalPackage = MainFunctionForBlackBoxTestsSourceProvider.detectPackage(fileWithBox)
                val boxFqName = if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"

                appendLine("@JsExport")
                appendLine("fun ${computeProxyLauncherName(services.testInfo)}(): String {")
                appendLine("    val result = $boxFqName()")
                appendLine(
                    "    if (result != \"OK\") throw AssertionError(" +
                            "\"Test failed with: \" + result + \". Expected <OK>, actual <\" + result + \">.\")"
                )
                appendLine("    return \"$BATCH_TEST_PASSED_MARKER\"")
                appendLine("}")
                appendLine()
            }
        }

        val launcherFile = tempDir.resolve(LAUNCHER_FILE_NAME).also { it.writeText(content) }
        return TestFile(
            LAUNCHER_FILE_NAME,
            content,
            launcherFile,
            /* startLineNumberInOriginalFile = */ 0,
            /* isAdditional = */ true,
            someModule.files.first().directives,
        )
    }

    /**
     * Runs [action] with [module] as the only module of [services]' module structure.
     *
     * The stage-2 facades resolve the main module through `moduleStructure` (`JsIrLoweringFacade` even requires its
     * input to *be* the main module), so the synthetic launcher has to be visible there — and only there, since the
     * per-test services keep being used for everything else, the batch runner included.
     */
    private inline fun <T> withTemporarySingleModuleStructure(
        services: TestServices,
        module: TestModule,
        action: () -> T,
    ): T {
        val originalModuleStructure = services.moduleStructure
        val temporaryModuleStructure = object : TestModuleStructure() {
            override val modules: List<TestModule> = listOf(module)
            override val allDirectives = originalModuleStructure.allDirectives
            override val originalTestDataFiles: List<File> = originalModuleStructure.originalTestDataFiles
        }

        services.register(TestModuleStructure::class, temporaryModuleStructure)
        return try {
            action()
        } finally {
            services.register(TestModuleStructure::class, originalModuleStructure)
        }
    }

    companion object {
        private const val LAUNCHER_FILE_NAME = "ProxyBatchLauncher.kt"

        /**
         * What a grouped test's launcher returns when its `box()` returned `"OK"`.
         *
         * Deliberately improbable rather than just `"OK"`: the JVM side looks for it *anywhere* in what the V8 REPL
         * echoed for that launcher call, so that unrelated output printed by the test body can neither hide a pass nor
         * fake one.
         */
        const val BATCH_TEST_PASSED_MARKER: String = "##KGTI_JS_PASSED##"
    }
}
