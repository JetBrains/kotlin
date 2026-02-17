/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy.*
import org.jetbrains.kotlin.statistics.metrics.StringOverridePolicy.*


enum class StringMetrics(val type: StringOverridePolicy, val anonymization: StringAnonymizationPolicy, val perProject: Boolean = false) {

    // User environment
    GRADLE_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    PROJECT_PATH(OVERRIDE, RegexControlled("([0-9A-Fa-f]{40,64})|undefined", true)),

    OS_TYPE(OVERRIDE, RegexControlled("(Windows|Windows |Windows Server |Mac|Linux|FreeBSD|Solaris|Other|Mac OS X)\\d*", false)),
    OS_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    IDES_INSTALLED(CONCAT, AllowedListAnonymizer(listOf("AS", "OC", "CL", "IU", "IC", "WC"))),

    // Build script
    MPP_PLATFORMS(
        CONCAT, AllowedListAnonymizer(
            listOf(
                "common",
                "native",
                "jvm",
                "js",
                "android_x64",
                "android_x86",
                "androidJvm",
                "android_arm32",
                "android_arm64",
                "ios_arm64",
                "ios_simulator_arm64",
                "ios_x64",
                "watchos_arm32",
                "watchos_arm64",
                "watchos_x64",
                "watchos_simulator_arm64",
                "watchos_device_arm64",
                "tvos_arm64",
                "tvos_x64",
                "tvos_simulator_arm64",
                "linux_arm32_hfp",
                "linux_arm64",
                "linux_x64",
                "macos_x64",
                "macos_arm64",
                "mingw_x64",
                "wasm"
            )
        )
    ),
    JS_COMPILER_MODE(CONCAT, AllowedListAnonymizer(listOf("ir", "legacy", "both", "UNKNOWN"))),

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
    JS_GENERATE_EXECUTABLE_DEFAULT(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    JS_TARGET_MODE(CONCAT, AllowedListAnonymizer(listOf("both", "browser", "nodejs", "none"))),
    JS_BINARY_TYPE(CONCAT, AllowedListAnonymizer(listOf("both", "library", "executable", "none"))),
    JS_OUTPUT_GRANULARITY(OVERRIDE, RegexControlled("(whole_program|per_module|per_file)", false)),
    JS_ES_TARGET(OVERRIDE, AllowedListAnonymizer(listOf("es5", "es2015", "default"))),
    JS_MODULE_SYSTEM(OVERRIDE, AllowedListAnonymizer(listOf("plain", "amd", "commonjs", "umd", "es", "default"))),

    // Compiler parameters
    JVM_DEFAULTS(CONCAT, AllowedListAnonymizer(listOf("enable", "no-compatibility", "disable"))),
    USE_OLD_BACKEND(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    USE_FIR(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),

    KOTLIN_COMPILER_EXECUTION_POLICY(CONCAT, AllowedListAnonymizer(listOf("in-process", "daemon", "out-of-process"))),
    JS_PROPERTY_LAZY_INITIALIZATION(CONCAT, AllowedListAnonymizer(listOf("true", "false")));


    companion object {
        const val VERSION = 9
    }
}
