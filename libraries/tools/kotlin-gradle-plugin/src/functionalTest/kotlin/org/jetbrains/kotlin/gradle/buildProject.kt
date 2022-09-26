/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.kpm.applyKpmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension

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
    project.plugins.apply(KotlinPlatformJvmPlugin::class.java)
    code()
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