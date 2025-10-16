/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalWasmDsl::class)
class UklibFragmentPlatformAttributeTests {

    @Test
    fun `expected platform attributes`() {
        val project = buildProjectWithMPP {
            androidLibrary { compileSdk = 31 }
            kotlin {
                iosArm64("customIos")
                js("customJs")
                wasmJs("customWasmJs")
                wasmWasi("customWasmWasi")
                createExternalKotlinTarget<FakeTarget> { defaults() }.createCompilation<FakeCompilation> { defaults(this@kotlin) }
                jvm("customJvm")
                @Suppress("DEPRECATION")
                androidTarget("customAndroid")
            }
        }.evaluate()

        assertEquals(
            setOf(
                UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndPublishInUmanifest(attribute="android"),
                UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(attribute="ios_arm64"),
                UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(attribute="js_ir"),
                UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(attribute="jvm"),
                UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(attribute="wasm_js"),
                UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest(attribute="wasm_wasi"),
                UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndFailOnPublication(unsupportedTargetName="fake"),
                UklibFragmentPlatformAttribute.FailOnConsumptionAndPublication(metadataTarget=project.multiplatformExtension.metadata() as KotlinMetadataTarget)
            ),
            project.multiplatformExtension.targets.map {
                it.uklibFragmentPlatformAttribute
            }.toSet()
        )
    }

}