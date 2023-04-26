/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DuplicatedCode", "FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Action
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.findKotlinSourceSet
import org.jetbrains.kotlin.gradle.util.addBuildEventsListenerRegistryMock
import org.jetbrains.kotlin.gradle.util.setMultiplatformAndroidSourceSetLayoutVersion
import kotlin.test.*

class MultiplatformAndroidSourceSetLayoutV1Test {

    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension
    private lateinit var android: LibraryExtension

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        addBuildEventsListenerRegistryMock(project)
        project.setMultiplatformAndroidSourceSetLayoutVersion(1)

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
        kotlin.androidTarget()

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
        kotlin.androidTarget()

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
        kotlin.androidTarget()

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
            create("beta").dimension = "releaseType"
            create("production").dimension = "releaseType"
            create("free").dimension = "pricing"
            create("paid").dimension = "pricing"
        }
        kotlin.androidTarget()
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
        android.productFlavors(
            Action {
                it.create("beta").dimension = "releaseType"
                it.create("production").dimension = "releaseType"
                it.create("free").dimension = "pricing"
                it.create("paid").dimension = "pricing"
            }
        )
        kotlin.androidTarget()
        project.evaluate()

        kotlin.sourceSets.toSet().generatePairs()
            .forEach { (sourceSetA, sourceSetB) ->
                val sourceDirsInBothSourceSets = sourceSetA.kotlin.srcDirs.intersect(sourceSetB.kotlin.srcDirs)
                assertTrue(
                    sourceDirsInBothSourceSets.isEmpty(),
                    "Expected disjoint source directories in source sets. " +
                            "Found $sourceDirsInBothSourceSets present in ${sourceSetA.name}(Kotlin) and ${sourceSetB.name}(Kotlin)"
                )
            }

        android.sourceSets.toSet().generatePairs()
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
        kotlin.androidTarget()

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
    fun `AndroidSourceSet#kotlinSourceSet convention`() {
        kotlin.androidTarget()

        fun AndroidSourceSet.kotlinSourceSetByConvention(): KotlinSourceSet =
            (this as HasConvention).convention.plugins["kotlin"] as KotlinSourceSet

        val main = android.sourceSets.getByName("main")
        assertSame(kotlin.sourceSets.getByName("androidMain"), main.kotlinSourceSetByConvention())

        val test = android.sourceSets.getByName("test")
        assertSame(kotlin.sourceSets.getByName("androidTest"), test.kotlinSourceSetByConvention())

        val androidTest = android.sourceSets.getByName("androidTest")
        assertSame(kotlin.sourceSets.getByName("androidAndroidTest"), androidTest.kotlinSourceSetByConvention())
    }

    @Test
    fun `AndroidSourceSet kotlin AndroidSourceDirectorySet`() {
        kotlin.androidTarget()
        project.evaluate()
        android.libraryVariants.all { variant ->
            val main = variant.sourceSets.first { it.name == "main" }
            assertEquals(
                project.files("src/main/kotlin", "src/main/java", "src/androidMain/kotlin").toSet(),
                main.kotlinDirectories.toSet()
            )
        }

        android.unitTestVariants.all { variant ->
            val test = variant.sourceSets.first { it.name == "test" }
            assertEquals(
                project.files("src/test/kotlin", "src/test/java", "src/androidTest/kotlin").toSet(),
                test.kotlinDirectories.toSet()
            )
        }

        android.testVariants.all { variant ->
            val androidTest = variant.sourceSets.first { it.name == "androidTest" }
            assertEquals(
                project.files("src/androidTest/java", "src/androidAndroidTest/kotlin").toSet(),
                androidTest.kotlinDirectories.toSet()
            )
        }
    }

    private val AndroidSourceSet.kotlinSourceSet
        get() = project.findKotlinSourceSet(this) ?: fail("Missing KotlinSourceSet for AndroidSourceSet: $name")
}
