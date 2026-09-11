/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.arguments

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal object TrackedCompilerArguments {
    /**
     * THE list of compiler arguments tracked as FUS metrics.
     */
    @Suppress("DEPRECATION") // several tracked arguments below are themselves deprecated compiler arguments
    val ALL: List<TrackedCompilerArgument<*>> = trackedCompilerArguments {
        forArguments<CommonCompilerArguments> {
            stringMetric(StringMetrics.KOTLIN_LANGUAGE_VERSION, CommonCompilerArguments::languageVersion)
            stringMetric(StringMetrics.KOTLIN_API_VERSION, CommonCompilerArguments::apiVersion)
            booleanMetric(BooleanMetrics.KOTLIN_PROGRESSIVE_MODE, CommonCompilerArguments::progressiveMode, allowImplicit = true)
            booleanMetric(BooleanMetrics.KOTLIN_SEPARATE_KMP_COMPILATION_ENABLED, CommonCompilerArguments::separateKmpCompilationScheme)

            booleanMetric(BooleanMetrics.CLI_DEBUG_LEVEL_COMPILER_CHECKS_ENABLED, CommonCompilerArguments::debugLevelCompilerChecks)
            booleanMetric(BooleanMetrics.CLI_LENIENT_MODE_ENABLED, CommonCompilerArguments::lenientMode)
            booleanMetric(BooleanMetrics.CLI_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, CommonCompilerArguments::allowAnyScriptsInSourceRoots)
            booleanMetric(BooleanMetrics.CLI_ALLOW_CONDITION_IMPLIES_RETURNS_CONTRACTS, CommonCompilerArguments::allowConditionImpliesReturnsContracts)
            booleanMetric(BooleanMetrics.CLI_ALLOW_CONTRACTS_ON_MORE_FUNCTIONS, CommonCompilerArguments::allowContractsOnMoreFunctions)
            booleanMetric(BooleanMetrics.CLI_ALLOW_HOLDSIN_CONTRACT, CommonCompilerArguments::allowHoldsinContract)
            booleanMetric(BooleanMetrics.CLI_ALLOW_KOTLIN_PACKAGE, CommonCompilerArguments::allowKotlinPackage)
            booleanMetric(BooleanMetrics.CLI_ALLOW_PRE_17_RUNTIME_JDK, CommonCompilerArguments::allowPre17RuntimeJdk)
            booleanMetric(BooleanMetrics.CLI_ALLOW_REIFIED_TYPE_IN_CATCH, CommonCompilerArguments::allowReifiedTypeInCatch)
            booleanMetric(BooleanMetrics.CLI_ALLOW_RETURNS_RESULT_OF, CommonCompilerArguments::allowReturnsResultOf)
            booleanMetric(BooleanMetrics.CLI_ANNOTATION_TARGET_ALL_ENABLED, CommonCompilerArguments::annotationTargetAll)
            booleanMetric(BooleanMetrics.CLI_CALLABLE_REFERENCES_TO_CONTEXTUAL_ENABLED, CommonCompilerArguments::callableReferencesToContextual)
            booleanMetric(BooleanMetrics.CLI_CHECK_PHASE_CONDITIONS_ENABLED, CommonCompilerArguments::checkPhaseConditions)
            booleanMetric(BooleanMetrics.CLI_COLLECTION_LITERALS_ENABLED, CommonCompilerArguments::collectionLiterals)
            booleanMetric(BooleanMetrics.CLI_COMPANION_BLOCKS_ENABLED, CommonCompilerArguments::companionBlocks)
            booleanMetric(BooleanMetrics.CLI_COMPANION_BLOCKS_AND_EXTENSIONS_ENABLED, CommonCompilerArguments::companionBlocksAndExtensions)
            booleanMetric(BooleanMetrics.CLI_CONSISTENT_DATA_CLASS_COPY_VISIBILITY_ENABLED, CommonCompilerArguments::consistentDataClassCopyVisibility)
            booleanMetric(BooleanMetrics.CLI_CONTEXT_PARAMETERS_ENABLED, CommonCompilerArguments::contextParameters)
            booleanMetric(BooleanMetrics.CLI_CONTEXT_SENSITIVE_RESOLUTION_ENABLED, CommonCompilerArguments::contextSensitiveResolution)
            booleanMetric(BooleanMetrics.CLI_DATA_FLOW_BASED_EXHAUSTIVENESS_ENABLED, CommonCompilerArguments::dataFlowBasedExhaustiveness)
            booleanMetric(BooleanMetrics.CLI_DETAILED_PERF_ENABLED, CommonCompilerArguments::detailedPerf)
            booleanMetric(BooleanMetrics.CLI_DEFAULT_SCRIPTING_PLUGIN_DISABLED, CommonCompilerArguments::disableDefaultScriptingPlugin)
            booleanMetric(BooleanMetrics.CLI_SOURCE_FILES_SORTING_DISABLED, CommonCompilerArguments::dontSortSourceFiles)
            booleanMetric(BooleanMetrics.CLI_WARN_ON_ERROR_SUPPRESSION_DISABLED, CommonCompilerArguments::dontWarnOnErrorSuppression)
            booleanMetric(BooleanMetrics.CLI_EAGER_LAMBDA_ANALYSIS_ENABLED, CommonCompilerArguments::eagerLambdaAnalysis)
            booleanMetric(BooleanMetrics.CLI_ALLOW_EXPECT_ACTUAL_CLASSES, CommonCompilerArguments::expectActualClasses)
            booleanMetric(BooleanMetrics.CLI_EXPLICIT_BACKING_FIELDS_ENABLED, CommonCompilerArguments::explicitBackingFields)
            booleanMetric(BooleanMetrics.CLI_EXPLICIT_CONTEXT_ARGUMENTS_ENABLED, CommonCompilerArguments::explicitContextArguments)
            booleanMetric(BooleanMetrics.CLI_FIR_AGGRESSIVE_PRUNING_ENABLED, CommonCompilerArguments::firAggressivePruning)
            booleanMetric(BooleanMetrics.CLI_HEADER_MODE_ENABLED, CommonCompilerArguments::headerMode)
            booleanMetric(BooleanMetrics.CLI_INTRINSIC_CONST_EVALUATION_ENABLED, CommonCompilerArguments::intrinsicConstEvaluation)
            booleanMetric(BooleanMetrics.CLI_LIST_PHASES_ENABLED, CommonCompilerArguments::listPhases)
            booleanMetric(BooleanMetrics.CLI_LOCAL_TYPE_ALIASES_ENABLED, CommonCompilerArguments::localTypeAliases)
            booleanMetric(BooleanMetrics.CLI_METADATA_KLIB_ENABLED, CommonCompilerArguments::metadataKlib)
            booleanMetric(BooleanMetrics.CLI_MULTI_DOLLAR_INTERPOLATION_ENABLED, CommonCompilerArguments::multiDollarInterpolation)
            booleanMetric(BooleanMetrics.CLI_MULTI_PLATFORM_ENABLED, CommonCompilerArguments::multiPlatform)
            booleanMetric(BooleanMetrics.CLI_NESTED_TYPE_ALIASES_ENABLED, CommonCompilerArguments::nestedTypeAliases)
            booleanMetric(BooleanMetrics.CLI_INLINING_DISABLED, CommonCompilerArguments::noInline)
            booleanMetric(BooleanMetrics.CLI_NON_LOCAL_BREAK_CONTINUE_ENABLED, CommonCompilerArguments::nonLocalBreakContinue)
            booleanMetric(BooleanMetrics.CLI_PRINT_CONFIGURATION_ENABLED, CommonCompilerArguments::printConfiguration)
            booleanMetric(BooleanMetrics.CLI_PROFILE_PHASES_ENABLED, CommonCompilerArguments::profilePhases)
            booleanMetric(BooleanMetrics.CLI_RENDER_INTERNAL_DIAGNOSTIC_NAMES_ENABLED, CommonCompilerArguments::renderInternalDiagnosticNames)
            booleanMetric(BooleanMetrics.CLI_REPL_ENABLED, CommonCompilerArguments::repl)
            booleanMetric(BooleanMetrics.CLI_REPORT_ALL_WARNINGS_ENABLED, CommonCompilerArguments::reportAllWarnings)
            booleanMetric(BooleanMetrics.CLI_REPORT_OUTPUT_FILES_ENABLED, CommonCompilerArguments::reportOutputFiles)
            booleanMetric(BooleanMetrics.CLI_REPORT_PERF_ENABLED, CommonCompilerArguments::reportPerf)
            booleanMetric(BooleanMetrics.CLI_SKIP_METADATA_VERSION_CHECK_ENABLED, CommonCompilerArguments::skipMetadataVersionCheck)
            booleanMetric(BooleanMetrics.CLI_SKIP_PRERELEASE_CHECK_ENABLED, CommonCompilerArguments::skipPrereleaseCheck)
            booleanMetric(BooleanMetrics.CLI_STDLIB_COMPILATION_ENABLED, CommonCompilerArguments::stdlibCompilation)
            booleanMetric(BooleanMetrics.CLI_VERSION_WARNINGS_SUPPRESSED, CommonCompilerArguments::suppressVersionWarnings)
            booleanMetric(BooleanMetrics.CLI_FIR_IC_ENABLED, CommonCompilerArguments::useFirIC)
            booleanMetric(BooleanMetrics.CLI_FIR_LIGHT_TREE_PARSER_ENABLED, CommonCompilerArguments::useFirLT)
            booleanMetric(BooleanMetrics.CLI_WHEN_GUARDS_ENABLED, CommonCompilerArguments::whenGuards)
            booleanMetric(BooleanMetrics.CLI_SCRIPT_EVALUATION_ENABLED, CommonCompilerArguments::script)

            stringMetric(StringMetrics.CLI_EXPLICIT_RETURN_TYPES_MODE, CommonCompilerArguments::explicitReturnTypes)
            stringMetric(StringMetrics.CLI_ANNOTATION_DEFAULT_TARGET, CommonCompilerArguments::annotationDefaultTarget)
            stringMetric(StringMetrics.CLI_EXPLICIT_API_MODE, CommonCompilerArguments::explicitApi)
            stringMetric(StringMetrics.CLI_HEADER_MODE_TYPE, CommonCompilerArguments::headerModeType)
            stringMetric(StringMetrics.CLI_METADATA_VERSION, CommonCompilerArguments::metadataVersion)
            stringMetric(StringMetrics.CLI_NAME_BASED_DESTRUCTURING_MODE, CommonCompilerArguments::nameBasedDestructuring)
            stringMetric(StringMetrics.CLI_RETURN_VALUE_CHECKER_MODE, CommonCompilerArguments::returnValueChecker)
            stringMetric(StringMetrics.CLI_VERIFY_IR_MODE, CommonCompilerArguments::verifyIr)
            stringMetric(StringMetrics.CLI_MANUALLY_CONFIGURED_LANGUAGE_FEATURES, CommonCompilerArguments::manuallyConfiguredFeatures)
            stringMetric(StringMetrics.CLI_DISABLED_IR_CHECKERS, CommonCompilerArguments::disableIrCheckers)
            stringMetric(StringMetrics.CLI_ENABLED_ADDITIONAL_IR_CHECKERS, CommonCompilerArguments::enableAdditionalIrCheckers)
            stringMetric(StringMetrics.CLI_DISABLED_PHASES, CommonCompilerArguments::disablePhases)
            stringMetric(StringMetrics.CLI_PHASES_TO_DUMP, CommonCompilerArguments::phasesToDump)
            stringMetric(StringMetrics.CLI_PHASES_TO_DUMP_AFTER, CommonCompilerArguments::phasesToDumpAfter)
            stringMetric(StringMetrics.CLI_PHASES_TO_DUMP_BEFORE, CommonCompilerArguments::phasesToDumpBefore)
            stringMetric(StringMetrics.CLI_PHASES_TO_VALIDATE, CommonCompilerArguments::phasesToValidate)
            stringMetric(StringMetrics.CLI_PHASES_TO_VALIDATE_AFTER, CommonCompilerArguments::phasesToValidateAfter)
            stringMetric(StringMetrics.CLI_PHASES_TO_VALIDATE_BEFORE, CommonCompilerArguments::phasesToValidateBefore)
            stringMetric(StringMetrics.CLI_VERBOSE_PHASES, CommonCompilerArguments::verbosePhases)
            stringMetric(StringMetrics.CLI_WARNING_LEVELS, CommonCompilerArguments::warningLevels)
        }

        forArguments<K2JVMCompilerArguments> {
            stringListMetric(
                StringListMetrics.JVM_DEFAULTS, "-jvm-default", "-Xjvm-default",
                extract = { it.jvmDefaultMode() },
                convert = JvmDefaultMode::description,
            )

            booleanMetric(BooleanMetrics.CLI_ALLOW_NO_SOURCE_FILES, K2JVMCompilerArguments::allowNoSourceFiles)
            booleanMetric(BooleanMetrics.CLI_ALLOW_UNSTABLE_DEPENDENCIES, K2JVMCompilerArguments::allowUnstableDependencies)
            booleanMetric(BooleanMetrics.CLI_ANNOTATIONS_IN_METADATA_ENABLED, K2JVMCompilerArguments::annotationsInMetadata)
            booleanMetric(BooleanMetrics.CLI_JVM_DEBUG_MODE_ENABLED, K2JVMCompilerArguments::enableDebugMode)
            booleanMetric(BooleanMetrics.CLI_DIRECT_JAVA_ACTUALIZATION_ENABLED, K2JVMCompilerArguments::directJavaActualization)
            booleanMetric(BooleanMetrics.CLI_STANDARD_SCRIPTING_DISABLED, K2JVMCompilerArguments::disableStandardScript)
            booleanMetric(BooleanMetrics.CLI_JVM_TYPE_ANNOTATIONS_EMITTED, K2JVMCompilerArguments::emitJvmTypeAnnotations)
            booleanMetric(BooleanMetrics.CLI_ENHANCED_COROUTINES_DEBUGGING_ENABLED, K2JVMCompilerArguments::enhancedCoroutinesDebugging)
            booleanMetric(BooleanMetrics.CLI_STRICT_METADATA_VERSION_SEMANTICS_ENABLED, K2JVMCompilerArguments::strictMetadataVersionSemantics)
            booleanMetric(BooleanMetrics.CLI_INDY_ANNOTATED_LAMBDAS_ALLOWED, K2JVMCompilerArguments::indyAllowAnnotatedLambdas)
            booleanMetric(BooleanMetrics.CLI_JAVA_DIRECT_ENABLED, K2JVMCompilerArguments::javaDirect)
            booleanMetric(BooleanMetrics.CLI_JVM_PREVIEW_FEATURES_ENABLED, K2JVMCompilerArguments::enableJvmPreview)
            booleanMetric(BooleanMetrics.CLI_JVM_EXPOSE_BOXED_ENABLED, K2JVMCompilerArguments::jvmExposeBoxed)
            booleanMetric(BooleanMetrics.CLI_MULTIFILE_PARTS_INHERITANCE_ENABLED, K2JVMCompilerArguments::inheritMultifileParts)
            booleanMetric(BooleanMetrics.CLI_CALL_ASSERTIONS_DISABLED, K2JVMCompilerArguments::noCallAssertions)
            booleanMetric(BooleanMetrics.CLI_NEW_JAVA_ANNOTATION_TARGETS_DISABLED, K2JVMCompilerArguments::noNewJavaAnnotationTargets)
            booleanMetric(BooleanMetrics.CLI_OPTIMIZATIONS_DISABLED, K2JVMCompilerArguments::noOptimize)
            booleanMetric(BooleanMetrics.CLI_PARAM_ASSERTIONS_DISABLED, K2JVMCompilerArguments::noParamAssertions)
            booleanMetric(BooleanMetrics.CLI_RECEIVER_ASSERTIONS_DISABLED, K2JVMCompilerArguments::noReceiverAssertions)
            booleanMetric(BooleanMetrics.CLI_JAR_TIMESTAMPS_RESET_DISABLED, K2JVMCompilerArguments::noResetJarTimestamps)
            booleanMetric(BooleanMetrics.CLI_SOURCE_DEBUG_EXTENSION_DISABLED, K2JVMCompilerArguments::noSourceDebugExtension)
            booleanMetric(BooleanMetrics.CLI_UNIFIED_NULL_CHECKS_DISABLED, K2JVMCompilerArguments::noUnifiedNullChecks)
            booleanMetric(BooleanMetrics.CLI_OUTPUT_BUILTINS_METADATA_ENABLED, K2JVMCompilerArguments::outputBuiltinsMetadata)
            booleanMetric(BooleanMetrics.CLI_SANITIZE_PARENTHESES_ENABLED, K2JVMCompilerArguments::sanitizeParentheses)
            booleanMetric(BooleanMetrics.CLI_MISSING_BUILTINS_ERROR_SUPPRESSED, K2JVMCompilerArguments::suppressMissingBuiltinsError)
            booleanMetric(BooleanMetrics.CLI_OLD_INLINE_CLASSES_MANGLING_SCHEME_USED, K2JVMCompilerArguments::useOldInlineClassesManglingScheme)
            booleanMetric(BooleanMetrics.CLI_FAST_JAR_FILE_SYSTEM_USED, K2JVMCompilerArguments::useFastJarFileSystem)
            booleanMetric(BooleanMetrics.CLI_INLINE_SCOPES_NUMBERS_USED, K2JVMCompilerArguments::useInlineScopesNumbers)
            booleanMetric(BooleanMetrics.CLI_METADATA_ON_INCREMENTAL_CLASSPATH_USED, K2JVMCompilerArguments::useMetadataOnIncrementalClasspath)
            booleanMetric(BooleanMetrics.CLI_OLD_CLASS_FILES_READING_USED, K2JVMCompilerArguments::useOldClassFilesReading)
            booleanMetric(BooleanMetrics.CLI_TYPE_TABLE_USED, K2JVMCompilerArguments::useTypeTable)
            booleanMetric(BooleanMetrics.CLI_BYTECODE_VALIDATION_ENABLED, K2JVMCompilerArguments::validateBytecode)
            booleanMetric(BooleanMetrics.CLI_KOTLIN_RUNTIME_INCLUDED_IN_JAR, K2JVMCompilerArguments::includeRuntime)
            booleanMetric(BooleanMetrics.CLI_JAVA_PARAMETERS_METADATA_ENABLED, K2JVMCompilerArguments::javaParameters)
            booleanMetric(BooleanMetrics.CLI_JDK_EXCLUDED_FROM_CLASSPATH, K2JVMCompilerArguments::noJdk)
            booleanMetric(BooleanMetrics.CLI_KOTLIN_REFLECT_EXCLUDED_FROM_CLASSPATH, K2JVMCompilerArguments::noReflect)
            booleanMetric(BooleanMetrics.CLI_KOTLIN_STDLIB_EXCLUDED_FROM_CLASSPATH, K2JVMCompilerArguments::noStdlib)

            stringMetric(StringMetrics.CLI_ABI_STABILITY_MODE, K2JVMCompilerArguments::abiStability)
            stringMetric(StringMetrics.CLI_ASSERTIONS_MODE, K2JVMCompilerArguments::assertionsMode)
            stringMetric(StringMetrics.CLI_DEFAULT_SCRIPT_EXTENSION, K2JVMCompilerArguments::defaultScriptExtension)
            stringMetric(StringMetrics.CLI_JDK_RELEASE_VERSION, K2JVMCompilerArguments::jdkRelease)
            stringMetric(StringMetrics.CLI_JSPECIFY_ANNOTATIONS_MODE, K2JVMCompilerArguments::jspecifyAnnotations)
            stringMetric(StringMetrics.CLI_LAMBDAS_CODEGEN_MODE, K2JVMCompilerArguments::lambdas)
            stringMetric(StringMetrics.CLI_SAM_CONVERSIONS_CODEGEN_MODE, K2JVMCompilerArguments::samConversions)
            stringMetric(StringMetrics.CLI_STRING_CONCAT_CODEGEN_MODE, K2JVMCompilerArguments::stringConcat)
            stringMetric(StringMetrics.CLI_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS_MODE, K2JVMCompilerArguments::supportCompatqualCheckerFrameworkAnnotations)
            stringMetric(StringMetrics.CLI_VALHALLA_SUPPORT_MODE, K2JVMCompilerArguments::valhallaSupport)
            stringMetric(StringMetrics.CLI_WHEN_EXPRESSIONS_CODEGEN_MODE, K2JVMCompilerArguments::whenExpressionsGeneration)
            stringMetric(StringMetrics.CLI_JVM_TARGET_VERSION, K2JVMCompilerArguments::jvmTarget)
            stringMetric(StringMetrics.CLI_JSR305_NULLABILITY_ANNOTATIONS_MODE, K2JVMCompilerArguments::jsr305)

            numberMetric(NumericalMetrics.CLI_BACKEND_THREADS_COUNT, K2JVMCompilerArguments::backendThreads)
        }

        forArguments<CommonKlibBasedCompilerArguments> {
            booleanMetric(BooleanMetrics.CLI_KLIB_SIGNATURE_CLASH_CHECKS_ENABLED, CommonKlibBasedCompilerArguments::enableSignatureClashChecks)
            booleanMetric(BooleanMetrics.CLI_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS_SKIPPED, CommonKlibBasedCompilerArguments::skipLibrarySpecialCompatibilityChecks)

            stringMetric(StringMetrics.CLI_KLIB_ABI_VERSION, CommonKlibBasedCompilerArguments::customKlibAbiVersion)
            stringMetric(StringMetrics.CLI_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY, CommonKlibBasedCompilerArguments::duplicatedUniqueNameStrategy)
            stringMetric(StringMetrics.CLI_KLIB_IR_INLINER_MODE, CommonKlibBasedCompilerArguments::irInlinerBeforeKlibSerialization)
            stringMetric(StringMetrics.CLI_PARTIAL_LINKAGE_MODE, CommonKlibBasedCompilerArguments::partialLinkageMode)
            stringMetric(StringMetrics.CLI_PARTIAL_LINKAGE_LOG_LEVEL, CommonKlibBasedCompilerArguments::partialLinkageLogLevel)

            numberMetric(NumericalMetrics.CLI_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT, CommonKlibBasedCompilerArguments::klibZipFileAccessorCacheLimit)
        }

        forArguments<K2MetadataCompilerArguments> {
            booleanMetric(BooleanMetrics.CLI_LEGACY_METADATA_JAR_ENABLED, K2MetadataCompilerArguments::legacyMetadataJar)

            stringMetric(StringMetrics.CLI_METADATA_TARGET_PLATFORM, K2MetadataCompilerArguments::targetPlatform)

            numberMetric(NumericalMetrics.CLI_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT, K2MetadataCompilerArguments::klibZipFileAccessorCacheLimit)
        }

        forArguments<CommonJsAndWasmCompilerArguments> {
            booleanMetric(BooleanMetrics.CLI_FRIEND_MODULES_DISABLED, CommonJsAndWasmCompilerArguments::friendModulesDisabled)
            booleanMetric(BooleanMetrics.CLI_IR_DCE_ENABLED, CommonJsAndWasmCompilerArguments::irDce)
            booleanMetric(BooleanMetrics.CLI_IR_DCE_REACHABILITY_INFO_PRINTED, CommonJsAndWasmCompilerArguments::irDcePrintReachabilityInfo)
            booleanMetric(BooleanMetrics.CLI_IR_PRODUCE_JS_ENABLED, CommonJsAndWasmCompilerArguments::irProduceJs)
            booleanMetric(BooleanMetrics.CLI_IR_PRODUCE_KLIB_DIR_ENABLED, CommonJsAndWasmCompilerArguments::irProduceKlibDir)
            booleanMetric(BooleanMetrics.CLI_IR_PRODUCE_KLIB_FILE_ENABLED, CommonJsAndWasmCompilerArguments::irProduceKlibFile)
            booleanMetric(BooleanMetrics.CLI_STRICT_IMPLICIT_EXPORT_TYPES_ENABLED, CommonJsAndWasmCompilerArguments::strictImplicitExportType)
            booleanMetric(BooleanMetrics.CLI_NOPACK_ENABLED, CommonJsAndWasmCompilerArguments::nopack)

            stringMetric(StringMetrics.CLI_IR_DCE_RUNTIME_DIAGNOSTIC_MODE, CommonJsAndWasmCompilerArguments::irDceRuntimeDiagnostic)
            stringMetric(StringMetrics.CLI_MAIN_FUNCTION_EXECUTION_MODE, CommonJsAndWasmCompilerArguments::main)
            stringMetric(StringMetrics.CLI_SOURCE_MAP_EMBED_SOURCES_MODE, CommonJsAndWasmCompilerArguments::sourceMapEmbedSources)
            stringMetric(StringMetrics.CLI_SOURCE_MAP_NAMES_POLICY, CommonJsAndWasmCompilerArguments::sourceMapNamesPolicy)
        }

        forArguments<KotlinWasmCompilerArguments> {
            booleanMetric(BooleanMetrics.CLI_WASM_BACKEND_EXPLICITLY_SELECTED, KotlinWasmCompilerArguments::wasm)
            booleanMetric(BooleanMetrics.CLI_WASM_IC_REGENERATE_UNCHANGED_MODULES_ENABLED, KotlinWasmCompilerArguments::regenerateUnchangedModules)
            booleanMetric(BooleanMetrics.CLI_WASM_DEBUG_FRIENDLY_COMPILATION_ENABLED, KotlinWasmCompilerArguments::forceDebugFriendlyCompilation)
            booleanMetric(BooleanMetrics.CLI_WASM_DEBUG_INFO_ENABLED, KotlinWasmCompilerArguments::wasmDebug)
            booleanMetric(BooleanMetrics.CLI_WASM_DEBUGGER_CUSTOM_FORMATTERS_ENABLED, KotlinWasmCompilerArguments::debuggerCustomFormatters)
            booleanMetric(BooleanMetrics.CLI_WASM_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION_DISABLED, KotlinWasmCompilerArguments::wasmDisableArrayRangeChecksSafeElimination)
            booleanMetric(BooleanMetrics.CLI_WASM_ARRAY_RANGE_CHECKS_ENABLED, KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks)
            booleanMetric(BooleanMetrics.CLI_WASM_ASSERTS_ENABLED, KotlinWasmCompilerArguments::wasmEnableAsserts)
            booleanMetric(BooleanMetrics.CLI_WASM_TAIL_CALLS_ENABLED, KotlinWasmCompilerArguments::wasmEnableTailCalls)
            booleanMetric(BooleanMetrics.CLI_WASM_CLOSED_WORLD_MULTIMODULE_ENABLED, KotlinWasmCompilerArguments::wasmGenerateClosedWorldMultimodule)
            booleanMetric(BooleanMetrics.CLI_WASM_DWARF_GENERATED, KotlinWasmCompilerArguments::generateDwarf)
            booleanMetric(BooleanMetrics.CLI_WASM_WAT_GENERATED, KotlinWasmCompilerArguments::wasmGenerateWat)
            booleanMetric(BooleanMetrics.CLI_WASM_INCLUDED_MODULE_ONLY_ENABLED, KotlinWasmCompilerArguments::wasmIncludedModuleOnly)
            booleanMetric(BooleanMetrics.CLI_WASM_KCLASS_FQN_SUPPORT_ENABLED, KotlinWasmCompilerArguments::wasmKClassFqn)
            booleanMetric(BooleanMetrics.CLI_WASM_JSTAG_DISABLED, KotlinWasmCompilerArguments::wasmNoJsTag)
            booleanMetric(BooleanMetrics.CLI_WASM_SOURCE_MAP_UNAVAILABLE_SOURCES_INCLUDED, KotlinWasmCompilerArguments::includeUnavailableSourcesIntoSourceMap)
            booleanMetric(BooleanMetrics.CLI_WASM_NEW_EXCEPTION_PROPOSAL_USED, KotlinWasmCompilerArguments::wasmUseNewExceptionProposal)
            booleanMetric(BooleanMetrics.CLI_WASM_STACK_SWITCHING_PROPOSAL_USED, KotlinWasmCompilerArguments::wasmUseStackSwitchingProposal)
            booleanMetric(BooleanMetrics.CLI_WASM_TRAPS_INSTEAD_OF_EXCEPTIONS_USED, KotlinWasmCompilerArguments::wasmUseTrapsInsteadOfExceptions)

            stringMetric(StringMetrics.CLI_WASM_INTERNAL_LOCAL_VARIABLE_PREFIX, KotlinWasmCompilerArguments::wasmInternalLocalVariablePrefix)
            stringMetric(StringMetrics.CLI_WASM_TARGET_MODE, KotlinWasmCompilerArguments::wasmTarget)
        }

        forArguments<K2JSCompilerArguments> {
            withCondition(K2JSCompilerArguments::irProduceJs) {
                booleanMetric(BooleanMetrics.JS_SOURCE_MAP, K2JSCompilerArguments::sourceMap, allowImplicit = true)
                booleanMetric(BooleanMetrics.JS_GENERATE_DTS, K2JSCompilerArguments::generateDts, allowImplicit = true)
                stringListMetric(StringListMetrics.JS_PROPERTY_LAZY_INITIALIZATION, K2JSCompilerArguments::irPropertyLazyInitialization, allowImplicit = true)
                stringMetric(StringMetrics.JS_ES_TARGET, K2JSCompilerArguments::target, default = DEFAULT_VALUE)
                stringMetric(StringMetrics.JS_MODULE_SYSTEM, K2JSCompilerArguments::moduleKind, default = DEFAULT_VALUE)
                stringMetric(StringMetrics.JS_OUTPUT_GRANULARITY, K2JSCompilerArguments::irPerModule, allowImplicit = true) { perModule ->
                    val granularity = when (perModule) {
                        true -> KotlinJsIrOutputGranularity.PER_MODULE
                        false -> KotlinJsIrOutputGranularity.WHOLE_PROGRAM
                    }
                    granularity.name.toLowerCaseAsciiOnly()
                }
            }

            booleanMetric(BooleanMetrics.CLI_JS_DTS_USE_UNKNOWN_INSTEAD_ANY_ENABLED, K2JSCompilerArguments::useUnknownInsteadAny)
            booleanMetric(BooleanMetrics.CLI_JS_EXTENSION_FUNCTIONS_IN_EXTERNALS_ENABLED, K2JSCompilerArguments::extensionFunctionsInExternals)
            booleanMetric(BooleanMetrics.CLI_JS_IMPLEMENTABLE_INTERFACES_EXPORTING_ENABLED, K2JSCompilerArguments::allowImplementableInterfacesExporting)
            booleanMetric(BooleanMetrics.CLI_JS_SUSPEND_FUNCTION_EXPORTING_ENABLED, K2JSCompilerArguments::allowExportingSuspendFunctions)
            booleanMetric(BooleanMetrics.CLI_JS_ES_ARROW_FUNCTIONS_USED, K2JSCompilerArguments::useEsArrowFunctions)
            booleanMetric(BooleanMetrics.CLI_JS_ES_CLASSES_USED, K2JSCompilerArguments::useEsClasses)
            booleanMetric(BooleanMetrics.CLI_JS_ES_GENERATORS_USED, K2JSCompilerArguments::useEsGenerators)
            booleanMetric(BooleanMetrics.CLI_JS_LONG_AS_BIGINT, K2JSCompilerArguments::compileLongAsBigInt)
            booleanMetric(BooleanMetrics.CLI_ENABLED_EXPORT_KDOC, K2JSCompilerArguments::exportKDoc)
            booleanMetric(BooleanMetrics.CLI_JS_POLYFILLS_GENERATED, K2JSCompilerArguments::generatePolyfills)
            booleanMetric(BooleanMetrics.CLI_JS_INTEGER_DIVISION_CHECK_ENABLED, K2JSCompilerArguments::integerDivisionCheck)
            booleanMetric(BooleanMetrics.CLI_JS_IR_BUILD_CACHE_ENABLED, K2JSCompilerArguments::irBuildCache)
            booleanMetric(BooleanMetrics.CLI_JS_IR_GENERATE_INLINE_ANONYMOUS_FUNCTIONS_ENABLED, K2JSCompilerArguments::irGenerateInlineAnonymousFunctions)
            booleanMetric(BooleanMetrics.CLI_JS_IR_MINIMIZED_MEMBER_NAMES_ENABLED, K2JSCompilerArguments::irMinimizedMemberNames)
            booleanMetric(BooleanMetrics.CLI_JS_IR_PER_FILE_OUTPUT_ENABLED, K2JSCompilerArguments::irPerFile)
            booleanMetric(BooleanMetrics.CLI_JS_IR_SAFE_EXTERNAL_BOOLEAN_ENABLED, K2JSCompilerArguments::irSafeExternalBoolean)
            booleanMetric(BooleanMetrics.CLI_JS_GENERATED_CODE_OPTIMIZATION_ENABLED, K2JSCompilerArguments::optimizeGeneratedJs)
            booleanMetric(BooleanMetrics.CLI_JS_SUSPEND_LAMBDA_EXPORTING_ENABLED, K2JSCompilerArguments::allowExportingSuspendLambdas)

            stringMetric(StringMetrics.CLI_JS_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC_MODE, K2JSCompilerArguments::irSafeExternalBooleanDiagnostic)
        }

        forArguments<K2NativeCompilerArguments> {
            selectFlagMetric(K2NativeCompilerArguments::binaryOptions) { it.binaryOption("gc")?.let(::gcMetric) }
            selectFlagMetric(K2NativeCompilerArguments::binaryOptions) {
                BooleanMetrics.ENABLED_SWIFT_EXPORT.takeIf { _ -> it.binaryOption("swiftExport") == "true" }
            }

            booleanMetric(BooleanMetrics.CLI_NATIVE_CHECK_DEPENDENCIES_ENABLED, K2NativeCompilerArguments::checkDependencies)
            booleanMetric(BooleanMetrics.CLI_NATIVE_CHECK_EXTERNAL_CALLS_ENABLED, K2NativeCompilerArguments::checkExternalCalls)
            booleanMetric(BooleanMetrics.CLI_ENABLED_EXPORT_KDOC, K2NativeCompilerArguments::exportKDoc)
            booleanMetric(BooleanMetrics.CLI_NATIVE_PER_FILE_CACHE_FORCED, K2NativeCompilerArguments::makePerFileCache)
            booleanMetric(BooleanMetrics.CLI_NATIVE_OBJC_GENERICS_DISABLED, K2NativeCompilerArguments::noObjcGenerics)
            booleanMetric(BooleanMetrics.CLI_NATIVE_FRAMEWORK_BINARY_OMITTED, K2NativeCompilerArguments::omitFrameworkBinary)
            booleanMetric(BooleanMetrics.CLI_NATIVE_PRINT_BITCODE_ENABLED, K2NativeCompilerArguments::printBitCode)
            booleanMetric(BooleanMetrics.CLI_NATIVE_PRINT_FILES_ENABLED, K2NativeCompilerArguments::printFiles)
            booleanMetric(BooleanMetrics.CLI_NATIVE_PRINT_IR_ENABLED, K2NativeCompilerArguments::printIr)
            booleanMetric(BooleanMetrics.CLI_NATIVE_STATIC_FRAMEWORK_ENABLED, K2NativeCompilerArguments::staticFramework)
            booleanMetric(BooleanMetrics.CLI_NATIVE_VERIFY_BITCODE_ENABLED, K2NativeCompilerArguments::verifyBitCode)
            booleanMetric(BooleanMetrics.CLI_NATIVE_RUNTIME_ASSERTIONS_ENABLED, K2NativeCompilerArguments::enableAssertions)
            booleanMetric(BooleanMetrics.CLI_NATIVE_DEBUG_INFO_ENABLED, K2NativeCompilerArguments::debug)
            booleanMetric(BooleanMetrics.CLI_NATIVE_NO_EXIT_TEST_RUNNER_GENERATED, K2NativeCompilerArguments::generateNoExitTestRunner)
            booleanMetric(BooleanMetrics.CLI_NATIVE_TEST_RUNNER_GENERATED, K2NativeCompilerArguments::generateTestRunner)
            booleanMetric(BooleanMetrics.CLI_NATIVE_WORKER_TEST_RUNNER_GENERATED, K2NativeCompilerArguments::generateWorkerTestRunner)
            booleanMetric(BooleanMetrics.CLI_NATIVE_LIST_TARGETS_ENABLED, K2NativeCompilerArguments::listTargets)
            booleanMetric(BooleanMetrics.CLI_NATIVE_DEFAULT_LIBS_EXCLUDED, K2NativeCompilerArguments::nodefaultlibs)
            booleanMetric(BooleanMetrics.CLI_NATIVE_MAIN_ENTRY_POINT_EXTERNAL, K2NativeCompilerArguments::nomain)
            booleanMetric(BooleanMetrics.CLI_NOPACK_ENABLED, K2NativeCompilerArguments::nopack)
            booleanMetric(BooleanMetrics.CLI_NATIVE_STDLIB_EXCLUDED, K2NativeCompilerArguments::nostdlib)
            booleanMetric(BooleanMetrics.CLI_NATIVE_OPTIMIZATION_ENABLED, K2NativeCompilerArguments::optimization)

            stringMetric(StringMetrics.CLI_NATIVE_LIGHT_DEBUG_MODE, K2NativeCompilerArguments::lightDebugString)
            stringMetric(StringMetrics.CLI_NATIVE_ALLOCATOR, K2NativeCompilerArguments::allocator)
            stringMetric(StringMetrics.CLI_NATIVE_DEBUG_INFO_FORMAT_VERSION, K2NativeCompilerArguments::debugInfoFormatVersion)
            stringMetric(StringMetrics.CLI_NATIVE_DEBUG_TRAMPOLINE_MODE, K2NativeCompilerArguments::generateDebugTrampolineString)
            stringMetric(StringMetrics.CLI_NATIVE_IR_PROPERTY_LAZY_INITIALIZATION_MODE, K2NativeCompilerArguments::propertyLazyInitialization)
            stringMetric(StringMetrics.CLI_NATIVE_LLVM_LTO_PASSES, K2NativeCompilerArguments::llvmLTOPasses)
            stringMetric(StringMetrics.CLI_NATIVE_LLVM_MODULE_PASSES, K2NativeCompilerArguments::llvmModulePasses)
            stringMetric(StringMetrics.CLI_NATIVE_PRE_LINK_CACHES_MODE, K2NativeCompilerArguments::preLinkCaches)
            stringMetric(StringMetrics.CLI_NATIVE_RUNTIME_LOGS, K2NativeCompilerArguments::runtimeLogs)
            stringMetric(StringMetrics.CLI_NATIVE_MEMORY_MODEL, K2NativeCompilerArguments::memoryModel)
            stringMetric(StringMetrics.CLI_NATIVE_PRODUCE_OUTPUT_KIND, K2NativeCompilerArguments::produce)
            stringMetric(StringMetrics.CLI_NATIVE_MANIFEST_TARGETS, K2NativeCompilerArguments::manifestNativeTargets)
            stringMetric(StringMetrics.CLI_NATIVE_SAVE_LLVM_IR_AFTER_PHASES, K2NativeCompilerArguments::saveLlvmIrAfter)

            numberMetric(NumericalMetrics.CLI_BACKEND_THREADS_COUNT, K2NativeCompilerArguments::backendThreads)
        }
    }
}

private const val DEFAULT_VALUE = "default"

/**
 * The effective `-jvm-default` mode, or `null` when the argument was not specified.
 *
 * Mirrors the (private) `K2JVMCompilerArgumentsConfigurator.configureJvmDefaultMode`: the stable `-jvm-default` wins
 * over the deprecated `-Xjvm-default`, and the deprecated one uses the *old* value names, so `all` maps to
 * [JvmDefaultMode.NO_COMPATIBILITY] and `all-compatibility` to [JvmDefaultMode.ENABLE].
 *
 * Branching on raw string nullability rather than on the parsed result is intentional and matches the compiler: a
 * malformed `-jvm-default=oops` yields `null` instead of falling through to the deprecated argument.
 *
 * Mapping through [JvmDefaultMode] is also what keeps the reported value inside the allowed list of
 * [StringListMetrics.JVM_DEFAULTS] - the raw deprecated spellings are not among its allowed values.
 */
@Suppress("DEPRECATION")
private fun K2JVMCompilerArguments.jvmDefaultMode(): JvmDefaultMode? = when {
    jvmDefaultStable != null -> JvmDefaultMode.fromStringOrNull(jvmDefaultStable)
    jvmDefault != null -> JvmDefaultMode.fromStringOrNullOld(jvmDefault)
    else -> null
}

// The value of a single `-Xbinary=<name>=<value>` option, or `null` when it was not specified.
private fun K2NativeCompilerArguments.binaryOption(name: String): String? =
    binaryOptions.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

private fun gcMetric(value: String): BooleanMetrics? = when (value.toLowerCaseAsciiOnly()) {
    "noop" -> BooleanMetrics.ENABLED_NOOP_GC
    "stwms", "stop_the_world_mark_and_sweep" -> BooleanMetrics.ENABLED_STWMS_GC
    "pmcs", "parallel_mark_concurrent_sweep" -> BooleanMetrics.ENABLED_PMCS_GC
    "cms", "concurrent_mark_and_sweep" -> BooleanMetrics.ENABLED_CMS_GC
    else -> null
}
