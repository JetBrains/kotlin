/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinSourceSetConvention.isRegisteredByKotlinSourceSetConventionAt
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
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

@KotlinGradlePluginDsl
interface KotlinMultiplatformSourceSetConventions {
    val NamedDomainObjectContainer<KotlinSourceSet>.commonMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.commonTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.nativeMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.nativeTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.appleMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.appleTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.iosMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.iosTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.tvosMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.tvosTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.watchosMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.watchosTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.macosMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.macosTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.linuxMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.linuxTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.mingwMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.mingwTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.androidNativeMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.androidNativeTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.jvmMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.jvmTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.jsMain: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.jsTest: NamedDomainObjectProvider<KotlinSourceSet>
    val NamedDomainObjectContainer<KotlinSourceSet>.androidMain: NamedDomainObjectProvider<KotlinSourceSet>

    operator fun NamedDomainObjectProvider<KotlinSourceSet>.invoke(
        configure: KotlinSourceSet.() -> Unit,
    ): Unit = get().run(configure)

    fun NamedDomainObjectProvider<KotlinSourceSet>.dependencies(
        handler: KotlinDependencyHandler.() -> Unit,
    ): Unit = get().dependencies(handler)

    fun NamedDomainObjectProvider<KotlinSourceSet>.languageSettings(
        configure: LanguageSettingsBuilder.() -> Unit,
    ): Unit = this { languageSettings(configure) }
}

/* Implementation */

internal object KotlinMultiplatformSourceSetConventionsImpl : KotlinMultiplatformSourceSetConventions {
    override val NamedDomainObjectContainer<KotlinSourceSet>.commonMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.commonTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.nativeMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.nativeTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.appleMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.appleTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.iosMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.iosTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.tvosMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.tvosTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.watchosMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.watchosTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.macosMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.macosTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.linuxMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.linuxTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.mingwMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.mingwTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.androidNativeMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.androidNativeTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.jvmMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.jvmTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.jsMain by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.jsTest by KotlinSourceSetConvention
    override val NamedDomainObjectContainer<KotlinSourceSet>.androidMain by KotlinSourceSetConvention
}

/* Checkers */

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
