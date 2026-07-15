/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy.AllowedListAnonymizer
import org.jetbrains.kotlin.statistics.metrics.StringListOverridePolicy.CONCAT

enum class StringListMetrics(
    val type: StringListOverridePolicy,
    val anonymization: StringAnonymizationPolicy,
    val perProject: Boolean = false,
) {
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
    JS_GENERATE_EXECUTABLE_DEFAULT(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    JS_TARGET_MODE(CONCAT, AllowedListAnonymizer(listOf("both", "browser", "nodejs", "none"))),
    JS_BINARY_TYPE(CONCAT, AllowedListAnonymizer(listOf("both", "library", "executable", "none"))),

    WASM_COMPILER_MODE(
        CONCAT,
        AllowedListAnonymizer(
            listOf(
                "monolith",
                "multimodule-open-world",
                "multimodule-closed-world",
                "multimodule-closed-world-only-in-dev"
            )
        )
    ),

    // Compiler parameters
    JVM_DEFAULTS(CONCAT, AllowedListAnonymizer(listOf("enable", "no-compatibility", "disable"))),
    USE_OLD_BACKEND(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    USE_FIR(CONCAT, AllowedListAnonymizer(listOf("true", "false"))),
    KOTLIN_COMPILER_EXECUTION_POLICY(CONCAT, AllowedListAnonymizer(listOf("in-process", "daemon"))),
    JS_PROPERTY_LAZY_INITIALIZATION(CONCAT, AllowedListAnonymizer(listOf("true", "false")));


    companion object {
        const val VERSION = 3
    }
}
