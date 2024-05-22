/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.KotlinTargetResourcesResolutionStrategy
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.KotlinArtifactsExtensionImpl
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.kotlinArtifactsExtension
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.XcodeVersion

fun buildProject(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    configureProject: Project.() -> Unit = {},
): ProjectInternal = ProjectBuilder
    .builder()
    .apply(projectBuilder)
    .build()
    .also { disableDownloadingKonanFromMavenCentral(it) }
    .apply(configureProject)
    .let { it as ProjectInternal }

fun buildProjectWithMPP(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyMultiplatformPlugin()
    disableLegacyWarning(project)
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
    androidExtension.code()
}

fun Project.androidApplication(code: ApplicationExtension.() -> Unit) {
    plugins.findPlugin("com.android.application") ?: plugins.apply("com.android.application")
    val androidExtension = project.extensions.getByName("android") as ApplicationExtension
    androidExtension.code()
}

fun Project.applyMultiplatformPlugin(): KotlinMultiplatformExtension {
    disableLegacyWarning(project)
    plugins.apply("kotlin-multiplatform")
    return extensions.getByName("kotlin") as KotlinMultiplatformExtension
}

fun Project.applyCocoapodsPlugin() {
    plugins.apply("org.jetbrains.kotlin.native.cocoapods")
    kotlin { cocoapods { version = "1.0" } }
}

fun KotlinMultiplatformExtension.cocoapods(code: CocoapodsExtension.() -> Unit) {
    getExtension<CocoapodsExtension>("cocoapods")!!.apply(code)
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)

fun Project.enableCInteropCommonization(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION, enabled.toString())
}

fun Project.enableMppResourcesPublication(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_RESOURCES_PUBLICATION, enabled.toString())
}

fun Project.setMppResourcesResolutionStrategy(strategy: KotlinTargetResourcesResolutionStrategy) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY, strategy.propertyName)
}

fun Project.enableIntransitiveMetadataConfiguration(enabled: Boolean = true) {
    propertiesExtension.set(KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION, enabled.toString())
}

fun Project.enableDefaultStdlibDependency(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_STDLIB_DEFAULT_DEPENDENCY, enabled.toString())
}

fun Project.enableSwiftExport(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_SWIFT_EXPORT_ENABLED, enabled.toString())
}

fun Project.setMultiplatformAndroidSourceSetLayoutVersion(version: Int) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION, version.toString())
}

fun Project.enableDependencyVerification(enabled: Boolean = true) {
    gradle.startParameter.dependencyVerificationMode = if (enabled) DependencyVerificationMode.STRICT
    else DependencyVerificationMode.OFF
}

fun Project.enableWasmStabilityNoWarn(enabled: Boolean = true) {
    propertiesExtension.set("kotlin.wasm.stability.nowarn", enabled.toString())
}

fun Project.mockXcodeVersion(version: XcodeVersion = XcodeVersion.maxTested) {
    project.layout.buildDirectory.getFile().apply {
        mkdirs()
        resolve("xcode-version.txt").writeText(version.toString())
    }
}
