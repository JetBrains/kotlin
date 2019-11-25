/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import kotlin.test.*

class SyncKotlinAndAndroidSourceSetsTest {

    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension
    private lateinit var android: LibraryExtension

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdkVersion(30)

        /* Kotlin Setup */
        kotlin = project.multiplatformExtension

    }


    @Test
    fun `main source set with default settings`() {
        kotlin.android()

        val kotlinAndroidMainSourceSet = kotlin.sourceSets.getByName("androidMain")
        val androidMainSourceSet = android.sourceSets.getByName("main")

        assertEquals(
            androidMainSourceSet.java.srcDirs.toSet(),
            kotlinAndroidMainSourceSet.kotlin.srcDirs.toSet(),
            "Expected all source directories being present in all models"
        )
    }

    @Test
    fun `test source set with default settings`() {
        kotlin.android()

        val kotlinAndroidTestSourceSet = kotlin.sourceSets.getByName("androidTest")
        val testSourceSet = android.sourceSets.getByName("test")

        assertEquals(
            testSourceSet.java.srcDirs.toSet(),
            kotlinAndroidTestSourceSet.kotlin.srcDirs.toSet(),
            "Expected all source directories being present in all models"
        )
    }

    @Test
    fun `androidTest source set with default settings`() {
        kotlin.android()

        val kotlinAndroidAndroidTestSourceSet = kotlin.sourceSets.getByName("androidAndroidTest")
        val androidTestSourceSet = android.sourceSets.getByName("androidTest")

        assertTrue(
            androidTestSourceSet.java.srcDirs.toSet().containsAll(kotlinAndroidAndroidTestSourceSet.kotlin.srcDirs),
            "Expected all kotlin source directories being registered on AGP"
        )

        assertTrue(
            project.file("src/androidTest/kotlin") !in kotlinAndroidAndroidTestSourceSet.kotlin.srcDirs,
            "Expected no source directory of 'androidTest' kotlin source set (Unit Test) " +
                    "being present in 'androidAndroidTest' kotlin source set (Instrumented Test)"
        )

        assertTrue(
            project.file("src/androidTest/kotlin") !in androidTestSourceSet.java.srcDirs,
            "Expected no source directory of 'androidTest' kotlin source set (Unit Test) " +
                    "being present in 'androidTest' Android source set (Instrumented Test)"
        )
    }

    @Test
    fun `all source directories are disjoint in source sets`() {
        kotlin.android()
        project.evaluate()

        kotlin.sourceSets.toSet().allPairs()
            .forEach { (sourceSetA, sourceSetB) ->
                val sourceDirsInBothSourceSets = sourceSetA.kotlin.srcDirs.intersect(sourceSetB.kotlin.srcDirs)
                assertTrue(
                    sourceDirsInBothSourceSets.isEmpty(),
                    "Expected disjoint source directories in source sets. " +
                            "Found $sourceDirsInBothSourceSets present in ${sourceSetA.name}(Kotlin) and ${sourceSetB.name}(Kotlin)"
                )
            }

        android.sourceSets.toSet().allPairs()
            .forEach { (sourceSetA, sourceSetB) ->
                val sourceDirsInBothSourceSets = sourceSetA.java.srcDirs.intersect(sourceSetB.java.srcDirs)
                assertTrue(
                    sourceDirsInBothSourceSets.isEmpty(),
                    "Expected disjoint source directories in source sets. " +
                            "Found $sourceDirsInBothSourceSets present in ${sourceSetA.name}(Android) and ${sourceSetB.name}(Android)"
                )
            }
    }

    @Test
    fun `sync includes user configuration`() {
        kotlin.android()

        val kotlinAndroidMain = kotlin.sourceSets.getByName("androidMain")
        val androidMain = android.sourceSets.getByName("main")

        kotlinAndroidMain.kotlin.srcDir(project.file("fromKotlin"))
        androidMain.java.srcDir(project.file("fromAndroid"))

        project.evaluate()

        assertTrue(
            kotlinAndroidMain.kotlin.srcDirs.containsAll(setOf(project.file("fromKotlin"), project.file("fromAndroid"))),
            "Expected custom configured source directories being present on kotlin source set after evaluation"
        )

        assertTrue(
            androidMain.java.srcDirs.containsAll(setOf(project.file("fromKotlin"), project.file("fromAndroid"))),
            "Expected custom configured source directories being present on android source set after evaluation"
        )
    }
}

private fun <T> Set<T>.allPairs(): Sequence<Pair<T, T>> {
    val values = this.toList()
    return sequence {
        for (index in values.indices) {
            val first = values[index]
            for (remainingIndex in (index + 1)..values.lastIndex) {
                val second = values[remainingIndex]
                yield(first to second)
            }
        }
    }
}
