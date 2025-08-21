/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

/**
 * FIXME: It is desirable that these attributes remain manually editable, but they must also be up-to-date with actually published attributes.
 * add a functional or integration test to make sure these attributes don't become stale.
 */
val uklibTransformationIosArm64Attributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "ios_arm64",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)
val uklibTransformationJvmAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "jvm",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)
val uklibTransformationMetadataAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "whole_uklib",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)
val uklibTransformationJsAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "js_ir",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)
val uklibTransformationWasmJsAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "wasm_js",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)
val uklibTransformationWasmWasiAttributes = mapOf(
    "artifactType" to "uklib",
    "org.jetbrains.kotlin.uklibView" to "wasm_wasi",
    "org.jetbrains.kotlin.uklibState" to "decompressed",
)

val jvmRuntimeAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "java-runtime",
)
val jvmApiAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "java-api",
)

val kmpJvmRuntimeVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "standard-jvm",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "java-runtime",
    "org.jetbrains.kotlin.platform.type" to "jvm",
)
val kmpJvmApiVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "standard-jvm",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "java-api",
    "org.jetbrains.kotlin.platform.type" to "jvm",
)

val kmpMetadataVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-metadata",
    "org.jetbrains.kotlin.platform.type" to "common",
)
val kmpIosArm64MetadataVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-metadata",
    "org.jetbrains.kotlin.native.target" to "ios_arm64",
    "org.jetbrains.kotlin.platform.type" to "native"
)
val kmpIosX64MetadataVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-metadata",
    "org.jetbrains.kotlin.native.target" to "ios_x64",
    "org.jetbrains.kotlin.platform.type" to "native"
)

val kmpJsVariantAttributes = mutableMapOf(
    "artifactType" to "org.jetbrains.kotlin.klib",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-api",
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
    "org.jetbrains.kotlin.js.compiler" to "ir",
    "org.jetbrains.kotlin.platform.type" to "js",
)
val kmpWasmJsVariantAttributes = mutableMapOf(
    "artifactType" to "org.jetbrains.kotlin.klib",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-api",
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
    "org.jetbrains.kotlin.platform.type" to "wasm",
    "org.jetbrains.kotlin.wasm.target" to "js",
)

val kmpIosArm64VariantAttributes = mapOf(
    "artifactType" to "org.jetbrains.kotlin.klib",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-api",
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
    "org.jetbrains.kotlin.native.target" to "ios_arm64",
    "org.jetbrains.kotlin.platform.type" to "native",
)
val kmpIosX64VariantAttributes = mapOf(
    "artifactType" to "org.jetbrains.kotlin.klib",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-api",
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
    "org.jetbrains.kotlin.native.target" to "ios_x64",
    "org.jetbrains.kotlin.platform.type" to "native",
)

val libraryElementsJar = mapOf(
    "org.gradle.libraryelements" to "jar",
)

val packed = mapOf(
    "org.jetbrains.kotlin.klib.packaging" to "packed",
)
// In most publications we override artifactType as "org.jetbrains.kotlin.klib" at variant-level
val klibArtifact = mapOf(
    "artifactType" to "klib",
)
val jarArtifact = mapOf(
    "artifactType" to "jar",
)
val uklibArtifact = mapOf(
    "artifactType" to "uklib",
)
val klibCinteropCommonizerType = mapOf(
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
)

val notAMetadataJar = mapOf(
    "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
)
val maybeAMetadataJar = mapOf(
    "org.jetbrains.kotlin.isMetadataJar" to "unknown",
)