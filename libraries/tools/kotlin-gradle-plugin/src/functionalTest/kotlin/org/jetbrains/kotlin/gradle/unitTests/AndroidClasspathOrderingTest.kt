/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.kotlinBuildDeps
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.configureDefaults
import org.jetbrains.kotlin.gradle.util.setAndroidSdkDirProperty
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidClasspathOrderingTest {

    @Test
    fun androidBootClasspathFirst() {
        val project = buildProject()
        setAndroidSdkDirProperty(project)

        project.plugins.apply("kotlin-android")
        project.plugins.apply("android-library")

        val android = project.extensions.getByName("android") as LibraryExtension
        android.configureDefaults()

        project.repositories.kotlinBuildDeps()
        project.repositories.mavenCentralCacheRedirector()
        project.evaluate()

        val classpath = project.tasks.named<KotlinJvmCompile>("compileDebugKotlin").get().libraries
        assertEquals(classpath.files.first(), android.bootClasspath.first())
    }
}
