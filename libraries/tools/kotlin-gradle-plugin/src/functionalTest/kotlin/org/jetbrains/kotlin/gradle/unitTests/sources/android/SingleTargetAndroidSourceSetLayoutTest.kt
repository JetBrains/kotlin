/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SingleTargetAndroidSourceSetLayoutTest {
    private val project = ProjectBuilder.builder().build() as ProjectInternal

    private val android: LibraryExtension = run {
        project.plugins.apply(LibraryPlugin::class.java)
        project.extensions.getByName("android") as LibraryExtension
    }

    @BeforeTest
    fun setup() {
        project.plugins.apply(KotlinAndroidPluginWrapper::class.java)
        android.compileSdk = 31
    }

    @Test
    fun `test - default configuration - AndroidSourceSet has associated KotlinSourceSet`() {
        android.sourceSets.all { androidSourceSet -> project.getKotlinSourceSetOrFail(androidSourceSet) }
        project.evaluate()
    }

    @Test
    fun `test - with flavors - AndroidSourceSet has associated KotlinSourceSet`() {
        android.flavorDimensions.add("market")
        android.flavorDimensions.add("price")
        android.productFlavors.create("german").dimension = "market"
        android.productFlavors.create("usa").dimension = "market"
        android.productFlavors.create("paid").dimension = "price"
        android.productFlavors.create("free").dimension = "price"

        android.sourceSets.all { androidSourceSet ->
            val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
            assertEquals(androidSourceSet.name, kotlinSourceSet.name)
        }
    }

    @Test
    fun `AndroidSourceSet kotlin AndroidSourceDirectorySet`() {
        project.evaluate()
        android.libraryVariants.all { variant ->
            val main = variant.sourceSets.first { it.name == "main" }
            assertEquals(
                project.files("src/main/kotlin", "src/main/java").toSet(),
                main.kotlinDirectories.toSet()
            )
        }

        android.unitTestVariants.all { variant ->
            val test = variant.sourceSets.first { it.name == "test" }
            assertEquals(
                project.files("src/test/kotlin", "src/test/java").toSet(),
                test.kotlinDirectories.toSet()
            )
        }

        android.testVariants.all { variant ->
            val androidTest = variant.sourceSets.first { it.name == "androidTest" }
            assertEquals(
                project.files("src/androidTest/kotlin", "src/androidTest/java").toSet(),
                androidTest.kotlinDirectories.toSet()
            )
        }
    }
}