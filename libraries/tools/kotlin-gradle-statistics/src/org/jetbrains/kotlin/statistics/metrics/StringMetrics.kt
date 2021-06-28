/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy.*
import org.jetbrains.kotlin.statistics.metrics.StringOverridePolicy.*


enum class StringMetrics(val type: StringOverridePolicy, val anonymization: StringAnonymizationPolicy, val perProject: Boolean = false) {

    // User environment
    GRADLE_VERSION(OVERRIDE, COMPONENT_VERSION),
    PROJECT_PATH(OVERRIDE, ANONYMIZE_IN_IDE),

    OS_TYPE(OVERRIDE, SAFE),

    //TODO could we collect only JB IDEs or, e.g. WsCode?
    IDES_INSTALLED(CONCAT, SAFE),

    // Build script
    MPP_PLATFORMS(CONCAT, SAFE),
    JS_COMPILER_MODE(CONCAT, SAFE),

    // Component versions
    LIBRARY_SPRING_VERSION(OVERRIDE_VERSION_IF_NOT_SET, COMPONENT_VERSION),
    LIBRARY_VAADIN_VERSION(OVERRIDE_VERSION_IF_NOT_SET, COMPONENT_VERSION),
    LIBRARY_GWT_VERSION(OVERRIDE_VERSION_IF_NOT_SET, COMPONENT_VERSION),
    LIBRARY_HIBERNATE_VERSION(OVERRIDE_VERSION_IF_NOT_SET, COMPONENT_VERSION),

    KOTLIN_COMPILER_VERSION(OVERRIDE, COMPONENT_VERSION),
    KOTLIN_STDLIB_VERSION(OVERRIDE, COMPONENT_VERSION),
    KOTLIN_REFLECT_VERSION(OVERRIDE, COMPONENT_VERSION),
    KOTLIN_COROUTINES_VERSION(OVERRIDE, COMPONENT_VERSION),
    KOTLIN_SERIALIZATION_VERSION(OVERRIDE, COMPONENT_VERSION),

    ANDROID_GRADLE_PLUGIN_VERSION(OVERRIDE, COMPONENT_VERSION),

    // Features
    KOTLIN_LANGUAGE_VERSION(OVERRIDE, COMPONENT_VERSION),
    KOTLIN_API_VERSION(OVERRIDE, COMPONENT_VERSION),
    JS_GENERATE_EXECUTABLE_DEFAULT(CONCAT, SAFE),
    JS_TARGET_MODE(CONCAT, SAFE),

    // Compiler parameters
    JVM_DEFAULTS(CONCAT, SAFE),
    USE_OLD_BACKEND(CONCAT, SAFE),

    JS_PROPERTY_LAZY_INITIALIZATION(CONCAT, SAFE),
}
