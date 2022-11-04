/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.externalTargetApi

import org.jetbrains.kotlin.gradle.android.androidTargetPrototype
import org.jetbrains.kotlin.gradle.androidApplication
import org.jetbrains.kotlin.gradle.assumeAndroidSdkAvailable
import org.jetbrains.kotlin.gradle.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.kpm.idea.mavenCentralCacheRedirector
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ExternalAndroidTargetPrototypeSmokeTest {

    @BeforeTest
    fun checkSdk() {
        assumeAndroidSdkAvailable()
    }

    @Test
    fun `apply prototype - evaluate - compilations exist`() {
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 31 }
        val androidTargetPrototype = project.multiplatformExtension.androidTargetPrototype()
        project.evaluate()

        assertEquals(
            setOf("main", "unitTest", "instrumentedTest"),
            androidTargetPrototype.compilations.map { it.name }.toSet()
        )
    }

    @Test
    fun `apply prototype - evaluate - configurations can be resolved`() {
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 31 }

        val androidTargetPrototype = project.multiplatformExtension.androidTargetPrototype()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        project.evaluate()

        androidTargetPrototype.compilations.all { compilation ->
            compilation.compileDependencyFiles.files
            compilation.runtimeDependencyFiles?.files
        }
    }
}