/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * This sealed class is intended to help in the mapping between KotlinTarget and the Uklib attribute that will be recorded in the Umanifest
 */
internal sealed class UklibFragmentPlatformAttribute {
    /**
     * Jvm, native and JS targets are published
     */
    data class ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(val attribute: String) : UklibFragmentPlatformAttribute()

    /**
     * Android target consumption is supported; however in platform compile dependency configuration we must resolve aar and the platform
     * attribute should only be used during GMT and in publication of an Umanifest
     */
    data class ConsumeInMetadataCompilationsAndPublishInUmanifest(val attribute: String) : UklibFragmentPlatformAttribute()

    /**
     * External target is not currently supported. It is not ever published in Umanifest, but we must use some unique attribute during GMT
     * to produce a correct metadata classpath.
     */
    data class ConsumeInMetadataCompilationsAndFailOnPublication(val unsupportedTargetName: String) : UklibFragmentPlatformAttribute()

    /**
     * This case should only be used for metadata target
     */
    data class FailOnConsumptionAndPublication(val metadataTarget: KotlinMetadataTarget) : UklibFragmentPlatformAttribute()

    fun convertToStringForPublicationInUmanifest(): String = when (this) {
        is ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest -> attribute
        is ConsumeInMetadataCompilationsAndPublishInUmanifest -> attribute
        is ConsumeInMetadataCompilationsAndFailOnPublication -> error("Publication with ${unsupportedTargetName} is not supported")
        is FailOnConsumptionAndPublication -> error("${metadataTarget} doesn't have a platform attribute for publication")
    }

    /**
     * Convert the attribute for consumption in transforms or GMT
     */
    fun convertToStringForConsumption(): String = when (this) {
        is ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest -> attribute
        is ConsumeInMetadataCompilationsAndPublishInUmanifest -> attribute
        is ConsumeInMetadataCompilationsAndFailOnPublication -> unsupportedTargetName
        is FailOnConsumptionAndPublication -> error("${metadataTarget} doesn't have a platform attribute for consumption")
    }
}

/**
 * This is the per-target attribute that will be recorded within the fragment of the umanifest
 *
 * When resolving the uklib we will use this attribute
 *  - In the transform for platform compilation
 *  - In GMT for metadata classpath formation
 */
internal val KotlinCompilation<*>.uklibFragmentPlatformAttribute: UklibFragmentPlatformAttribute
    get() = this.target.uklibFragmentPlatformAttribute
internal val KotlinTarget.uklibFragmentPlatformAttribute: UklibFragmentPlatformAttribute
    get() {
        if (this is KotlinMetadataTarget) {
            return UklibFragmentPlatformAttribute.FailOnConsumptionAndPublication(this)
        }

        /**
         * FIXME: Android configurations currently do not resolve consistently with the targets below
         * FIXME: Request jvm transform in Android?
         */
        if (this is KotlinAndroidTarget) {
            return UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndPublishInUmanifest(
                UklibTargetFragmentAttribute.android.name
            )
        }

        val supportedUklibTarget = when (this) {
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
        }
        if (supportedUklibTarget != null) {
            return UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(supportedUklibTarget)
        }

        // FIXME: This is a temporary (KT-81394) workaround before proper external target support (KT-77074)
        if (targetName == "android") {
            return UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndPublishInUmanifest(targetName)
        } else {
            return UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndFailOnPublication(targetName)
        }
    }

/**
 * These attribute names will be recorded in and resolved from the Umanifest
 */
internal enum class UklibTargetFragmentAttribute {
    js_ir,
    wasm_js,
    wasm_wasi,
    jvm,
    android;
}