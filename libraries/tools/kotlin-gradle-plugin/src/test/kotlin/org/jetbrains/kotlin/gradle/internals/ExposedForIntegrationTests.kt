/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internals

import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset.Companion.NO_NATIVE_STDLIB_PROPERTY_WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset.Companion.NO_NATIVE_STDLIB_WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.targets.native.DisabledNativeTargetsReporter
import org.w3c.dom.Document

fun parseKotlinSourceSetMetadataFromXml(document: Document): KotlinProjectStructureMetadata? =
    org.jetbrains.kotlin.gradle.plugin.mpp.parseKotlinSourceSetMetadataFromXml(document)

const val MULTIPLATFORM_PROJECT_METADATA_FILE_NAME =
    org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_FILE_NAME

const val GRADLE_NO_METADATA_WARNING =
    org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin.GRADLE_NO_METADATA_WARNING

const val DISABLED_NATIVE_TARGETS_REPORTER_DISABLE_WARNING_PROPERTY_NAME =
    DisabledNativeTargetsReporter.DISABLE_WARNING_PROPERTY_NAME

const val DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX: String =
    DisabledNativeTargetsReporter.WARNING_PREFIX

const val NO_NATIVE_STDLIB_WARNING = NO_NATIVE_STDLIB_WARNING
const val NO_NATIVE_STDLIB_PROPERTY_WARNING = NO_NATIVE_STDLIB_PROPERTY_WARNING