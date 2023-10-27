/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.artifacts.*
import org.jetbrains.kotlin.gradle.artifacts.KotlinJsKlibArtifact
import org.jetbrains.kotlin.gradle.artifacts.KotlinJvmJarArtifact
import org.jetbrains.kotlin.gradle.artifacts.KotlinNativeHostSpecificMetadataArtifact
import org.jetbrains.kotlin.gradle.artifacts.KotlinNativeKlibArtifact
import org.jetbrains.kotlin.gradle.artifacts.KotlinTargetArtifact
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.AndroidMainSourceSetConventionsChecker
import org.jetbrains.kotlin.gradle.dsl.IosSourceSetConventionChecker
import org.jetbrains.kotlin.gradle.dsl.PlatformSourceSetConventionsChecker
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.CustomizeKotlinDependenciesSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.AndroidPluginWithoutAndroidTargetChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.AndroidSourceSetLayoutV1SourceSetsNotFoundChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.CommonMainOrTestWithDependsOnChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DeprecatedKotlinNativeTargetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DisabledCinteropCommonizationInHmppProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DisabledNativeTargetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DuplicateSourceSetChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.ExperimentalK2UsageChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.InternalGradlePropertiesUsageChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.JsEnvironmentChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.KotlinSourceSetTreeDependsOnMismatchChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.KotlinTargetAlreadyDeclaredChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.MissingNativeStdlibChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.NoKotlinTargetsDeclaredChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.PreHmppDependenciesUsageChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.UnusedSourceSetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.WasmSourceSetsNotFoundChecker
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportSetupAction
import org.jetbrains.kotlin.gradle.plugin.ide.IdeResolveDependenciesTaskSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.GlobalProjectStructureMetadataStorageSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformTargetPresetAction
import org.jetbrains.kotlin.gradle.plugin.mpp.MultiplatformPublishingSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.SyncLanguageSettingsWithKotlinExtensionSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.UserDefinedAttributesSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AddBuildListenerForXCodeSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationProcessorSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCreateResourcesTaskSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCreateSourcesJarTaskSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.DeprecatedMppGradlePropertiesMigrationSetupAction
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinMultiplatformSourceSetSetupAction
import org.jetbrains.kotlin.gradle.plugin.sources.LanguageSettingsSetupAction
import org.jetbrains.kotlin.gradle.plugin.statistics.MultiplatformBuildStatsReportSetupAction
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubpluginSetupAction
import org.jetbrains.kotlin.gradle.targets.*
import org.jetbrains.kotlin.gradle.targets.ConfigureBuildSideEffect
import org.jetbrains.kotlin.gradle.targets.CreateArtifactsSideEffect
import org.jetbrains.kotlin.gradle.targets.CreateDefaultCompilationsSideEffect
import org.jetbrains.kotlin.gradle.targets.CreateTargetConfigurationsSideEffect
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.NativeForwardImplementationToApiElementsSideEffect
import org.jetbrains.kotlin.gradle.targets.js.npm.AddNpmDependencyExtensionProjectSetupAction
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetSetupAction
import org.jetbrains.kotlin.gradle.targets.native.CreateFatFrameworksSetupAction
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeConfigureBinariesSideEffect
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizedCInteropApiElementsConfigurationsSetupAction
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.KotlinArtifactsExtensionSetupAction
import org.jetbrains.kotlin.gradle.tooling.RegisterBuildKotlinToolingMetadataTask

/**
 * Active Extensions (using the [KotlinGradlePluginExtensionPoint] infrastructure) will be registered here by the Kotlin Gradle Plugin.
 */
internal fun Project.registerKotlinPluginExtensions() {
    KotlinProjectSetupAction.extensionPoint.apply {
        register(project, AddNpmDependencyExtensionProjectSetupAction)
        register(project, RegisterBuildKotlinToolingMetadataTask)
        register(project, KotlinToolingDiagnosticsSetupAction)
        register(project, SyncLanguageSettingsWithKotlinExtensionSetupAction)
        register(project, UserDefinedAttributesSetupAction)
        register(project, CustomizeKotlinDependenciesSetupAction)


        if (isJvm || isMultiplatform) {
            register(project, ScriptingGradleSubpluginSetupAction)
        }

        if (isMultiplatform) {
            register(project, ApplyJavaBasePluginSetupAction)
            register(project, DeprecatedMppGradlePropertiesMigrationSetupAction)
            register(project, KotlinMultiplatformTargetPresetAction)
            register(project, KotlinMultiplatformSourceSetSetupAction)
            register(project, MultiplatformBuildStatsReportSetupAction)
            register(project, KotlinMetadataTargetSetupAction)
            register(project, KotlinArtifactsExtensionSetupAction)
            register(project, MultiplatformPublishingSetupAction)
            register(project, LanguageSettingsSetupAction)
            register(project, GlobalProjectStructureMetadataStorageSetupAction)
            register(project, IdeMultiplatformImportSetupAction)
            register(project, IdeResolveDependenciesTaskSetupAction)
            register(project, CInteropCommonizedCInteropApiElementsConfigurationsSetupAction)
            register(project, AddBuildListenerForXCodeSetupAction)
            register(project, CreateFatFrameworksSetupAction)
        }
    }

    KotlinTargetSideEffect.extensionPoint.apply {
        register(project, CreateDefaultCompilationsSideEffect)
        register(project, CreateTargetConfigurationsSideEffect)
        register(project, NativeForwardImplementationToApiElementsSideEffect)
        register(project, CreateArtifactsSideEffect)
        register(project, ConfigureBuildSideEffect)
        register(project, KotlinNativeConfigureBinariesSideEffect)
        register(project, CreateDefaultTestRunSideEffect)
    }

    KotlinCompilationSideEffect.extensionPoint.apply {
        register(project, KotlinCreateSourcesJarTaskSideEffect)
        register(project, KotlinCreateResourcesTaskSideEffect)
        register(project, KotlinCreateLifecycleTasksSideEffect)
        register(project, KotlinCreateNativeCompileTasksSideEffect)
        register(project, KotlinCompilationProcessorSideEffect)
    }

    KotlinTargetArtifact.extensionPoint.apply {
        register(project, KotlinMetadataArtifact)
        register(project, KotlinLegacyCompatibilityMetadataArtifact)
        register(project, KotlinLegacyMetadataArtifact)
        register(project, KotlinJvmJarArtifact)
        register(project, KotlinJsKlibArtifact)
        register(project, KotlinNativeKlibArtifact)
        register(project, KotlinNativeHostSpecificMetadataArtifact)
    }

    KotlinGradleProjectChecker.extensionPoint.apply {
        register(project, CommonMainOrTestWithDependsOnChecker)
        register(project, DeprecatedKotlinNativeTargetsChecker)
        register(project, MissingNativeStdlibChecker)
        register(project, UnusedSourceSetsChecker)
        register(project, AndroidSourceSetLayoutV1SourceSetsNotFoundChecker)
        register(project, AndroidPluginWithoutAndroidTargetChecker)
        register(project, NoKotlinTargetsDeclaredChecker)
        register(project, DisabledCinteropCommonizationInHmppProjectChecker)
        register(project, DisabledNativeTargetsChecker)
        register(project, JsEnvironmentChecker)
        register(project, PreHmppDependenciesUsageChecker)
        register(project, ExperimentalK2UsageChecker)
        register(project, KotlinSourceSetTreeDependsOnMismatchChecker)
        register(project, PlatformSourceSetConventionsChecker)
        register(project, AndroidMainSourceSetConventionsChecker)
        register(project, IosSourceSetConventionChecker)
        register(project, KotlinTargetAlreadyDeclaredChecker)
        register(project, InternalGradlePropertiesUsageChecker)
        register(project, WasmSourceSetsNotFoundChecker)
        register(project, DuplicateSourceSetChecker)

        if (isMultiplatform) {
            register(project, KotlinMultiplatformAndroidGradlePluginCompatibilityChecker)
        }
    }
}

/* Helper functions to make configuration code above easier to read */

private val Project.isMultiplatform get() = multiplatformExtensionOrNull != null

private val Project.isJvm get() = kotlinJvmExtensionOrNull != null

private val Project.isJs get() = kotlinExtensionOrNull is KotlinJsProjectExtension

private val Project.isAndroid get() = kotlinExtension is KotlinAndroidProjectExtension