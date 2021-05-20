/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName")
package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.junit.Test

class KotlinAndroidDependsOnEdgesTest {
    @Test
    fun `default android source set declares dependsOn commonMain`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdkVersion(30)

        /* Minimal MPP setup */
        val kotlin = project.kotlinExtension as KotlinMultiplatformExtension
        kotlin.android("android")

        /* Force evaluation */
        project as ProjectInternal
        project.evaluate()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val androidMain = kotlin.sourceSets.getByName("androidMain")
        val androidTest = kotlin.sourceSets.getByName("androidTest")
        val androidAndroidTest = kotlin.sourceSets.getByName("androidAndroidTest")

        assertThat(
            "Expected androidMain to dependOn commonMain",
            setOf(commonMain) == androidMain.dependsOn
        )

        assertThat(
            "Expected androidTest to dependOn commonTest",
            setOf(commonTest) == androidTest.dependsOn
        )

        assertThat(
            "Expected androidAndroidTest to dependOn commonTest",
            setOf(commonTest) == androidAndroidTest.dependsOn
        )
    }

    @Test
    fun `custom dependsOn edges`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdkVersion(30)

        /* Custom MPP setup */
        val kotlin = project.kotlinExtension as KotlinMultiplatformExtension
        kotlin.android("android")
        kotlin.sourceSets.apply {
            val jvmMain = create("jvmMain") {
                it.dependsOn(getByName("commonMain"))
            }
            getByName("androidMain") {
                it.dependsOn(jvmMain)
            }
        }

        /* Force evaluation */
        project as ProjectInternal
        project.evaluate()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val androidMain = kotlin.sourceSets.getByName("androidMain")
        val androidTest = kotlin.sourceSets.getByName("androidTest")
        val androidAndroidTest = kotlin.sourceSets.getByName("androidAndroidTest")

        assertThat(
            "Expected androidMain to depend on commonMain and jvmMain",
            setOf(commonMain, jvmMain).sorted() == androidMain.dependsOn.sorted()
        )

        assertThat(
            "Expected androidTest to only depend on commonTest",
            setOf(commonTest) == androidTest.dependsOn
        )

        assertThat(
            "Expected androidAndroidTest to only depend on commonTest",
            setOf(commonTest) == androidAndroidTest.dependsOn
        )
    }
}

private fun Iterable<KotlinSourceSet>.sorted() = this.sortedBy { it.name }
