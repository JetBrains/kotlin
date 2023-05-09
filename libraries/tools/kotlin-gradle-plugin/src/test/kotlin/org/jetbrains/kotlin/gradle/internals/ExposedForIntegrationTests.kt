/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internals

import org.jetbrains.kotlin.gradle.plugin.KOTLIN_12X_MPP_DEPRECATION_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DIAGNOSTIC_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.parseKotlinSourceSetMetadataFromJson
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter

fun parseKotlinSourceSetMetadataFromJson(json: String): KotlinProjectStructureMetadata? = parseKotlinSourceSetMetadataFromJson(json)

const val MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME = MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME

const val DISABLED_NATIVE_TARGETS_REPORTER_DISABLE_WARNING_PROPERTY_NAME = DisabledNativeTargetsReporter.DISABLE_WARNING_PROPERTY_NAME

const val DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX: String = DisabledNativeTargetsReporter.WARNING_PREFIX

val KOTLIN_12X_MPP_DEPRECATION_WARNING = KOTLIN_12X_MPP_DEPRECATION_WARNING

const val VERBOSE_DIAGNOSTIC_SEPARATOR = DIAGNOSTIC_SEPARATOR
