/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeJvmAndAndroidPlatformBinaryDependencyResolver
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.BeforeTest
import kotlin.test.Test

class IdeJvmAndAndroidPlatformDependencyResolverTest {

    @BeforeTest
    fun checkEnvironment() {
        assumeAndroidSdkAvailable()
    }

    @Test
    fun `test - MVIKotlin - on jvmAndAndroidMain`() {
        val project = buildProject {
            enableDefaultStdlibDependency(false)
            enableDependencyVerification(false)
            applyMultiplatformPlugin()
            plugins.apply("com.android.library")
            androidExtension.compileSdkVersion(33)
            repositories.mavenCentralCacheRedirector()
        }

        val kotlin = project.multiplatformExtension
        kotlin.targetHierarchy.custom {
            common {
                group("jvmAndAndroid") {
                    anyJvm()
                    anyAndroid()
                }
            }
        }

        kotlin.jvm()
        kotlin.android()

        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        val jvmAndAndroidDependencies = listOf(
            binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin-jvm:3.0.2"),
            binaryCoordinates("com.arkivanov.essenty:lifecycle-jvm:0.4.2"),
            binaryCoordinates("com.arkivanov.essenty:instance-keeper-jvm:0.4.2"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:1.7.10"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
            /* This resolver cannot differentiate between non-hmpp metadata libraries and platform libraries. This is OK */
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-common:1.7.10")
        )

        IdeJvmAndAndroidPlatformBinaryDependencyResolver(project).resolve(kotlin.sourceSets.getByName("jvmAndAndroidMain"))
            .assertMatches(jvmAndAndroidDependencies)

        IdeJvmAndAndroidPlatformBinaryDependencyResolver(project).resolve(kotlin.sourceSets.getByName("jvmAndAndroidTest"))
            .assertMatches(jvmAndAndroidDependencies)
    }
}
