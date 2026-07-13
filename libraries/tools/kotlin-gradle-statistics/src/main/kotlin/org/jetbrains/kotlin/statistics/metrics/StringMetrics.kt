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
    JS_ES_TARGET(OVERRIDE, AllowedListAnonymizer(listOf("es5", "es2015", "default"))),
    JS_MODULE_SYSTEM(OVERRIDE, AllowedListAnonymizer(listOf("plain", "amd", "commonjs", "umd", "es", "default")));

    companion object {
        const val VERSION = 12
    }
}
