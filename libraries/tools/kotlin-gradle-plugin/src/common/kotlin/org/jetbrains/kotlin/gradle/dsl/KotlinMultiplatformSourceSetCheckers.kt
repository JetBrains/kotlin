/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.dsl.KotlinSourceSetConvention.isAccessedByKotlinSourceSetConventionAt
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.AndroidMainSourceSetConventionUsedWithoutAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.IosSourceSetConventionUsedWithoutIosTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.PlatformSourceSetConventionUsedWithCustomTargetName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.PlatformSourceSetConventionUsedWithoutCorrespondingTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName

internal object PlatformSourceSetConventionsChecker : KotlinGradleProjectChecker {
    data class CheckedPlatformInfo<T: KotlinTarget>(
        val expectedTargetName: String,
        val expectedTargetType: Class<T>,
        val matches: (T) -> Boolean,
    )

    inline fun <reified T : KotlinTarget> CheckedPlatformInfo(
        expectedTargetName: String,
        noinline matches: (T) -> Boolean = { true }
    ) = CheckedPlatformInfo(
        expectedTargetName = expectedTargetName,
        expectedTargetType = T::class.java,
        matches = matches
    )

    private val nativeTargetPresets get() = HostManager()
            .targetValues
            .map { konanTarget -> CheckedPlatformInfo<KotlinNativeTarget>(konanTarget.presetName) { it.konanTarget == konanTarget } }

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        listOf(
            CheckedPlatformInfo<KotlinJsTargetDsl>("js"),
            CheckedPlatformInfo<KotlinJvmTarget>("jvm"),
            CheckedPlatformInfo<KotlinWasmJsTargetDsl>("wasmJs"),
            CheckedPlatformInfo<KotlinWasmWasiTargetDsl>("wasmJs"),
        ).plus(nativeTargetPresets).forEach { checkedPlatformInfo ->
            @Suppress("UNCHECKED_CAST")
            runChecks(checkedPlatformInfo as CheckedPlatformInfo<KotlinTarget>)
        }
    }

    private suspend fun KotlinGradleProjectCheckerContext.runChecks(platform: CheckedPlatformInfo<KotlinTarget>) {
        val kotlin = project.multiplatformExtensionOrNull ?: return
        val sourceSets = kotlin.awaitSourceSets()

        /* Find Main and Test source sets that have been registered by the convention */
        val platformSourceSets = listOfNotNull(
            sourceSets.findByName("${platform.expectedTargetName}Main"),
            sourceSets.findByName("${platform.expectedTargetName}Test")
        )
            .filter { it.isAccessedByKotlinSourceSetConventionAt != null }
            .ifEmpty { return }

        /* Check if a custom target name was used */
        val customNamedTarget = kotlin.targets.withType(platform.expectedTargetType)
            .filter { target -> platform.matches(target) }
            .firstOrNull { target -> target.name != platform.expectedTargetName }

        if (customNamedTarget != null) {
            platformSourceSets.forEach { sourceSet ->
                project.reportDiagnostic(
                    PlatformSourceSetConventionUsedWithCustomTargetName(sourceSet, customNamedTarget, platform.expectedTargetName)
                )
            }
        } else if (kotlin.targets.findByName(platform.expectedTargetName) == null) {
            platformSourceSets.forEach { sourceSet ->
                project.reportDiagnostic(
                    PlatformSourceSetConventionUsedWithoutCorrespondingTarget(sourceSet, platform.expectedTargetName)
                )
            }
        }
    }
}

internal object AndroidMainSourceSetConventionsChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val kotlin = project.multiplatformExtensionOrNull ?: return
        val androidMainSourceSet = kotlin.awaitSourceSets().findByName("androidMain") ?: return
        if (androidMainSourceSet.isAccessedByKotlinSourceSetConventionAt == null) return
        val androidTarget = kotlin.awaitTargets().findByName("android")
        if (androidTarget == null) {
            project.reportDiagnostic(AndroidMainSourceSetConventionUsedWithoutAndroidTarget(androidMainSourceSet))
        }
    }
}

internal object IosSourceSetConventionChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val kotlin = project.multiplatformExtensionOrNull ?: return

        val iosSourceSets = listOf("iosMain", "iosTest")
            .mapNotNull { sourceSetName -> kotlin.awaitSourceSets().findByName(sourceSetName) }
            .filter { it.isAccessedByKotlinSourceSetConventionAt != null }


        val hasIosTarget = kotlin.awaitTargets()
            .any { target -> target is KotlinNativeTarget && target.konanTarget.family == Family.IOS }

        if (!hasIosTarget) {
            iosSourceSets.forEach { sourceSet ->
                project.reportDiagnostic(IosSourceSetConventionUsedWithoutIosTarget(sourceSet))
            }
        }

    }
}
