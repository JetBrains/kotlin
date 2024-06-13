/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.testUtils

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

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

fun buildProjectWithJvm(
    projectBuilder: ProjectBuilder.() -> Unit = {},
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyKotlinJvmPlugin()
    project.applyKotlinComposePlugin()
    code()
}

fun buildProjectWithMPP(
    projectBuilder: ProjectBuilder.() -> Unit = { },
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {}
) = buildProject(projectBuilder) {
    preApplyCode()
    project.applyMultiplatformPlugin()
    project.applyKotlinComposePlugin()
    code()
}

inline val ExtensionAware.extraProperties: ExtraPropertiesExtension
    get() = extensions.extraProperties

// TODO(Dmitrii Krasnov): we can remove this, when downloading konan from maven local will be possible KT-63198
internal fun disableDownloadingKonanFromMavenCentral(project: Project) {
    project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "false")
}

fun Project.applyKotlinJvmPlugin() {
    project.plugins.apply(KotlinPluginWrapper::class.java)
}

fun Project.applyKotlinComposePlugin() {
    project.plugins.apply(ComposeCompilerGradleSubplugin::class.java)
}

fun Project.applyMultiplatformPlugin(): KotlinMultiplatformExtension {
    disableLegacyWarning(project)
    plugins.apply("kotlin-multiplatform")
    return extensions.getByName("kotlin") as KotlinMultiplatformExtension
}

internal fun disableLegacyWarning(project: Project) {
    project.extraProperties.set("kotlin.js.compiler.nowarn", "true")
}