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
                "ios_arm32",
                "ios_arm64",
                "ios_simulator_arm64",
                "ios_x64",
                "watchos_arm32",
                "watchos_arm64",
                "watchos_x86",
                "watchos_x64",
                "watchos_simulator_arm64",
                "watchos_device_arm64",
                "tvos_arm64",
                "tvos_x64",
                "tvos_simulator_arm64",
                "linux_arm32_hfp",
                "linux_mips32",
                "linux_mipsel32",
                "linux_arm64",
                "linux_x64",
                "macos_x64",
                "macos_arm64",
                "mingw_x64",
                "mingw_x86",
                "wasm32",
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

    KOTLIN_COMPILER_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_STDLIB_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_REFLECT_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_COROUTINES_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_SERIALIZATION_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    ANDROID_GRADLE_PLUGIN_VERSION(OVERRIDE, ComponentVersionAnonymizer()),

    // Features
    KOTLIN_LANGUAGE_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    KOTLIN_API_VERSION(OVERRIDE, ComponentVersionAnonymizer()),
    USE_CLASSPATH_SNAPSHOT(CONCAT, AllowedListAnonymizer(listOf("true", "false", "default-true"))),
    JS_GENERATE_EXECUTABLE_DEFAULT(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    JS_TARGET_MODE(CONCAT, AllowedListAnonymizer(listOf("both", "browser", "nodejs", "none"))),
    JS_OUTPUT_GRANULARITY(OVERRIDE, RegexControlled("(whole_program|per_module|per_file)", false)),

    // Compiler parameters
    JVM_DEFAULTS(CONCAT, AllowedListAnonymizer(listOf("disable", "enable", "compatibility", "all", "all-compatibility"))),
    USE_OLD_BACKEND(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    USE_FIR(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),

    JS_PROPERTY_LAZY_INITIALIZATION(CONCAT, AllowedListAnonymizer(listOf("true", "false")));

    companion object {
        const val VERSION = 1
    }
}
