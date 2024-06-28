/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.builder.model.SourceProvider
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.android.*
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.setMultiplatformAndroidSourceSetLayoutVersion
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.jetbrains.kotlin.gradle.utils.forAllAndroidVariants
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MultiplatformAndroidSourceSetLayoutV2Test {

    private val project = ProjectBuilder.builder().build()
        .run { this as ProjectInternal }
        .also { project -> project.setMultiplatformAndroidSourceSetLayoutVersion(2) }

    private val kotlin = project.applyMultiplatformPlugin()

    private val android: LibraryExtension = run {
        project.plugins.apply(LibraryPlugin::class.java)
        project.androidExtension as LibraryExtension
    }

    init {
        android.compileSdk = 31
    }

    @Test
    fun `test - main SourceSet - is called 'androidMain'`() {
        kotlin.androidTarget()
        assertEquals("androidMain", project.getKotlinSourceSetOrFail(android.sourceSets.main).name)
    }

    @Test
    fun `test - main SourceSet name - with custom targetName`() {
        kotlin.androidTarget("foo")
        assertEquals("fooMain", project.getKotlinSourceSetOrFail(android.sourceSets.main).name)
    }

    @Test
    fun `test - unitTest SourceSet - is called 'androidUnitTest'`() {
        kotlin.androidTarget()
        assertEquals("androidUnitTest", project.getKotlinSourceSetOrFail(android.sourceSets.test).name)
    }

    @Test
    fun `test - unitTest SourceSet name - with custom targetName`() {
        kotlin.androidTarget("foo")
        assertEquals("fooUnitTest", project.getKotlinSourceSetOrFail(android.sourceSets.test).name)
    }

    @Test
    fun `test - instrumentedTest SourceSet - is called 'androidInstrumentedTest'`() {
        kotlin.androidTarget()
        assertEquals("androidInstrumentedTest", project.getKotlinSourceSetOrFail(android.sourceSets.androidTest).name)
    }

    @Test
    fun `test instrumentedTest SourceSet - with custom targetName`() {
        kotlin.androidTarget("foo")
        assertEquals("fooInstrumentedTest", project.getKotlinSourceSetOrFail(android.sourceSets.androidTest).name)
    }

    @Test
    fun `test SourceSet names - with two flavorDimensions`() {
        kotlin.androidTarget()
        android.flavorDimensions.add("market")
        android.flavorDimensions.add("price")
        android.productFlavors.create("german").dimension = "market"
        android.productFlavors.create("usa").dimension = "market"
        android.productFlavors.create("paid").dimension = "price"
        android.productFlavors.create("free").dimension = "price"

        project.evaluate()

        fun assertKotlinSourceSetNameEquals(
            androidSourceSetName: String, kotlinSourceSetName: String
        ) {
            val androidSourceSet = android.sourceSets.getByName(androidSourceSetName)
            val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
            assertEquals(
                kotlinSourceSetName, kotlinSourceSet.name,
                "Expected KotlinSourceSet name: $kotlinSourceSetName for AndroidSourceSet name: $androidSourceSetName"
            )
        }

        assertKotlinSourceSetNameEquals("germanPaidDebug", "androidGermanPaidDebug")
        assertKotlinSourceSetNameEquals("germanPaid", "androidGermanPaid")
        assertKotlinSourceSetNameEquals("german", "androidGerman")

        assertKotlinSourceSetNameEquals("testGermanPaidDebug", "androidUnitTestGermanPaidDebug")
        assertKotlinSourceSetNameEquals("testGermanPaid", "androidUnitTestGermanPaid")
        assertKotlinSourceSetNameEquals("testGerman", "androidUnitTestGerman")

        assertKotlinSourceSetNameEquals("androidTestGermanPaidDebug", "androidInstrumentedTestGermanPaidDebug")
        assertKotlinSourceSetNameEquals("androidTestGermanPaid", "androidInstrumentedTestGermanPaid")
        assertKotlinSourceSetNameEquals("androidTestGerman", "androidInstrumentedTestGerman")
    }

    @Test
    fun `test - all source directories - are disjoint in source sets`() {
        kotlin.androidTarget()
        android.flavorDimensions.add("market")
        android.flavorDimensions.add("price")
        android.productFlavors.create("german").dimension = "market"
        android.productFlavors.create("usa").dimension = "market"
        android.productFlavors.create("paid").dimension = "price"
        android.productFlavors.create("free").dimension = "price"

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
    fun `test - default dependsOn edges`() {
        kotlin.androidTarget()
        project.evaluate()

        android.libraryVariants.all { libraryVariant ->
            libraryVariant.sourceSets.forEach { androidSourceSet ->
                val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
                assertEquals(
                    setOf("commonMain"), kotlinSourceSet.dependsOn.map { it.name }.toSet(),
                    "Expected KotlinSourceSet: ${kotlinSourceSet.name} to dependsOn commonMain"
                )
            }
        }

        android.unitTestVariants.all { libraryVariant ->
            libraryVariant.sourceSets.forEach { androidSourceSet ->
                val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
                assertEquals(
                    setOf("commonTest"), kotlinSourceSet.dependsOn.map { it.name }.toSet(),
                    "Expected KotlinSourceSet: ${kotlinSourceSet.name} to dependsOn commonTest"
                )
            }
        }

        android.testVariants.all { libraryVariant ->
            libraryVariant.sourceSets.forEach { androidSourceSet ->
                val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
                assertEquals(
                    emptySet(), kotlinSourceSet.dependsOn.map { it.name }.toSet(),
                    "Expected KotlinSourceSet: ${kotlinSourceSet.name} to have no dependsOn edges"
                )
            }
        }
    }

    @Test
    fun `test - kotlin source directories - are in sync between KotlinSourceSet and AndroidSourceSet`() {
        kotlin.androidTarget()

        project.forAllAndroidVariants { variant ->
            variant.sourceSets.forEach { androidSourceSet ->
                androidSourceSet as SourceProvider

                /* Check if KotlinSourceSet and AndroidSourceSet contain the same srcDirs */
                val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
                assertEquals(
                    androidSourceSet.kotlinDirectories.toSet(), kotlinSourceSet.kotlin.srcDirs,
                    "Expected same kotlin srcDirs for KotlinSourceSet: ${kotlinSourceSet.name} " +
                            "and AndroidSourceSet: ${androidSourceSet.name}"
                )


                /* Check if androidSourceSet.kotlinDirectories is live and includes sources from Kotlin */
                val adhocSrcDir = project.file("src/${kotlinSourceSet.name}/adhoc/${variant.name}")
                assertFalse(adhocSrcDir in androidSourceSet.kotlinDirectories)
                kotlinSourceSet.kotlin.srcDir(adhocSrcDir)
                assertTrue(adhocSrcDir in androidSourceSet.kotlinDirectories)
            }
        }

        project.evaluate()
    }

    @Test
    fun `test - kotlin source directories - supports AGP default location`() {
        kotlin.androidTarget()

        project.forAllAndroidVariants { variant ->
            variant.sourceSets.forEach { androidSourceSet ->
                val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
                assertTrue(
                    project.file("src/${androidSourceSet.name}/kotlin") in kotlinSourceSet.kotlin.srcDirs,
                    "Expected 'src/${androidSourceSet.name}/kotlin to be supported by KotlinSourceSet: ${kotlinSourceSet.name}"
                )
            }
        }

        project.evaluate()
    }

    @Test
    fun `test - main - default AndroidManifest location`() {
        kotlin.androidTarget()

        assertEquals(
            project.file("src/androidMain/AndroidManifest.xml"),
            android.sourceSets.main.manifest.srcFile,
            "Expected default AndroidManifest to be placed in 'androidMain'"
        )


        /* Check if default can be overwritten */
        android.sourceSets.main.manifest.srcFile(project.file("custom.xml"))
        project.evaluate()
        assertEquals(project.file("custom.xml"), android.sourceSets.main.manifest.srcFile)
    }

    @Test
    fun `test - main - default Manifest location - already changed by user - does not get overwritten`() {
        /* First: Change manifest location to something non-default */
        val customManifestFile = project.file("src/main/CustomAndroidManifest.xml")
        android.sourceSets.main.manifest.srcFile(customManifestFile)

        /* Then: Setup Kotlin/Android target */
        kotlin.androidTarget()

        assertEquals(
            customManifestFile, android.sourceSets.main.manifest.srcFile,
            "Expected location of Manifest file to retain user configuration."
        )
    }

    @Test
    fun `test - defaultKotlinSourceSetName - is determined for all compilations`() {
        kotlin.androidTarget()
        android.flavorDimensions.add("market")
        android.flavorDimensions.add("price")
        android.productFlavors.create("german").dimension = "market"
        android.productFlavors.create("usa").dimension = "market"
        android.productFlavors.create("paid").dimension = "price"
        android.productFlavors.create("free").dimension = "price"
        project.evaluate()

        kotlin.androidTarget().compilations.all { compilation ->
            val defaultKotlinSourceSetName = multiplatformAndroidSourceSetLayoutV2.naming
                .defaultKotlinSourceSetName(kotlin.androidTarget(), compilation.androidVariant)

            assertNotNull(
                defaultKotlinSourceSetName,
                "Expected non-null 'defaultKotlinSourceSetName' for compilation ${compilation.name}"
            )

            val kotlinSourceSet = kotlin.sourceSets.getByName(defaultKotlinSourceSetName)

            assertEquals(
                setOf(compilation.androidVariant.name), kotlinSourceSet.androidSourceSetInfo.androidVariantNames,
                "Expected KotlinSourceSet ${kotlinSourceSet.name} to only mention androidVariant ${compilation.androidVariant.name}"
            )
        }
    }

    @Test
    fun `test - defaultKotlinSourceSetName`() {
        kotlin.androidTarget()
        android.flavorDimensions.add("market")
        android.flavorDimensions.add("price")
        android.productFlavors.create("german").dimension = "market"
        android.productFlavors.create("usa").dimension = "market"
        android.productFlavors.create("paid").dimension = "price"
        android.productFlavors.create("free").dimension = "price"
        project.evaluate()

        assertEquals(
            "androidGermanFreeDebug",
            kotlin.androidTarget().compilations.getByName("germanFreeDebug").defaultSourceSet.name
        )

        assertEquals(
            "androidUsaFreeDebug",
            kotlin.androidTarget().compilations.getByName("usaFreeDebug").defaultSourceSet.name
        )

        assertEquals(
            "androidGermanPaidRelease",
            kotlin.androidTarget().compilations.getByName("germanPaidRelease").defaultSourceSet.name
        )

        assertEquals(
            "androidUsaPaidRelease",
            kotlin.androidTarget().compilations.getByName("usaPaidRelease").defaultSourceSet.name
        )

        assertEquals(
            "androidUnitTestGermanFreeDebug",
            kotlin.androidTarget().compilations.getByName("germanFreeDebugUnitTest").defaultSourceSet.name
        )

        assertEquals(
            "androidInstrumentedTestGermanFreeDebug",
            kotlin.androidTarget().compilations.getByName("germanFreeDebugAndroidTest").defaultSourceSet.name
        )
    }
}
