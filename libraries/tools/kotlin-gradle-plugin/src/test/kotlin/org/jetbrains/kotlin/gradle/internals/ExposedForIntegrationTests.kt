/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internals

import org.jetbrains.kotlin.compilerRunner.asFinishLogMessage
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DIAGNOSTICS_USE_PARSABLE_FORMATTING
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DIAGNOSTICS_SHOW_STACKTRACE
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DIAGNOSTIC_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CheckKotlinGradlePluginConfigurationErrors
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DIAGNOSTIC_STACKTRACE_END_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DIAGNOSTIC_STACKTRACE_START
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.parseKotlinSourceSetMetadataFromJson
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy

fun parseKotlinSourceSetMetadataFromJson(json: String): KotlinProjectStructureMetadata = parseKotlinSourceSetMetadataFromJson(json)

const val MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME = MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME

val KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS_PROPERTY = KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS

const val VERBOSE_DIAGNOSTIC_SEPARATOR = DIAGNOSTIC_SEPARATOR

const val ENSURE_NO_KOTLIN_GRADLE_PLUGIN_ERRORS_TASK_NAME = CheckKotlinGradlePluginConfigurationErrors.TASK_NAME

val KotlinCompilerExecutionStrategy.asFinishLogMessage: String
    get() = this.asFinishLogMessage

val KOTLIN_INTERNAL_DIAGNOSTICS_USE_PARSABLE_FORMATTING = KOTLIN_INTERNAL_DIAGNOSTICS_USE_PARSABLE_FORMATTING
val KOTLIN_INTERNAL_DIAGNOSTICS_SHOW_STACKTRACE = KOTLIN_INTERNAL_DIAGNOSTICS_SHOW_STACKTRACE
val KOTLIN_DIAGNOSTIC_STACKTRACE_START = DIAGNOSTIC_STACKTRACE_START
val KOTLIN_DIAGNOSTIC_STACKTRACE_END_SEPARATOR = DIAGNOSTIC_STACKTRACE_END_SEPARATOR
