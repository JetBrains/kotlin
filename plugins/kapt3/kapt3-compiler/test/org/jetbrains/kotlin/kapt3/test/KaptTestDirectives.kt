/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.kapt3.base.KaptFlag
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object KaptTestDirectives : SimpleDirectivesContainer() {
    val SHOW_PROCESSOR_STATS by directive("Enables SHOW_PROCESSOR_STATS flag")
    val VERBOSE by directive("Enables VERBOSE flag")
    val INFO_AS_WARNINGS by directive("Enables INFO_AS_WARNINGS flag")
    val USE_LIGHT_ANALYSIS by directive("Enables USE_LIGHT_ANALYSIS flag")
    val CORRECT_ERROR_TYPES by directive("Enables CORRECT_ERROR_TYPES flag")
    val DUMP_DEFAULT_PARAMETER_VALUES by directive("Enables DUMP_DEFAULT_PARAMETER_VALUES flag")
    val MAP_DIAGNOSTIC_LOCATIONS by directive("Enables MAP_DIAGNOSTIC_LOCATIONS flag")
    val STRICT by directive("Enables STRICT flag")
    val INCLUDE_COMPILE_CLASSPATH by directive("Enables INCLUDE_COMPILE_CLASSPATH flag")
    val INCREMENTAL_APT by directive("Enables INCREMENTAL_APT flag")
    val STRIP_METADATA by directive("Enables STRIP_METADATA flag")
    val KEEP_KDOC_COMMENTS_IN_STUBS by directive("Enables KEEP_KDOC_COMMENTS_IN_STUBS flag")

    val DISABLED_FLAGS by enumDirective<KaptFlag>("Disables listed flags")

    val NON_EXISTENT_CLASS by directive("TODO")
    val NO_VALIDATION by directive("TODO")
    val EXPECTED_ERROR by stringDirective("TODO()", multiLine = true)

    val flagDirectives = listOf(
        SHOW_PROCESSOR_STATS, VERBOSE, INFO_AS_WARNINGS, USE_LIGHT_ANALYSIS, CORRECT_ERROR_TYPES,
        DUMP_DEFAULT_PARAMETER_VALUES, MAP_DIAGNOSTIC_LOCATIONS, STRICT, INCLUDE_COMPILE_CLASSPATH,
        INCREMENTAL_APT, STRIP_METADATA, KEEP_KDOC_COMMENTS_IN_STUBS,
    )
}
