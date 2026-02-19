/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import java.io.File

// TODO(Dmitrii Krasnov): we can remove this, when downloading konan from maven local will be possible KT-63198
private fun disableDownloadingKonanFromMavenCentral(project: Project) {
    project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "false")
}

private fun Project.enableDependencyVerification(enabled: Boolean = true) {
    gradle.startParameter.dependencyVerificationMode = if (enabled) DependencyVerificationMode.STRICT
    else DependencyVerificationMode.OFF
}

internal val konanHome: File by lazy {
    val project = ProjectBuilder
        .builder()
        .build()
        .run {
            disableDownloadingKonanFromMavenCentral(this)
            enableDependencyVerification(false)
            project.plugins.apply("kotlin-multiplatform")

            (project.kotlinExtension as KotlinMultiplatformExtension).apply {
                @Suppress("DEPRECATION")
                macosX64()
                macosArm64()
                linuxX64()
                mingwX64()
            }
            this
        } as ProjectInternal
    project.evaluate()

    NativeCompilerDownloader(project)
        .also { it.downloadIfNeeded() }
        .compilerDirectory
}
