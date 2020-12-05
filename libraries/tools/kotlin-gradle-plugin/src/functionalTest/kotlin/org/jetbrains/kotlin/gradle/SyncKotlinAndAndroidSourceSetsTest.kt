/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinSourceSet
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

        assertTrue(
            kotlinAndroidMainSourceSet.kotlin.srcDirs.containsAll(androidMainSourceSet.java.srcDirs),
            "Expected all Android java srcDirs in Kotlin source set.\n" +
                    "Kotlin=${kotlinAndroidMainSourceSet.kotlin.srcDirs}\n" +
                    "Android=${androidMainSourceSet.java.srcDirs}"
        )
    }

    @Test
    fun `test source set with default settings`() {
        kotlin.android()

        val kotlinAndroidTestSourceSet = kotlin.sourceSets.getByName("androidTest")
        val testSourceSet = android.sourceSets.getByName("test")

        assertTrue(
            kotlinAndroidTestSourceSet.kotlin.srcDirs.containsAll(testSourceSet.java.srcDirs),
            "Expected all Android java srcDirs in Kotlin source set.\n" +
                    "Kotlin=${kotlinAndroidTestSourceSet.kotlin.srcDirs}\n" +
                    "Android=${testSourceSet.java.srcDirs}"
        )
    }

    @Test
    fun `androidTest source set with default settings`() {
        kotlin.android()

        val kotlinAndroidAndroidTestSourceSet = kotlin.sourceSets.getByName("androidAndroidTest")
        val androidTestSourceSet = android.sourceSets.getByName("androidTest")

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
    fun `two product flavor dimensions`() {
        android.flavorDimensions("pricing", "releaseType")
        android.productFlavors {
            it.create("beta").dimension = "releaseType"
            it.create("production").dimension = "releaseType"
            it.create("free").dimension = "pricing"
            it.create("paid").dimension = "pricing"
        }
        kotlin.android()
        project.evaluate()

        fun assertSourceSetsExist(androidName: String, kotlinName: String) {
            val androidSourceSet = assertNotNull(android.sourceSets.findByName(androidName), "Expected Android source set '$androidName'")
            val kotlinSourceSet = assertNotNull(kotlin.sourceSets.findByName(kotlinName), "Expected Kotlin source set '$kotlinName'")
            assertSame(kotlinSourceSet, androidSourceSet.kotlinSourceSet)
        }

        assertSourceSetsExist("freeBetaDebug", "androidFreeBetaDebug")
        assertSourceSetsExist("freeBetaRelease", "androidFreeBetaRelease")

        assertSourceSetsExist("freeProductionDebug", "androidFreeProductionDebug")
        assertSourceSetsExist("freeProductionRelease", "androidFreeProductionRelease")


        assertSourceSetsExist("paidBetaDebug", "androidPaidBetaDebug")
        assertSourceSetsExist("paidBetaRelease", "androidPaidBetaRelease")

        assertSourceSetsExist("paidProductionDebug", "androidPaidProductionDebug")
        assertSourceSetsExist("paidProductionRelease", "androidPaidProductionRelease")
    }

    @Test
    fun `all source directories are disjoint in source sets`() {
        android.flavorDimensions("pricing", "releaseType")
        android.productFlavors {
            it.create("beta").dimension = "releaseType"
            it.create("production").dimension = "releaseType"
            it.create("free").dimension = "pricing"
            it.create("paid").dimension = "pricing"
        }
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
    }

    @Test
    fun `AndroidSourceSet#kotlinSourceSet extension`() {
        kotlin.android()

        val main = android.sourceSets.getByName("main")
        assertSame(kotlin.sourceSets.getByName("androidMain"), main.kotlinSourceSet)

        val test = android.sourceSets.getByName("test")
        assertSame(kotlin.sourceSets.getByName("androidTest"), test.kotlinSourceSet)

        val androidTest = android.sourceSets.getByName("androidTest")
        assertSame(kotlin.sourceSets.getByName("androidAndroidTest"), androidTest.kotlinSourceSet)
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
