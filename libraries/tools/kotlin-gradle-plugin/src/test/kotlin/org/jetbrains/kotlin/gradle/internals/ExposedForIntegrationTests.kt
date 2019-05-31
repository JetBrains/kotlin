/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internals

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.w3c.dom.Document

fun parseKotlinSourceSetMetadataFromXml(document: Document): KotlinProjectStructureMetadata? =
    org.jetbrains.kotlin.gradle.plugin.mpp.parseKotlinSourceSetMetadataFromXml(document)

const val MULTIPLATFORM_PROJECT_METADATA_FILE_NAME =
    org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_FILE_NAME