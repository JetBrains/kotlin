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
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.unitTests.kpm.applyKpmPlugin

fun buildProject(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    configureProject: Project.() -> Unit = {},
): ProjectInternal = ProjectBuilder
    .builder()
    .apply(projectBuilder)
    .build()
    //temporary solution for BuildEventsListenerRegistry
    .also { addBuildEventsListenerRegistryMock(it) }
    .apply(configureProject)
    .let { it as ProjectInternal }

fun buildProjectWithMPP(projectBuilder: ProjectBuilder.() -> Unit = { }, code: Project.() -> Unit = {}) = buildProject(projectBuilder) {
    project.applyMultiplatformPlugin()
    disableLegacyWarning(project)
    code()
}

fun buildProjectWithKPM(projectBuilder: ProjectBuilder.() -> Unit = { }, code: Project.() -> Unit= {}) = buildProject(projectBuilder) {
    project.applyKpmPlugin()
    code()
}

fun buildProjectWithJvm(projectBuilder: ProjectBuilder.() -> Unit = {}, code: Project.() -> Unit = {}) = buildProject(projectBuilder) {
    project.applyKotlinJvmPlugin()
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

fun Project.projectModel(code: KotlinPm20ProjectExtension.() -> Unit) {
    val extension = project.extensions.getByType(KotlinPm20ProjectExtension::class.java)
    extension.code()
}

fun Project.applyMultiplatformPlugin(): KotlinMultiplatformExtension {
    addBuildEventsListenerRegistryMock(this)
    disableLegacyWarning(project)
    plugins.apply("kotlin-multiplatform")
    return extensions.getByName("kotlin") as KotlinMultiplatformExtension
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)

fun Project.enableGranularSourceSetsMetadata() {
    propertiesExtension.set("kotlin.mpp.enableGranularSourceSetsMetadata", "true")
}

fun Project.enableCInteropCommonization(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION, enabled.toString())
}

fun Project.enableHierarchicalStructureByDefault(enabled: Boolean = true) {
    propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_HIERARCHICAL_STRUCTURE_BY_DEFAULT, enabled.toString())
}

fun Project.enableIntransitiveMetadataConfiguration(enabled: Boolean = true) {
    propertiesExtension.set(KOTLIN_MPP_ENABLE_INTRANSITIVE_METADATA_CONFIGURATION, enabled.toString())
}

fun Project.enableDefaultStdlibDependency(enabled: Boolean = true) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_STDLIB_DEFAULT_DEPENDENCY, enabled.toString())
}

fun Project.setMultiplatformAndroidSourceSetLayoutVersion(version: Int) {
    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION, version.toString())
}

fun Project.enableDependencyVerification(enabled: Boolean = true) {
    gradle.startParameter.dependencyVerificationMode = if (enabled) DependencyVerificationMode.STRICT
    else DependencyVerificationMode.OFF
}

fun Project.enableCompatibilityMetadataVariant(enabled: Boolean = true) {
    propertiesExtension.set(KOTLIN_MPP_ENABLE_COMPATIBILITY_METADATA_VARIANT, enabled.toString())
}
