/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import java.io.File

internal val konanHome: File by lazy {
    val project = ProjectBuilder
        .builder()
        .build()
        .run {
            project.plugins.apply("kotlin-multiplatform")

            (project.kotlinExtension as KotlinMultiplatformExtension).apply {
                macosX64()
                macosArm64()
                linuxX64()
                mingwX64()
            }
            this
        } as ProjectInternal
    project.evaluate()
    NativeCompilerDownloader(project).compilerDirectory
}