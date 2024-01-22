/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.artifacts.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.CustomizeKotlinDependenciesSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.*
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportActionSetupAction
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportSetupAction
import org.jetbrains.kotlin.gradle.plugin.ide.IdeResolveDependenciesTaskSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AddBuildListenerForXCodeSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.DeprecatedMppGradlePropertiesMigrationSetupAction
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinMultiplatformSourceSetSetupAction
import org.jetbrains.kotlin.gradle.plugin.sources.LanguageSettingsSetupAction
import org.jetbrains.kotlin.gradle.plugin.statistics.MultiplatformBuildStatsReportSetupAction
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubpluginSetupAction
import org.jetbrains.kotlin.gradle.targets.*
import org.jetbrains.kotlin.gradle.targets.js.npm.AddNpmDependencyExtensionProjectSetupAction
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetSetupAction
import org.jetbrains.kotlin.gradle.targets.native.ConfigureFrameworkExportSideEffect
import org.jetbrains.kotlin.gradle.targets.native.CreateFatFrameworksSetupAction
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeConfigureBinariesSideEffect
import org.jetbrains.kotlin.gradle.targets.native.SetupEmbedAndSignAppleFrameworkTaskSideEffect
import org.jetbrains.kotlin.gradle.targets.native.internal.*
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
        register(project, AddKotlinPlatformIntegersSupportLibrary)
        register(project, SetupKotlinNativePlatformDependenciesForLegacyImport)


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
            register(project, KotlinRegisterCompilationArchiveTasksExtension)
            register(project, IdeMultiplatformImportActionSetupAction)
            register(project, KotlinLLDBScriptSetupAction)
            register(project, ExcludeDefaultPlatformDependenciesFromKotlinNativeCompileTasks)
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
        register(project, ConfigureFrameworkExportSideEffect)
        register(project, SetupCInteropApiElementsConfigurationSideEffect)
        register(project, SetupEmbedAndSignAppleFrameworkTaskSideEffect)
    }

    KotlinCompilationSideEffect.extensionPoint.apply {
        register(project, KotlinCreateSourcesJarTaskSideEffect)
        register(project, KotlinCreateResourcesTaskSideEffect)
        register(project, KotlinCreateLifecycleTasksSideEffect)
        register(project, KotlinCreateNativeCompileTasksSideEffect)
        register(project, KotlinCompilationProcessorSideEffect)
        register(project, KotlinCreateNativeCInteropTasksSideEffect)
        register(project, KotlinCreateCompilationArchivesTask)
        register(project, SetupKotlinNativePlatformDependenciesAndStdlib)
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
        register(project, ExperimentalTryNextUsageChecker)
        register(project, KotlinSourceSetTreeDependsOnMismatchChecker)
        register(project, PlatformSourceSetConventionsChecker)
        register(project, AndroidMainSourceSetConventionsChecker)
        register(project, IosSourceSetConventionChecker)
        register(project, KotlinTargetAlreadyDeclaredChecker)
        register(project, InternalGradlePropertiesUsageChecker)
        register(project, WasmSourceSetsNotFoundChecker)
        register(project, DuplicateSourceSetChecker)
        register(project, CInteropInputChecker)
        register(project, IncorrectNativeDependenciesChecker)

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