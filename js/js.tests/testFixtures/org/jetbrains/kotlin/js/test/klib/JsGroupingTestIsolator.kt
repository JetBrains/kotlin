/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
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
 * A batch is linked into a single whole-program executable by one compiler invocation and is run through a launcher
 * that calls every `box()` by its fully qualified name. So a test is isolated whenever it needs a compiler or a runner
 * setup of its own, or declares something that is visible outside its (renamed) package. The rules are deliberately
 * conservative: a wrongly isolated test merely runs the way it always did, while a wrongly grouped one fails and can break its whole batch.
 */
class JsGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(
            ConfigurationDirectives,
            JsEnvironmentConfigurationDirectives,
            CodegenTestDirectives,
            LanguageSettingsDirectives,
            CustomKlibCompilerTestDirectives,
        )

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        val allDirectives = moduleStructure.allDirectives
        if (ISOLATION_DIRECTIVES.any { it in allDirectives }) return BatchToken.Isolated

        val modules = moduleStructure.modules
        val shouldBeIsolated = modules.any { module ->
            module.files.any { file ->
                // The batch launcher cannot supply companion JS files, and a recompiled file needs the incremental pipeline.
                !file.name.endsWith(".kt") || FILE_ISOLATION_DIRECTIVES.any { it in file.directives }
            }
                    // The friendship can be declared only for the included module, which is the launcher in a grouped batch.
                    || module.allDependencies.any { it.relation == DependencyRelation.FriendDependency }
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
     * runtime sets `Throwable.name` from the JS constructor, through `toString()` of an exception: a test that
     * declares an exception class and spells its name in a string literal most likely compares them.
     */
    private fun TestFile.observesJsClassName(): Boolean {
        if (originalContent.contains(JS_CLASS_NAME_REGEX)) return true
        return THROWABLE_SUBCLASS_REGEX.findAll(originalContent).any { match ->
            originalContent.contains(Regex("\"[^\"\\n]*\\b${Regex.escape(match.groupValues[1])}\\b"))
        }
    }

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
                    is StringDirective -> "${directive.name}=${directives[directive]}"
                    is ValueDirective<*> -> "${directive.name}=${directives[directive]}"
                }
            }
        }

    private fun computeLanguageSettings(directives: RegisteredDirectives): String? {
        val languageFeatures = directives[LanguageSettingsDirectives.LANGUAGE].sorted()
        val optIns = directives[LanguageSettingsDirectives.OPT_IN].sorted()
        val apiVersion = directives[LanguageSettingsDirectives.API_VERSION]
        val languageVersion = directives[LanguageSettingsDirectives.LANGUAGE_VERSION]
        val returnValueCheckerMode = directives[LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE]
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
        private val ISOLATION_DIRECTIVES: List<Directive> = listOf(
            // Some test failures can bring down an entire batch, so where a failure is expected, the test runs in isolation.
            CodegenTestDirectives.IGNORE_BACKEND,
            CodegenTestDirectives.IGNORE_BACKEND_K2,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE,
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
            // What is expected of the executable of this very test. The grouped pipeline runs `box()` only, so the
            // expectation is not checked, but a batch executable could not satisfy it anyway.
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

        /** A class or an object whose supertype list starts with a call of a `Throwable` subclass constructor. */
        private val THROWABLE_SUBCLASS_REGEX = Regex("""\b(?:class|object)\s+([A-Z]\w*)\b[^{\n]*?:\s*[\w.]*(?:Exception|Error|Throwable)\s*\(""")
    }
}
