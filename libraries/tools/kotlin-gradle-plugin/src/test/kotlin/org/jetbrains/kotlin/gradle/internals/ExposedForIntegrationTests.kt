/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internals

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DIAGNOSTIC_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.parseKotlinSourceSetMetadataFromJson

fun parseKotlinSourceSetMetadataFromJson(json: String): KotlinProjectStructureMetadata = parseKotlinSourceSetMetadataFromJson(json)

const val MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME = MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME

const val KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS_PROPERTY = KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS

const val VERBOSE_DIAGNOSTIC_SEPARATOR = DIAGNOSTIC_SEPARATOR
