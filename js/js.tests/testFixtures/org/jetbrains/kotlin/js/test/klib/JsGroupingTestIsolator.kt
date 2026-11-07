/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.ast.JsAstDirectives
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.getClassifiedDumpFile
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.IrCheckersDisabledByTestDirectives
import org.jetbrains.kotlin.test.services.IrCheckersEnabledByTestDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.detectPackage
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * Decides which JS box tests can share a grouped batch.
 *
 * A batch is linked into a single executable by one compiler invocation and is run through a launcher
 * that calls every `box()` by its fully qualified name. So a test is isolated whenever it needs a compiler or a runner
 * setup of its own, or declares something that is visible outside its (renamed) package. The rules are deliberately
 * conservative: a wrongly isolated test merely runs the way it always did, while a wrongly grouped one fails and can break its whole batch.
 *
 * @param customCompilerStage the stage compiled by a custom (previously released) compiler in this test runner. Only
 *   the `IGNORE_KLIB_*_WITH_CUSTOM_<STAGE>` directives of that stage expect a failure (see `CustomKlibCompilerFirstStageTestSuppressor`
 *   and `CustomKlibCompilerSecondStageTestSuppressor`), so only they isolate a test: the directives of the other stage are no-ops here.
 *   `null` if both stages are compiled by the current compiler, so none of these directives is in effect.
 * @param isolateMultiModuleTests whether a test of several modules is isolated, so that its modules keep their names
 *   and its main module is linked as the main one, as in the one-stage pipeline. In a grouped batch, the modules of
 *   the tests get names of the batch, and every one of them is linked as a library of the launcher module.
 * @param isolateTestsWithDumps whether a test that dumps its FIR, IR or KLIB ABI is isolated. `BatchingPackageInserter`
 *   puts a grouped test into a package of its own, which the dumps would spell, so they could not match the expected ones.
 * @param isolateTestsWithJsAstChecks whether a test with the checks of its generated JS (see [JsAstDirectives]) is
 *   isolated. `JsAstHandler` runs them on the executable of an isolated test only: on a grouped batch, it validates
 *   the JS AST as a whole, which cannot tell the JS of one test from another.
 * @param additionalIgnoreDirectives the ignore directives the test runner has registered in `BlackBoxCodegenSuppressor`
 *   on top of `IGNORE_BACKEND*`: a test that lists a JS backend in one of them is expected to fail, so it is isolated.
 */
class JsGroupingTestIsolator(
    testServices: TestServices,
    private val customCompilerStage: CompilationStage?,
    private val isolateMultiModuleTests: Boolean = false,
    private val isolateTestsWithDumps: Boolean = false,
    private val isolateTestsWithJsAstChecks: Boolean = false,
    private val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>> = emptyList(),
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOfNotNull(
            ConfigurationDirectives,
            JsEnvironmentConfigurationDirectives,
            CodegenTestDirectives,
            LanguageSettingsDirectives,
            CustomKlibCompilerTestDirectives,
            FirDiagnosticsDirectives,
            KlibAbiDumpDirectives,
            JsAstDirectives.takeIf { isolateTestsWithJsAstChecks },
        )

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        val allDirectives = moduleStructure.allDirectives
        if (ISOLATION_DIRECTIVES.any { it in allDirectives }) return BatchToken.Isolated
        val customCompilerIgnoreDirectives = when (customCompilerStage) {
            CompilationStage.FIRST -> CUSTOM_FIRST_STAGE_IGNORE_DIRECTIVES
            CompilationStage.SECOND -> CUSTOM_SECOND_STAGE_IGNORE_DIRECTIVES
            null -> emptyList()
        }
        if (customCompilerIgnoreDirectives.any { it in allDirectives }) return BatchToken.Isolated
        if ((IGNORE_BACKEND_DIRECTIVES + additionalIgnoreDirectives).any { allDirectives[it].any(::isJsBackend) }) return BatchToken.Isolated

        val modules = moduleStructure.modules
        if (isolateMultiModuleTests && hasSeveralModules(moduleStructure)) return BatchToken.Isolated
        if (isolateTestsWithDumps && hasDumps(moduleStructure)) return BatchToken.Isolated

        val shouldBeIsolated = modules.any { module ->
            module.files.any { file ->
                // The batch launcher cannot supply companion JS files, and a recompiled file needs the incremental pipeline.
                !file.name.endsWith(".kt") || FILE_ISOLATION_DIRECTIVES.any { it in file.directives } || file.directives.hasJsAstChecks()
            }
                    // The friendship can be declared only for the included module, which is the launcher in a grouped batch.
                    || module.allDependencies.any { it.relation == DependencyRelation.FriendDependency }
                    // `JsAstHandler` runs the checks of the generated JS on the executable of an isolated test only.
                    || module.directives.hasJsAstChecks()
        }
        if (shouldBeIsolated) return BatchToken.Isolated

        if (moduleStructure.originalTestDataFiles.any { file ->
                COMPANION_JS_FILE_SUFFIXES.any { suffix -> file.resolveSibling(file.nameWithoutExtension + suffix).exists() }
                        || file.resolveSibling(COMMON_JS_FILE_NAME).exists()
            }
        ) return BatchToken.Isolated

        val testFiles = modules.flatMap { it.files }.filterNot { it.isAdditional }
        if (testFiles.any { detectPackage(it, SourceContentView.ORIGINAL) in PACKAGES_KEPT_BY_PACKAGE_INSERTER }) return BatchToken.Isolated
        if (testFiles.any { it.originalContent.contains(JS_INTEROP_REGEX) }) return BatchToken.Isolated
        if (testFiles.any { it.originalContent.contains(MAIN_MODULE_ONLY_FEATURES_REGEX) }) return BatchToken.Isolated
        if (testFiles.any { it.mentionsOwnPackageInStringLiteral() }) return BatchToken.Isolated
        if (testFiles.any { it.observesJsClassName() }) return BatchToken.Isolated
        // K/JS supports a class implementing a suspend function type only partially, so a test of such a class suppresses
        // the diagnostic saying it. Such a test has been seen to pass alone, but to fail in a batch with minimized names.
        if (testFiles.any { it.originalContent.contains(IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE) }) return BatchToken.Isolated

        // Tests share a batch only if they agree on everything the batch is compiled and linked with.
        val batchSettings = listOfNotNull(
            computeRuntimeSetting(allDirectives),
            computeCompilerSettings(allDirectives),
            computeLanguageSettings(allDirectives),
            computeToggledCheckers(allDirectives),
            computeHelpers(moduleStructure),
        )
        return if (batchSettings.isEmpty()) BatchToken.Regular else BatchToken.Custom(batchSettings.joinToString("; "))
    }

    private fun RegisteredDirectives.hasJsAstChecks(): Boolean = isolateTestsWithJsAstChecks && any { it in JsAstDirectives }

    /**
     * The module structure may be still under construction: the module names are escaped for a grouped test as soon as
     * each module is finished (see `ModuleStructureExtractorImpl`), and the first module of a test of several modules
     * would be escaped too, if only the finished modules were counted. So the modules declared in the test data count as well.
     */
    private fun hasSeveralModules(moduleStructure: TestModuleStructure): Boolean {
        val finishedModuleCount = moduleStructure.modules.count { it.name != JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME }
        return finishedModuleCount > 1 || moduleStructure.originalTestDataFiles.any { file ->
            file.isFile && MODULE_DIRECTIVE_REGEX.findAll(file.readText()).take(2).count() > 1
        }
    }

    /**
     * Whether a handler of the non-grouping stage dumps something of the test to compare it with the expected dump.
     * `FirDumpHandler` does it not only by the directive, but also whenever the expected dump exists.
     */
    private fun hasDumps(moduleStructure: TestModuleStructure): Boolean =
        DUMP_DIRECTIVES.any { it in moduleStructure.allDirectives } || moduleStructure.getClassifiedDumpFile(FIR_DUMP_EXTENSION).exists()

    /**
     * A batch links the helpers KLIB of one of its tests only (see `JsInProcessSecondStageFacade`), so all its tests
     * must have got the very same helpers.
     */
    private fun computeHelpers(moduleStructure: TestModuleStructure): String? {
        val helpersModule = moduleStructure.modules.find { it.name == JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME } ?: return null
        val helpers = helpersModule.files.map { "${it.name}#${it.originalContent.hashCode().toHexString()}" }.sorted()
        return "Helpers: $helpers"
    }

    /**
     * A test that spells its own package in a string literal most likely compares it with a qualified name the compiler
     * has generated, like the one in `toString()` of an annotation instance. The JS backend knows nothing about
     * `kotlin.internal.ReflectionPackageName`, so such a name would carry the package the batch has put the test into.
     */
    private fun TestFile.mentionsOwnPackageInStringLiteral(): Boolean {
        val packageName = detectPackage(this, SourceContentView.ORIGINAL) ?: return false
        return originalContent.contains(Regex("\"[^\"\\n]*\\b${Regex.escape(packageName)}\\."))
    }

    /**
     * The JS name of a class is not a stable thing in a batch: the backend renames one of two same-named classes of
     * different tests linked into one program, to `Foo_0`. The name is observable through `KClass.js` and, as the
     * runtime sets `Throwable.name` from the JS constructor, through `toString()` of an exception. Whether a test
     * observes the name of its exception class cannot be told from its source reliably, and the outcome would depend
     * on the other tests of the batch, so every test declaring one is isolated: there are few of them.
     */
    private fun TestFile.observesJsClassName(): Boolean =
        originalContent.contains(JS_CLASS_NAME_REGEX) || originalContent.contains(THROWABLE_SUBCLASS_REGEX)

    /**
     * The runtime KLIBs the batch is compiled and linked with, which are chosen by the directives of one of its tests
     * (see `JsEnvironmentConfigurator.isFullJsRuntimeNeeded`). Either directive asks for the full runtime, so the tests
     * asking for it by different directives still share a batch.
     */
    private fun computeRuntimeSetting(directives: RegisteredDirectives): String? =
        "Full runtime".takeIf {
            ConfigurationDirectives.WITH_STDLIB in directives || JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME in directives
        }

    /**
     * The settings the whole batch is linked with, which are taken from one of its tests. They do not isolate a test:
     * a test runner may set any of them for all its tests, and such tests are still to be grouped with each other.
     */
    private fun computeCompilerSettings(directives: RegisteredDirectives): String? =
        BATCH_SETTING_DIRECTIVES.filter { it in directives }.ifNotEmpty {
            "Compiler settings: " + joinToString { directive ->
                when (directive) {
                    is SimpleDirective -> directive.name
                    is StringDirective -> "${directive.name}=${directives[directive].distinct()}"
                    is ValueDirective<*> -> "${directive.name}=${directives[directive].distinct()}"
                }
            }
        }

    private fun computeLanguageSettings(directives: RegisteredDirectives): String? {
        // `disctinct()` below is used to match directive lists for tests having different amount of modules, like `[+X]` vs `[+X, +X]`
        val languageFeatures = directives[LanguageSettingsDirectives.LANGUAGE].distinct().sorted()
        val optIns = directives[LanguageSettingsDirectives.OPT_IN].distinct().sorted()
        val apiVersion = directives[LanguageSettingsDirectives.API_VERSION].distinct()
        val languageVersion = directives[LanguageSettingsDirectives.LANGUAGE_VERSION].distinct()
        val returnValueCheckerMode = directives[LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE].distinct()
        val progressiveMode = LanguageSettingsDirectives.PROGRESSIVE_MODE in directives

        if (languageFeatures.isEmpty()
            && optIns.isEmpty()
            && apiVersion.isEmpty()
            && languageVersion.isEmpty()
            && returnValueCheckerMode.isEmpty()
            && !progressiveMode
        ) {
            return null
        }

        return "Lang settings: $languageFeatures, $optIns, $apiVersion, $languageVersion, $returnValueCheckerMode, progressive=$progressiveMode"
    }

    private fun computeToggledCheckers(directives: RegisteredDirectives): String? {
        val enabledCheckers = IrCheckersEnabledByTestDirectives.filter { it.key in directives }.values
        val disabledCheckers = IrCheckersDisabledByTestDirectives.filter { entry ->
            directives[entry.key].any { it == TargetBackend.ANY || it.isTransitivelyCompatibleWith(TargetBackend.JS_IR) }
        }.values
        return (enabledCheckers + disabledCheckers).toSortedSet().ifNotEmpty { "Toggled checkers: $this" }
    }

    companion object {
        /**
         * Expect a failure, as the `IGNORE_KLIB_*` directives do, but on JS only if they list a JS backend or `ANY`:
         * a test ignored on other backends only is grouped.
         */
        private val IGNORE_BACKEND_DIRECTIVES: List<ValueDirective<TargetBackend>> = listOf(
            CodegenTestDirectives.IGNORE_BACKEND,
            CodegenTestDirectives.IGNORE_BACKEND_K2,
        )

        private fun isJsBackend(backend: TargetBackend): Boolean =
            backend == TargetBackend.ANY || backend.isTransitivelyCompatibleWith(TargetBackend.JS_IR)

        // Some test failures can bring down an entire batch, so where a failure is expected, the test runs in isolation.
        private val CUSTOM_FIRST_STAGE_IGNORE_DIRECTIVES: List<Directive> = listOf(
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE,
        )

        private val CUSTOM_SECOND_STAGE_IGNORE_DIRECTIVES: List<Directive> = listOf(
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE,
        )

        private val ISOLATION_DIRECTIVES: List<Directive> = listOf(
            // Which module is the main one, and how it is loaded and called.
            JsEnvironmentConfigurationDirectives.JS_MODULE_KIND,
            JsEnvironmentConfigurationDirectives.NO_JS_MODULE_SYSTEM,
            JsEnvironmentConfigurationDirectives.ES_MODULES,
            JsEnvironmentConfigurationDirectives.INFER_MAIN_MODULE,
            JsEnvironmentConfigurationDirectives.RUN_PLAIN_BOX_FUNCTION,
            JsEnvironmentConfigurationDirectives.CALL_MAIN,
            JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE,
            JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE,
            JsEnvironmentConfigurationDirectives.NO_COMMON_FILES,
            // The translation modes and the tools the executable of the test is produced with.
            JsEnvironmentConfigurationDirectives.SPLIT_PER_MODULE,
            JsEnvironmentConfigurationDirectives.SPLIT_PER_FILE,
            JsEnvironmentConfigurationDirectives.DELEGATE_JS_TRANSPILATION,
            // The declarations it names are in the packages the batch would rename.
            JsEnvironmentConfigurationDirectives.KEEP,
            JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP,
            JsEnvironmentConfigurationDirectives.SOURCE_MAP_EMBED_SOURCES,
            JsEnvironmentConfigurationDirectives.TS_COMPILATION_STRATEGY,
            JsEnvironmentConfigurationDirectives.GENERATE_DTS_FROM_IR,
            JsEnvironmentConfigurationDirectives.SKIP_IR_INCREMENTAL_CHECKS,
            // What is expected of the executable of this very test. A grouped batch is checked by running `box()` and by
            // the checks of the executable as a whole only, so the expectation is not checked, but it could not hold for
            // the executable of a batch anyway.
            JsEnvironmentConfigurationDirectives.CHECK_OPTIMIZED_JS,
            JsEnvironmentConfigurationDirectives.JS_DCE_EXPECTED_OUTPUT_SIZE,
        )

        private val BATCH_SETTING_DIRECTIVES: List<Directive> = listOf(
            JsEnvironmentConfigurationDirectives.ES6_MODE,
            JsEnvironmentConfigurationDirectives.DISABLE_ES6_ARROWS,
            JsEnvironmentConfigurationDirectives.ONLY_IR_DCE,
            JsEnvironmentConfigurationDirectives.PROPERTY_LAZY_INITIALIZATION,
            JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS,
            JsEnvironmentConfigurationDirectives.GENERATE_STRICT_IMPLICIT_EXPORT,
            JsEnvironmentConfigurationDirectives.EXPORT_WITH_UNKNOWN_TYPE_INSTEAD_ANY,
            JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN,
            JsEnvironmentConfigurationDirectives.SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC,
            JsEnvironmentConfigurationDirectives.ENABLE_UNUSED_PROPERTY_DCE,
            JsEnvironmentConfigurationDirectives.JS_DROP_REGION_COMMENTS,
        )

        private val FILE_ISOLATION_DIRECTIVES: List<Directive> = listOf(
            JsEnvironmentConfigurationDirectives.RECOMPILE,
            JsEnvironmentConfigurationDirectives.ENTRY_ES_MODULE,
        )

        /** The directives that make the handlers of the non-grouping stage dump the FIR, IR or KLIB ABI of the test. */
        private val DUMP_DIRECTIVES: List<Directive> = listOf(
            CodegenTestDirectives.DUMP_IR,
            CodegenTestDirectives.DUMP_KT_IR,
            CodegenTestDirectives.DUMP_IR_AFTER_INLINE,
            CodegenTestDirectives.DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS,
            CodegenTestDirectives.DUMP_SOURCE_RANGES_IR,
            FirDiagnosticsDirectives.FIR_DUMP,
            FirDiagnosticsDirectives.DUMP_CFG,
            KlibAbiDumpDirectives.DUMP_KLIB_ABI,
        )

        private const val FIR_DUMP_EXTENSION = ".fir.txt"

        /** The companion JS files of a test, and the `_common.js` of its directory (see `JsAdditionalSourceProvider`). */
        private val COMPANION_JS_FILE_SUFFIXES = listOf(".js", ".mjs", "__main.js", "__main.mjs")
        private const val COMMON_JS_FILE_NAME = "_common.js"

        /** `BatchingPackageInserter` keeps these packages as they are, so tests declaring something there would clash in a batch. */
        private val PACKAGES_KEPT_BY_PACKAGE_INSERTER = setOf("kotlin", "kotlin.internal", "helpers")

        /**
         * What the backend generates properly only for the module it links as the main one, which is the launcher in
         * a grouped batch: a top-level `main` function, and the associated objects of a class.
         */
        private val MAIN_MODULE_ONLY_FEATURES_REGEX = Regex("""(?m)^\s*(?:suspend\s+)?fun\s+main\s*\(|\bAssociatedObjectKey\b|\bfindAssociatedObject\b""")

        /**
         * Declarations and code that reach outside the package of the test: exported or explicitly named declarations
         * share the namespace of the executable, and external ones or `js()` code depend on the JS environment of the test.
         * Any `external` declaration counts, whatever modifiers precede the keyword it introduces: the batch has no JS
         * environment to supply what the declaration refers to.
         */
        private val JS_INTEROP_REGEX = Regex("""@(?:file:)?Js[A-Z]\w*|\bexternal\b|\bjs\s*\(|\bdynamic\b""")

        private val JS_CLASS_NAME_REGEX = Regex("""::class\.js\b|\.jsClass\b""")

        private const val IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE = "IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE"

        private val MODULE_DIRECTIVE_REGEX = Regex("""(?m)^\s*//\s*MODULE\s*:""")

        /** A class or an object whose supertype list starts with a call of a `Throwable` subclass constructor. */
        private val THROWABLE_SUBCLASS_REGEX = Regex("""\b(?:class|object)\s+[A-Z]\w*\b[^{\n]*?:\s*[\w.]*(?:Exception|Error|Throwable)\s*\(""")
    }
}
