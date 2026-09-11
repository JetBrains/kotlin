/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy.*
import org.jetbrains.kotlin.statistics.metrics.StringOverridePolicy.*
import org.jetbrains.kotlin.statistics.metrics.StringListOverridePolicy.*

enum class StringMetrics(val type: StringOverridePolicy, val anonymization: StringAnonymizationPolicy, val perProject: Boolean = false) {

    // User environment
    GRADLE_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    PROJECT_PATH(OVERRIDE, RegexControlled("([0-9A-Fa-f]{40,64})|undefined", true)),

    OS_TYPE(OVERRIDE, RegexControlled("(Windows|Windows |Windows Server |Mac|Linux|FreeBSD|Solaris|Other|Mac OS X)\\d*", false)),
    OS_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    // Component versions
    LIBRARY_SPRING_VERSION(OVERRIDE_VERSION_IF_NOT_SET, ComponentVersionAnonymizer()),
    LIBRARY_VAADIN_VERSION(OVERRIDE_VERSION_IF_NOT_SET, ComponentVersionAnonymizer()),
    LIBRARY_GWT_VERSION(OVERRIDE_VERSION_IF_NOT_SET, ComponentVersionAnonymizer()),
    LIBRARY_HIBERNATE_VERSION(OVERRIDE_VERSION_IF_NOT_SET, ComponentVersionAnonymizer()),

    KOTLIN_GRADLE_PLUGIN_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_COMPILER_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_STDLIB_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_REFLECT_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_COROUTINES_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_SERIALIZATION_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    ANDROID_GRADLE_PLUGIN_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    KSP_GRADLE_PLUGIN_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    // Features
    KOTLIN_LANGUAGE_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_API_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    JS_OUTPUT_GRANULARITY(OVERRIDE, RegexControlled("(whole_program|per_module|per_file)", false)),
    JS_ES_TARGET(OVERRIDE, AllowedListAnonymizer(listOf("es5", "es2015", "es2020", "default"))),
    JS_MODULE_SYSTEM(OVERRIDE, AllowedListAnonymizer(listOf("plain", "amd", "commonjs", "umd", "es", "default"))),

    // CLI arguments
    CLI_EXPLICIT_RETURN_TYPES_MODE(OVERRIDE, AllowedListAnonymizer(listOf("strict", "warning", "disable"))),
    CLI_ANNOTATION_DEFAULT_TARGET(OVERRIDE, AllowedListAnonymizer(listOf("first-only", "first-only-warn", "param-property"))),
    CLI_EXPLICIT_API_MODE(OVERRIDE, AllowedListAnonymizer(listOf("strict", "warning", "disable"))),
    CLI_HEADER_MODE_TYPE(OVERRIDE, AllowedListAnonymizer(listOf("any", "compilation"))),
    CLI_METADATA_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    CLI_NAME_BASED_DESTRUCTURING_MODE(OVERRIDE, AllowedListAnonymizer(listOf("only-syntax", "name-mismatch", "complete"))),
    CLI_RETURN_VALUE_CHECKER_MODE(OVERRIDE, AllowedListAnonymizer(listOf("check", "full", "disable"))),
    CLI_VERIFY_IR_MODE(OVERRIDE, AllowedListAnonymizer(listOf("none", "warning", "error"))),
    CLI_ABI_STABILITY_MODE(OVERRIDE, AllowedListAnonymizer(listOf("stable", "unstable"))),
    CLI_ASSERTIONS_MODE(OVERRIDE, AllowedListAnonymizer(listOf("always-enable", "always-disable", "jvm", "legacy"))),
    CLI_JDK_RELEASE_VERSION(OVERRIDE, AllowedListAnonymizer(listOf("1.8") + (9..27).map { it.toString() })),
    CLI_JSPECIFY_ANNOTATIONS_MODE(OVERRIDE, AllowedListAnonymizer(listOf("ignore", "strict", "warn"))),
    CLI_LAMBDAS_CODEGEN_MODE(OVERRIDE, AllowedListAnonymizer(listOf("class", "indy"))),
    CLI_SAM_CONVERSIONS_CODEGEN_MODE(OVERRIDE, AllowedListAnonymizer(listOf("class", "indy"))),
    CLI_STRING_CONCAT_CODEGEN_MODE(OVERRIDE, AllowedListAnonymizer(listOf("indy-with-constants", "indy", "inline"))),
    CLI_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS_MODE(OVERRIDE, AllowedListAnonymizer(listOf("enable", "disable"))),
    CLI_VALHALLA_SUPPORT_MODE(OVERRIDE, AllowedListAnonymizer(listOf("none", "primitives", "primitivesAndFullValueClasses", "allValues"))),
    CLI_WHEN_EXPRESSIONS_CODEGEN_MODE(OVERRIDE, AllowedListAnonymizer(listOf("indy", "inline"))),
    CLI_JVM_TARGET_VERSION(OVERRIDE, AllowedListAnonymizer(listOf("1.8") + (9..27).map { it.toString() })),
    CLI_KLIB_ABI_VERSION(OVERRIDE, RegexControlled("\\d+(\\.\\d+)*", false)),
    CLI_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY(OVERRIDE, AllowedListAnonymizer(listOf("deny", "allow-all-with-warning", "allow-first-with-warning"))),
    CLI_KLIB_IR_INLINER_MODE(OVERRIDE, AllowedListAnonymizer(listOf("intra-module", "full", "disabled", "default"))),
    CLI_PARTIAL_LINKAGE_MODE(OVERRIDE, AllowedListAnonymizer(listOf("enable", "disable"))),
    CLI_PARTIAL_LINKAGE_LOG_LEVEL(OVERRIDE, AllowedListAnonymizer(listOf("silent", "info", "warning", "error"))),
    CLI_IR_DCE_RUNTIME_DIAGNOSTIC_MODE(OVERRIDE, AllowedListAnonymizer(listOf("log", "exception"))),
    CLI_MAIN_FUNCTION_EXECUTION_MODE(OVERRIDE, AllowedListAnonymizer(listOf("call", "noCall"))),
    CLI_SOURCE_MAP_EMBED_SOURCES_MODE(OVERRIDE, AllowedListAnonymizer(listOf("always", "never", "inlining"))),
    CLI_SOURCE_MAP_NAMES_POLICY(OVERRIDE, AllowedListAnonymizer(listOf("no", "simple-names", "fully-qualified-names"))),
    CLI_WASM_TARGET_MODE(OVERRIDE, AllowedListAnonymizer(listOf("wasm-js", "wasm-wasi"))),
    CLI_JS_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC_MODE(OVERRIDE, AllowedListAnonymizer(listOf("log", "exception"))),
    CLI_NATIVE_LIGHT_DEBUG_MODE(OVERRIDE, AllowedListAnonymizer(listOf("disable", "enable"))),
    CLI_NATIVE_ALLOCATOR(OVERRIDE, AllowedListAnonymizer(listOf("std", "mimalloc", "custom"))),
    CLI_NATIVE_DEBUG_INFO_FORMAT_VERSION(OVERRIDE, AllowedListAnonymizer(listOf("1", "2"))),
    CLI_NATIVE_DEBUG_TRAMPOLINE_MODE(OVERRIDE, AllowedListAnonymizer(listOf("disable", "enable"))),
    CLI_NATIVE_IR_PROPERTY_LAZY_INITIALIZATION_MODE(OVERRIDE, AllowedListAnonymizer(listOf("disable", "enable"))),
    CLI_NATIVE_PRE_LINK_CACHES_MODE(OVERRIDE, AllowedListAnonymizer(listOf("disable", "enable"))),
    CLI_NATIVE_MEMORY_MODEL(OVERRIDE, AllowedListAnonymizer(listOf("strict", "experimental"))),
    CLI_NATIVE_PRODUCE_OUTPUT_KIND(OVERRIDE, AllowedListAnonymizer(listOf("program", "static", "dynamic", "framework", "library", "bitcode"))),
    CLI_MANUALLY_CONFIGURED_LANGUAGE_FEATURES(OVERRIDE, ARRAY_REGEX),
    CLI_DISABLED_IR_CHECKERS(OVERRIDE, ARRAY_REGEX),
    CLI_ENABLED_ADDITIONAL_IR_CHECKERS(OVERRIDE, ARRAY_REGEX),
    CLI_DISABLED_PHASES(OVERRIDE, ARRAY_REGEX),
    CLI_PHASES_TO_DUMP(OVERRIDE, ARRAY_REGEX),
    CLI_PHASES_TO_DUMP_AFTER(OVERRIDE, ARRAY_REGEX),
    CLI_PHASES_TO_DUMP_BEFORE(OVERRIDE, ARRAY_REGEX),
    CLI_PHASES_TO_VALIDATE(OVERRIDE, ARRAY_REGEX),
    CLI_PHASES_TO_VALIDATE_AFTER(OVERRIDE, ARRAY_REGEX),
    CLI_PHASES_TO_VALIDATE_BEFORE(OVERRIDE, ARRAY_REGEX),
    CLI_VERBOSE_PHASES(OVERRIDE, ARRAY_REGEX),
    CLI_WARNING_LEVELS(OVERRIDE, ARRAY_REGEX),
    CLI_JSR305_NULLABILITY_ANNOTATIONS_MODE(OVERRIDE, ARRAY_REGEX),
    CLI_METADATA_TARGET_PLATFORM(OVERRIDE, ARRAY_REGEX),
    CLI_NATIVE_MANIFEST_TARGETS(OVERRIDE, ARRAY_REGEX);

    companion object {
        const val VERSION = 15
    }
}

private val ARRAY_REGEX: RegexControlled = RegexControlled("\\[.*\\]", anonymizeInIde = false)
