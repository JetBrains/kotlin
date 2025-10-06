/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.GradleDeprecatedPropertyChecker.DeprecatedProperty
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

internal object GradleDeprecatedPropertyChecker : KotlinGradleProjectChecker {
    private open class DeprecatedProperty(
        val propertyName: String,
        val details: String? = null,
    )

    private class NativeCacheDeprecatedProperty(presetName: String? = null) : DeprecatedProperty(
        presetName?.let { "kotlin.native.cacheKind.$presetName" } ?: "kotlin.native.cacheKind",
        "This property is deprecated. If you still need to disable the native cache, then use a new DSL." +
                " It was removed in 2.3.20, see https://kotl.in/disable-native-cache for details."
    )

    private val warningDeprecatedProperties: List<DeprecatedProperty> = listOf(
        DeprecatedProperty("kotlin.useK2"),
        DeprecatedProperty("kotlin.experimental.tryK2"),
        DeprecatedProperty("kotlin.incremental.classpath.snapshot.enabled"),
        DeprecatedProperty("kotlin.internal.single.build.metrics.file"),
        DeprecatedProperty("kotlin.build.report.dir"),
        DeprecatedProperty("kotlin.native.ignoreIncorrectDependencies"),
        DeprecatedProperty("kotlin.wasm.stability.nowarn"),
        DeprecatedProperty(KotlinJsCompilerType.jsCompilerProperty),
        DeprecatedProperty("${KotlinJsCompilerType.jsCompilerProperty}.nowarn"),
        DeprecatedProperty("kotlin.mpp.androidGradlePluginCompatibility.nowarn"), // Since 2.1.0
        DeprecatedProperty("kotlin.experimental.swift-export.enabled"),
        NativeCacheDeprecatedProperty(), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.IOS_ARM64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.IOS_SIMULATOR_ARM64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.IOS_X64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.MACOS_ARM64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.MACOS_X64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.LINUX_X64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.LINUX_ARM64.presetName), // Since 2.3.20
        NativeCacheDeprecatedProperty(KonanTarget.MINGW_X64.presetName), // Since 2.3.20
        DeprecatedProperty(
            "kotlin.native.useEmbeddableCompilerJar",
            "This property is no longer needed. The embeddable compiler jar is always used for Kotlin/Native projects." +
                    " It was removed in 2.2.20, see https://kotl.in/KT-51301 for details."
        ), // Since 2.2.20
        DeprecatedProperty(
            propertyName = "kotlin.incremental.useClasspathSnapshot",
            details = "History based incremental compilation approach for JVM platform is removed." +
                    " Kotlin Gradle plugin is now using a more efficient approach based on ABI snapshots."
        ),
        DeprecatedProperty(
            propertyName = "kotlin.compiler.preciseCompilationResultsBackup",
            details = "Backups of compilation outputs using the non-precise method have been deprecated and phased out. Only the precise backup method is now used, which is more efficient."
        ), // since 2.3.0
        DeprecatedProperty(
            propertyName = "kotlin.compiler.keepIncrementalCompilationCachesInMemory",
            details = "Backups of compilation outputs using the non-precise method have been deprecated and phased out. Incremental cache changes are now kept in memory until a successful compilation result, which is more efficient."
        ), // since 2.3.0
        DeprecatedProperty(
            propertyName = "kotlin.mpp.import.enableKgpDependencyResolution",
            details = "Legacy mode of KMP IDE import has been removed: https://kotl.in/KT-61127",
        ),
    )

    private val errorDeprecatedProperties: List<DeprecatedProperty> = listOf(
        DeprecatedProperty(
            KOTLIN_MPP_ENABLE_OPTIMISTIC_NUMBER_COMMONIZATION,
            "See https://kotl.in/KT-75161 for details.",
        ),
        DeprecatedProperty(
            KOTLIN_MPP_ENABLE_PLATFORM_INTEGER_COMMONIZATION,
            "See https://kotl.in/KT-75161 for details.",
        ),
    )

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val propertiesBuildService = PropertiesBuildService.registerIfAbsent(project).get()

        warningDeprecatedProperties.filter {
            propertiesBuildService.isPropertyUsed(project, it.propertyName)
        }.forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedWarningGradleProperties(
                    it.propertyName,
                    it.details,
                ),
                key = it.propertyName,
            )
        }

        errorDeprecatedProperties.filter {
            propertiesBuildService.isPropertyUsed(project, it.propertyName)
        }.forEach {
            collector.reportOncePerGradleBuild(
                project,
                KotlinToolingDiagnostics.DeprecatedErrorGradleProperties(
                    it.propertyName,
                    it.details,
                ),
                key = it.propertyName,
            )
        }
    }

    private fun PropertiesBuildService.isPropertyUsed(project: Project, property: String): Boolean = get(property, project) != null
}
