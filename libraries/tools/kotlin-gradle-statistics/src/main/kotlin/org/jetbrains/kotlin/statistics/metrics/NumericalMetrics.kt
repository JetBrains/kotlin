/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.metrics.NumberAnonymizationPolicy.*
import org.jetbrains.kotlin.statistics.metrics.NumberOverridePolicy.*


enum class NumericalMetrics(val type: NumberOverridePolicy, val anonymization: NumberAnonymizationPolicy, val perProject: Boolean = false) {


    // User environment
    // Number of CPU cores. No other information (e.g. env.PROCESSOR_IDENTIFIER is not reported)
    CPU_NUMBER_OF_CORES(OVERRIDE, SAFE),

    //Download speed in Bytes per second
    ARTIFACTS_DOWNLOAD_SPEED(OVERRIDE, RANDOM_10_PERCENT),

    // Build script
    GRADLE_DAEMON_HEAP_SIZE(OVERRIDE, RANDOM_10_PERCENT),

    GRADLE_BUILD_NUMBER_IN_CURRENT_DAEMON(OVERRIDE, SAFE),

    // gradle configuration types
    CONFIGURATION_API_COUNT(SUM, RANDOM_10_PERCENT),
    CONFIGURATION_IMPLEMENTATION_COUNT(SUM, RANDOM_10_PERCENT),
    CONFIGURATION_COMPILE_COUNT(SUM, RANDOM_10_PERCENT),
    CONFIGURATION_RUNTIME_COUNT(SUM, RANDOM_10_PERCENT),

    // gradle task types
    GRADLE_NUMBER_OF_TASKS(SUM, RANDOM_10_PERCENT),
    GRADLE_NUMBER_OF_UNCONFIGURED_TASKS(SUM, RANDOM_10_PERCENT),
    GRADLE_NUMBER_OF_INCREMENTAL_TASKS(SUM, RANDOM_10_PERCENT),

    //Features
    BUILD_SRC_COUNT(SUM, RANDOM_10_PERCENT),

    // Build performance
    // duration of the whole gradle build
    GRADLE_BUILD_DURATION(OVERRIDE, SAFE),

    //duration of the execution gradle phase
    GRADLE_EXECUTION_DURATION(OVERRIDE, SAFE),

    //performance of compiler
    COMPILATIONS_COUNT(SUM, RANDOM_10_PERCENT),
    INCREMENTAL_COMPILATIONS_COUNT(SUM, RANDOM_10_PERCENT),
    COMPILATION_DURATION(SUM, SAFE),
    COMPILED_LINES_OF_CODE(SUM, RANDOM_10_PERCENT),
    COMPILATION_LINES_PER_SECOND(OVERRIDE, SAFE),
    ANALYSIS_LINES_PER_SECOND(AVERAGE, SAFE),
    CODE_GENERATION_LINES_PER_SECOND(AVERAGE, SAFE),

    NUMBER_OF_SUBPROJECTS(SUM, RANDOM_10_PERCENT),

    STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD(SUM, RANDOM_10_PERCENT),
    STATISTICS_COLLECT_METRICS_OVERHEAD(SUM, RANDOM_10_PERCENT),

    // User scenarios

    // this value is not reported, only time intervals from the previous build are used
    BUILD_FINISH_TIME(OVERRIDE, SAFE);

    companion object {
        const val VERSION = 1
    }
}
