/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.grouping

import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken.Custom
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.Companion.sourceContains
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

/**
 * Decides which K/JS box tests may share a single stage-2 link + run, and which have to be compiled and run on their own.
 *
 * A grouped batch is linked as *one* JS module: the per-test KLIBs become plain `-libraries` of a synthesized launcher
 * KLIB that is passed as `-Xinclude` (see [org.jetbrains.kotlin.js.test.converters.JsGroupingSecondStageFacade]), and
 * the resulting JS files are loaded into one V8 realm, where each test is then invoked through its own exported launcher
 * function. Everything that this single link/run cannot express per test — a differing module system, a custom JS entry
 * point, an expected failure that would abort the shared link — must therefore be isolated.
 *
 * [BatchToken.Custom] is used where tests only need to be *separated* rather than run alone: tests carrying the same
 * token still batch together, which keeps e.g. all `WITH_STDLIB` tests in one batch instead of one batch each.
 */
class JsGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(
            JsEnvironmentConfigurationDirectives,
            JvmEnvironmentConfigurationDirectives, // for directive WITH_REFLECT
            CodegenTestDirectives,
            FirDiagnosticsDirectives,
            KlibAbiDumpDirectives,
            LanguageSettingsDirectives,
        )

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        if (isolationDirectives.any { it in moduleStructure.allDirectives }) return BatchToken.Isolated
        if (dumpDirectives.any { it in moduleStructure.allDirectives }) return BatchToken.Isolated

        // `allDirectives` composes the *module* directives, so file-applicable ones have to be looked up on the files.
        if (moduleStructure.modules.any { module ->
                module.files.any { file -> fileLevelIsolationDirectives.any { it in file.directives } }
            }
        ) {
            return BatchToken.Isolated
        }

        if (moduleStructure.modules.any { module ->
                // A test with a non-Kotlin file of its own (`// FILE: foo.js`, `// FILE: foo.mjs`) is driven by that
                // file rather than by a plain `box()` call, and its JS is loaded around the module under test.
                module.files.any { !it.name.endsWith(".kt") }
                        // Friend visibility between two modules of the same test survives only for the `-Xinclude`d main
                        // module: there is no way to declare friendship between two `-libraries`. In a grouped batch every
                        // per-test KLIB is an ordinary library, so `internal` declarations crossing the test's own module
                        // boundary would stop resolving. Isolation routes such a test through the per-test link, which
                        // keeps its own friend dependencies.
                        || module.allDependencies.any { it.relation == DependencyRelation.FriendDependency }
            }
        ) {
            return BatchToken.Isolated
        }

        if (sourceLevelIsolationReasons.any { moduleStructure.sourceContains(it) }) return BatchToken.Isolated

        // Companion JS/TS files on disk next to the testdata are loaded before/after the module under test, and
        // `__main` files replace the entry point altogether — neither is expressible for one test of a shared batch.
        val hasCompanionFile = moduleStructure.originalTestDataFiles.any { file ->
            val parent = file.parentFile
            val base = file.nameWithoutExtension
            companionFileSuffixes.any { suffix -> parent.resolve(base + suffix).exists() }
        }
        if (hasCompanionFile) return BatchToken.Isolated

        val specificTokens = listOfNotNull(
            computeRuntimeToken(moduleStructure),
            computeLanguageSettingsToken(moduleStructure),
        )
        return when (specificTokens.size) {
            0 -> BatchToken.Regular
            1 -> specificTokens.single()
            else -> Custom(specificTokens.joinToString(separator = " & ") { (it as Custom).name })
        }
    }

    /**
     * Separates tests needing the full JS stdlib (plus `kotlin-test`) from the ones linked against the reduced one.
     *
     * Both KLIBs declare `unique_name = "kotlin"`, so a batch mixing them would fail to link; and linking everything
     * against the full stdlib instead would silently give the reduced-stdlib tests declarations they are meant not to
     * see. Tests that agree on the runtime still share a batch.
     */
    private fun computeRuntimeToken(moduleStructure: TestModuleStructure): BatchToken? =
        Custom("full JS runtime").takeIf {
            moduleStructure.modules.any { module -> JsEnvironmentConfigurator.isFullJsRuntimeNeeded(module) }
        }

    /**
     * Separates tests compiled under different language settings, which the batch has to apply uniformly.
     *
     * Mirrors `WasmGroupingTestIsolator.computeLanguageSettingsToken`: tests with the very same settings stay in one
     * batch, so a widely used `// LANGUAGE:` feature does not degenerate into one batch per test.
     */
    private fun computeLanguageSettingsToken(moduleStructure: TestModuleStructure): BatchToken? {
        val languageFeatures = moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE].sorted()
        val optIns = moduleStructure.allDirectives[LanguageSettingsDirectives.OPT_IN].sorted()
        val apiVersion = moduleStructure.allDirectives[LanguageSettingsDirectives.API_VERSION]
        val languageVersion = moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE_VERSION]
        val returnValueCheckerMode = moduleStructure.allDirectives[LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE]
        val progressiveMode = LanguageSettingsDirectives.PROGRESSIVE_MODE in moduleStructure.allDirectives

        if (languageFeatures.isEmpty()
            && optIns.isEmpty()
            && apiVersion.isEmpty()
            && languageVersion.isEmpty()
            && returnValueCheckerMode.isEmpty()
            && !progressiveMode
        ) {
            return null
        }

        return Custom(
            "Lang settings: $languageFeatures, $optIns, $apiVersion, $languageVersion, $returnValueCheckerMode, progressive=$progressiveMode"
        )
    }

    companion object {
        private val isolationDirectives = listOf(
            // The test opted out of grouping explicitly, most often because it observes its own fully qualified names
            // at runtime and grouping renames the packages of the tests sharing a compilation.
            JsEnvironmentConfigurationDirectives.JS_STANDALONE,
            // An expected failure can take the whole shared link or the whole V8 realm down, so where one is expected
            // the test has to run on its own for the suppressors to see it in isolation.
            CodegenTestDirectives.IGNORE_BACKEND,
            CodegenTestDirectives.IGNORE_BACKEND_K2,
            CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE,
            CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE,
            CodegenTestDirectives.IGNORE_HMPP,
            // The module system decides how the linked JS is loaded and how the test's entry point is reached, and a
            // batch is loaded and reached in exactly one way.
            JsEnvironmentConfigurationDirectives.JS_MODULE_KIND,
            JsEnvironmentConfigurationDirectives.ES_MODULES,
            JsEnvironmentConfigurationDirectives.ES6_MODE,
            JsEnvironmentConfigurationDirectives.NO_JS_MODULE_SYSTEM,
            JsEnvironmentConfigurationDirectives.DELEGATE_JS_TRANSPILATION,
            // The entry point is not the launcher's `box()` call, so the batch cannot report a result for the test.
            JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION,
            JsEnvironmentConfigurationDirectives.CALL_MAIN,
            JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE,
            // Global settings of the stage-2 link: which translation modes are produced, what DCE keeps, and which
            // extra artifacts are generated. All of these are properties of the batch, not of a single test in it.
            JsEnvironmentConfigurationDirectives.SPLIT_PER_MODULE,
            JsEnvironmentConfigurationDirectives.SPLIT_PER_FILE,
            JsEnvironmentConfigurationDirectives.ONLY_IR_DCE,
            JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE,
            JsEnvironmentConfigurationDirectives.SKIP_NODE_JS,
            JsEnvironmentConfigurationDirectives.KEEP,
            JsEnvironmentConfigurationDirectives.JS_DCE_EXPECTED_OUTPUT_SIZE,
            JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP,
            JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER,
            JsEnvironmentConfigurationDirectives.GENERATE_DTS_FROM_IR,
            JsEnvironmentConfigurationDirectives.TS_COMPILATION_STRATEGY,
            JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS,
            JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION,
            JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN,
            JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC,
            JsEnvironmentConfigurationDirectives.EXPORT_WITH_UNKNOWN_TYPE_INSTEAD_ANY,
            JsEnvironmentConfigurationDirectives.GENERATE_STRICT_IMPLICIT_EXPORT,
            JsEnvironmentConfigurationDirectives.CHECK_OPTIMIZED_JS,
            // Incremental compilation recompiles one module of one test and compares the result, which has no meaning
            // for a batch linked as a whole.
            JsEnvironmentConfigurationDirectives.RUN_IC,
            JsEnvironmentConfigurationDirectives.KLIB_MAIN_MODULE,
            JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE,
            // Suppresses the `_common.kt` helpers the other tests of the batch may well rely on.
            JsEnvironmentConfigurationDirectives.NO_COMMON_FILES,
            JvmEnvironmentConfigurationDirectives.WITH_REFLECT,
        )

        /**
         * Directives enabling a dump that is compared against a testdata file.
         *
         * Grouping renames every test's packages (`BatchingPackageInserter`) so that the batch can be linked as one
         * module, and any such dump spells those packages out — so a grouped test would compare its dump against a
         * testdata file written for the original package. Isolation is what makes the renaming not happen for the test,
         * which keeps the dump comparable; the alternative, dropping the dump handlers from the grouped configuration,
         * would silently lose the coverage instead.
         */
        private val dumpDirectives = listOf(
            FirDiagnosticsDirectives.FIR_DUMP,
            FirDiagnosticsDirectives.DUMP_CFG,
            FirDiagnosticsDirectives.DUMP_VFIR,
            FirDiagnosticsDirectives.SCOPE_DUMP,
            FirDiagnosticsDirectives.DUMP_INFERENCE_LOGS,
            CodegenTestDirectives.DUMP_IR,
            CodegenTestDirectives.DUMP_KT_IR,
            CodegenTestDirectives.DUMP_IR_AFTER_INLINE,
            CodegenTestDirectives.DUMP_IR_AFTER_SPLITTING,
            CodegenTestDirectives.DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS,
            CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES,
            CodegenTestDirectives.DUMP_SOURCE_RANGES_IR,
            CodegenTestDirectives.DUMP_EXTERNAL_CLASS,
            KlibAbiDumpDirectives.DUMP_KLIB_ABI,
            // The `*_DIFFERENCE` ones compare the dump of one target backend against another's, so they pull in the
            // very same dumps and need the very same isolation.
            CodegenTestDirectives.DUMP_IR_DIFFERENCE,
            CodegenTestDirectives.DUMP_IR_AFTER_INLINE_DIFFERENCE,
            CodegenTestDirectives.DUMP_IR_AFTER_SPLITTING_DIFFERENCE,
        )

        /** Isolation reasons carried by a `// FILE:` rather than by a module, so absent from `allDirectives`. */
        private val fileLevelIsolationDirectives = listOf(
            // Recompiles one file of one test and compares the result, which has no meaning for a batch linked as a whole.
            JsEnvironmentConfigurationDirectives.RECOMPILE,
            // Replaces the entry point of the whole run with that ES module.
            JsEnvironmentConfigurationDirectives.ENTRY_ES_MODULE,
        )

        private val companionFileSuffixes = listOf(
            ".js", ".mjs", "__main.js", "__main.mjs", ".d.ts",
        )

        /**
         * Isolation reasons that only the test's sources reveal, no directive marking them.
         *
         * Both are about being *the* main module of the link, which exactly one module of a batch can be — and in a
         * grouped batch that one is the synthesized launcher, never a test.
         */
        private val sourceLevelIsolationReasons = listOf(
            // A top-level `main` is the module's entry point. Nothing in a batch references the `main` of a test, so DCE
            // drops it while its fragment still declares itself as having one, and the link then fails with
            // "Expect to have name binding for tag ...<test package>/main" — taking the whole batch down with it.
            Regex("""(^|\s)(suspend\s+)?fun\s+main\s*\("""),
            // `js("_")` reaches for the enclosing module's own scope object to read a declaration off it. In a batch
            // that object belongs to the launcher, so the test's exported declaration is not on it.
            Regex("""js\(\s*"_"\s*\)"""),
        )
    }
}
