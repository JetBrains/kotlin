/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.dsl.KotlinSourceSetConvention.isRegisteredByKotlinSourceSetConventionAt
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
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.Family

internal object PlatformSourceSetConventionsChecker : KotlinGradleProjectChecker {
    data class CheckedPlatformInfo(
        val expectedTargetName: String,
        val expectedTargetType: Class<out KotlinTarget>,
    )

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        listOf(
            CheckedPlatformInfo("js", KotlinJsTargetDsl::class.java),
            CheckedPlatformInfo("jvm", KotlinJvmTarget::class.java)
        ).forEach { checkedPlatformInfo -> runChecks(checkedPlatformInfo) }
    }

    private suspend fun KotlinGradleProjectCheckerContext.runChecks(platform: CheckedPlatformInfo) {
        val kotlin = project.multiplatformExtensionOrNull ?: return
        val sourceSets = kotlin.awaitSourceSets()

        /* Find jvmMain and jvmTest source sets that have been registered by the convention */
        val platformSourceSets = listOfNotNull(
            sourceSets.findByName("${platform.expectedTargetName}Main"),
            sourceSets.findByName("${platform.expectedTargetName}Test")
        )
            .filter { it.isRegisteredByKotlinSourceSetConventionAt != null }
            .ifEmpty { return }

        /* Check if a custom target name was used */
        val customNamedTarget = kotlin.targets.withType(platform.expectedTargetType)
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
        if (androidMainSourceSet.isRegisteredByKotlinSourceSetConventionAt == null) return
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
            .filter { it.isRegisteredByKotlinSourceSetConventionAt != null }


        val hasIosTarget = kotlin.awaitTargets()
            .any { target -> target is KotlinNativeTarget && target.konanTarget.family == Family.IOS }

        if (!hasIosTarget) {
            iosSourceSets.forEach { sourceSet ->
                project.reportDiagnostic(IosSourceSetConventionUsedWithoutIosTarget(sourceSet))
            }
        }

    }
}
