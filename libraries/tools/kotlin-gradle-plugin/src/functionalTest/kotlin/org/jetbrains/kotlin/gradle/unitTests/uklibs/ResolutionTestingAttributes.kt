/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

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
val uklibVariantAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.usage" to "kotlin-uklib-api",
)
val jvmRuntimeAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.status" to "release",
    "org.gradle.usage" to "java-runtime",
)
val jvmApiAttributes = mapOf(
    "org.gradle.category" to "library",
    "org.gradle.libraryelements" to "jar",
    "org.gradle.status" to "release",
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
    "org.gradle.libraryelements" to "jar",
    "org.gradle.usage" to "kotlin-metadata",
    "org.jetbrains.kotlin.platform.type" to "common",
)
val releaseStatus = mapOf(
    "org.gradle.status" to "release",
)

// We only emit packing in secondary variants which are not published?
val nonPacked = mapOf(
    "org.jetbrains.kotlin.klib.packaging" to "non-packed",
)
val jarArtifact = mapOf(
    "artifactType" to "jar",
)
val uklibArtifact = mapOf(
    "artifactType" to "uklib",
)
val platformIosArm64Attributes = mapOf(
    "artifactType" to "org.jetbrains.kotlin.klib",
    "org.gradle.category" to "library",
    "org.gradle.jvm.environment" to "non-jvm",
    "org.gradle.usage" to "kotlin-api",
    "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
    "org.jetbrains.kotlin.native.target" to "ios_arm64",
    "org.jetbrains.kotlin.platform.type" to "native",
)