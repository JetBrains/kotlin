/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.problems.Problem
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.internal.AdditionalDataBuilderFactory
import org.gradle.api.problems.internal.InternalProblem
import org.gradle.api.problems.internal.InternalProblemBuilder
import org.gradle.api.problems.internal.InternalProblemReporter
import org.gradle.api.problems.internal.InternalProblemSpec
import org.gradle.api.problems.internal.InternalProblems
import org.gradle.api.problems.internal.ProblemsInfrastructure
import org.gradle.api.problems.internal.ProblemsProgressEventEmitterHolder
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.reflect.Instantiator
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.base.TestingExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KmpIsolatedProjectsSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.KotlinArtifactsExtensionImpl
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.kotlinArtifactsExtension
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.XcodeVersion

fun buildProject(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    configureProject: Project.() -> Unit = {},
): ProjectInternal = ProjectBuilder
    .builder()
    .apply(projectBuilder)
    .build()
    .also {
        disableDownloadingKonanFromMavenCentral(it)
        it.enableDependencyVerification(false)
    }
    .apply(configureProject)
    .let { it as ProjectInternal }

fun buildProjectWithMPP(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyMultiplatformPlugin()
    code()
}

fun buildProjectWithJvm(
    projectBuilder: ProjectBuilder.() -> Unit = {},
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyKotlinJvmPlugin()
    code()
}

fun buildProjectWithCocoapods(projectBuilder: ProjectBuilder.() -> Unit = {}, code: Project.() -> Unit = {}) = buildProject(projectBuilder) {
    project.applyMultiplatformPlugin()
    project.applyCocoapodsPlugin()
    code()
}

fun Project.applyKotlinJvmPlugin() {
    project.plugins.apply(KotlinPluginWrapper::class.java)
}

fun Project.applyKotlinAndroidPlugin() {
    project.plugins.apply(KotlinAndroidPluginWrapper::class.java)
}

fun Project.kotlin(code: KotlinMultiplatformExtension.() -> Unit) {
    val kotlin = project.kotlinExtension as KotlinMultiplatformExtension
    kotlin.code()
}

fun Project.kotlinArtifacts(code: KotlinArtifactsExtensionImpl.() -> Unit) {
    val kotlinArtifacts = project.kotlinArtifactsExtension as KotlinArtifactsExtensionImpl
    kotlinArtifacts.code()
}

fun Project.androidLibrary(code: LibraryExtension.() -> Unit) {
    plugins.findPlugin("com.android.library") ?: plugins.apply("com.android.library")
    val androidExtension = project.extensions.getByName("android") as LibraryExtension
    androidExtension.configureDefaults()
    androidExtension.code()
}

fun Project.androidApplication(code: ApplicationExtension.() -> Unit) {
    plugins.findPlugin("com.android.application") ?: plugins.apply("com.android.application")
    val androidExtension = project.extensions.getByName("android") as ApplicationExtension
    androidExtension.configureDefaults()
    androidExtension.code()
}

fun Project.testing(code: TestingExtension.() -> Unit) {
    extensions.configure(TestingExtension::class.java, code)
}

fun Project.applyMultiplatformPlugin(): KotlinMultiplatformExtension {
    plugins.apply("kotlin-multiplatform")
    return extensions.getByName("kotlin") as KotlinMultiplatformExtension
}

fun Project.applyCocoapodsPlugin() {
    plugins.apply("org.jetbrains.kotlin.native.cocoapods")
    kotlin { cocoapods { version = "1.0" } }
}

fun KotlinMultiplatformExtension.cocoapods(code: CocoapodsExtension.() -> Unit) {
    requireNotNull(getExtension<CocoapodsExtension>("cocoapods")).apply(code)
}

fun KotlinMultiplatformExtension.swiftExport(code: SwiftExportExtension.() -> Unit) {
    requireNotNull(getExtension<SwiftExportExtension>("swiftExport")).apply(code)
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)

fun Project.enableCInteropCommonization(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION, enabled.toString())
}

fun Project.enableMppResourcesPublication(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_RESOURCES_PUBLICATION, enabled.toString())
}

internal fun Project.setUklibPublicationStrategy(strategy: KmpPublicationStrategy = KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_KMP_PUBLICATION_STRATEGY, strategy.propertyName)
}

fun Project.disableCrossCompilation(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_NATIVE_DISABLE_KLIBS_CROSSCOMPILATION, enabled.toString())
}

internal fun Project.setUklibResolutionStrategy(strategy: KmpResolutionStrategy) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_KMP_RESOLUTION_STRATEGY, strategy.propertyName)
}

fun Project.enableIntransitiveMetadataConfiguration(enabled: Boolean = true) {
    propertiesExtension.set(KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION, enabled.toString())
}

fun Project.enableDefaultStdlibDependency(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_STDLIB_DEFAULT_DEPENDENCY, enabled.toString())
}

fun Project.enableDefaultJsDomApiDependency(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_JS_STDLIB_DOM_API_INCLUDED, enabled.toString())
}

fun Project.setMultiplatformAndroidSourceSetLayoutVersion(version: Int) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION, version.toString())
}

fun Project.enableDependencyVerification(enabled: Boolean = true) {
    gradle.startParameter.dependencyVerificationMode = if (enabled) DependencyVerificationMode.STRICT
    else DependencyVerificationMode.OFF
}

fun Project.mockXcodeVersion(version: XcodeVersion = XcodeVersion.maxTested) {
    project.layout.buildDirectory.getFile().apply {
        mkdirs()
        resolve("xcode-version.txt").writeText(version.toString())
    }
}

fun Project.enableSecondaryJvmClassesVariant(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_JVM_ADD_CLASSES_VARIANT, enabled.toString())
}

fun Project.enableKmpProjectIsolationSupport(enabled: Boolean = true) {
    if (enabled) {
        project.propertiesExtension.set(KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT, KmpIsolatedProjectsSupport.ENABLE)
    } else {
        project.propertiesExtension.set(KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT, KmpIsolatedProjectsSupport.DISABLE)
    }
}

fun Project.enableNonPackedKlibsUsage(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_USE_NON_PACKED_KLIBS, enabled.toString())
}
