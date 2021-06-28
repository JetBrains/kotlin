/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.metrics.BooleanAnonymizationPolicy.*
import org.jetbrains.kotlin.statistics.metrics.BooleanOverridePolicy.*


enum class BooleanMetrics(val type: BooleanOverridePolicy, val anonymization: BooleanAnonymizationPolicy, val perProject: Boolean = false) {

    // whether the build is executed from IDE or from console
    EXECUTED_FROM_IDEA(OVERRIDE, SAFE),

    // Build script

    //annotation processors
    ENABLED_KAPT(OR, SAFE),
    ENABLED_DAGGER(OR, SAFE),
    ENABLED_DATABINDING(OR, SAFE),

    ENABLED_COMPILER_PLUGIN_ALL_OPEN(OR, SAFE),
    ENABLED_COMPILER_PLUGIN_NO_ARG(OR, SAFE),
    ENABLED_COMPILER_PLUGIN_JPA_SUPPORT(OR, SAFE),
    ENABLED_COMPILER_PLUGIN_SAM_WITH_RECEIVER(OR, SAFE),

    ENABLED_HMPP(OR, SAFE),

    // Enabled features
    BUILD_SRC_EXISTS(OR, SAFE),
    GRADLE_BUILD_CACHE_USED(OVERRIDE, SAFE),
    GRADLE_WORKER_API_USED(OVERRIDE, SAFE),

    KOTLIN_OFFICIAL_CODESTYLE(OVERRIDE, SAFE),
    KOTLIN_PROGRESSIVE_MODE(OVERRIDE, SAFE),
    KOTLIN_KTS_USED(OR, SAFE),
    JVM_COMPILER_IR_MODE(OR, SAFE),

    JS_GENERATE_EXTERNALS(OR, SAFE),

    JS_SOURCE_MAP(OR, SAFE),

    JS_KLIB_INCREMENTAL(OR, SAFE),
    JS_IR_INCREMENTAL(OR, SAFE),

    // User scenarios
    DEBUGGER_ENABLED(OVERRIDE, SAFE),
    COMPILATION_STARTED(OVERRIDE, SAFE),
    TESTS_EXECUTED(OVERRIDE, SAFE),
    MAVEN_PUBLISH_EXECUTED(OVERRIDE, SAFE),
    BUILD_FAILED(OVERRIDE, SAFE)
}
