/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal sealed class UklibFragmentPlatformAttribute {
    // Jvm, native, js
    data class PublishAndConsumeInAllCompilations(val attribute: String) : UklibFragmentPlatformAttribute()

    // Android
    data class PublishAndConsumeInMetadataCompilations(val attribute: String) : UklibFragmentPlatformAttribute()

    // External target
    data class FailOnPublicationAndUseTargetNameForMetadataCompilations(val unsupportedTargetName: String) : UklibFragmentPlatformAttribute()

    fun safeToPublish(): String = when (this) {
        is PublishAndConsumeInAllCompilations -> attribute
        is PublishAndConsumeInMetadataCompilations -> attribute
        is FailOnPublicationAndUseTargetNameForMetadataCompilations -> error("Publication with ${unsupportedTargetName} is not supported")
    }

    fun safeToConsume(): String = when (this) {
        is PublishAndConsumeInAllCompilations -> attribute
        is PublishAndConsumeInMetadataCompilations -> attribute
        is FailOnPublicationAndUseTargetNameForMetadataCompilations -> unsupportedTargetName
    }
}

/**
 * The fragment identifier is recorded as umanifest fragment identifier and as the fragment directory name in the archive
 */
internal val KotlinSourceSet.fragmentIdentifier: String
    get() = name
internal val KotlinCompilation<*>.fragmentIdentifier: String
    get() = defaultSourceSet.fragmentIdentifier

/**
 * This is the per-target attribute that will be recorded within the fragment of the umanifest
 *
 * When resolving the uklib we will use this attribute
 *  - In the transform for platform compilation
 *  - In GMT for metadata compilations
 */
internal val KotlinCompilation<*>.uklibFragmentPlatformAttribute: UklibFragmentPlatformAttribute
    get() = this.target.uklibFragmentPlatformAttribute
internal val KotlinTarget.uklibFragmentPlatformAttribute: UklibFragmentPlatformAttribute
    get() {
        if (this is KotlinMetadataTarget) {
            error("FIXME: Uklib fragment attribute requested for metadata target, report to youtrack")
        }

        // FIXME: Request jvm transform in Android?
        if (this is KotlinAndroidTarget) {
            return UklibFragmentPlatformAttribute.PublishAndConsumeInMetadataCompilations(
                UklibTargetFragmentAttribute.android.name
            )
        }

        when (this) {
            is KotlinNativeTarget -> konanTarget.name
            is KotlinJsIrTarget -> when (platformType) {
                KotlinPlatformType.js -> UklibTargetFragmentAttribute.js_ir.name
                KotlinPlatformType.wasm -> when (wasmTargetType ?: error("${KotlinJsIrTarget::class} missing wasm type in wasm platform ")) {
                    KotlinWasmTargetType.JS -> UklibTargetFragmentAttribute.wasm_js.name
                    KotlinWasmTargetType.WASI -> UklibTargetFragmentAttribute.wasm_wasi.name
                }
                else -> error("${KotlinJsIrTarget::class} unexpected platform type $platformType")
            }
            is KotlinJvmTarget -> UklibTargetFragmentAttribute.jvm.name
            else -> null
        }?.let { attribute ->
            return UklibFragmentPlatformAttribute.PublishAndConsumeInAllCompilations(attribute)
        }

        val unsupportedTargetName = when (this) {
            is DecoratedExternalKotlinTarget -> "external target"
            else -> this.targetName
        }
        return UklibFragmentPlatformAttribute.FailOnPublicationAndUseTargetNameForMetadataCompilations(unsupportedTargetName)
    }

internal val KotlinSourceSet.metadataFragmentAttributes: Set<UklibFragmentPlatformAttribute>
    get() = internal.compilations.filterNot {
        it is KotlinMetadataCompilation
    }.map {
        it.uklibFragmentPlatformAttribute
    }.toSet()
internal val KotlinCompilation<*>.metadataFragmentAttributes: Set<UklibFragmentPlatformAttribute>
    get() = defaultSourceSet.metadataFragmentAttributes

private enum class UklibTargetFragmentAttribute {
    js_ir,
    wasm_js,
    wasm_wasi,
    jvm,
    android;
}