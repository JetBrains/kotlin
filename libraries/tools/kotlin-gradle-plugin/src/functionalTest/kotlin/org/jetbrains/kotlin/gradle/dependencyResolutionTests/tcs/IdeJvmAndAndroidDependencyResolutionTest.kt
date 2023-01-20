/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeJvmAndAndroidPlatformBinaryDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.BeforeTest
import kotlin.test.Test

class IdeJvmAndAndroidDependencyResolutionTest {

    @BeforeTest
    fun checkEnvironment() {
        assumeAndroidSdkAvailable()
    }

    private fun Project.configureAndroidAndMultiplatform() {
        enableDefaultStdlibDependency(false)
        enableDependencyVerification(false)
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        applyMultiplatformPlugin()
        plugins.apply("com.android.library")
        androidExtension.compileSdkVersion(33)
        repositories.mavenCentralCacheRedirector()

        project.multiplatformExtension.targetHierarchy.custom {
            common {
                group("jvmAndAndroid") {
                    anyJvm()
                    anyAndroid()
                }
            }
        }

        project.multiplatformExtension.jvm()
        project.multiplatformExtension.android()

    }

    @Test
    fun `test - MVIKotlin - on jvmAndAndroidMain`() {
        val project = buildProject { configureAndroidAndMultiplatform() }
        val kotlin = project.multiplatformExtension
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

    @Test
    fun `test - project to project dependency`() {
        val root = buildProject { setMultiplatformAndroidSourceSetLayoutVersion(2) }
        val producer = buildProject({ withParent(root).withName("producer") }) { configureAndroidAndMultiplatform() }
        val consumer = buildProject({ withParent(root).withName("consumer") }) { configureAndroidAndMultiplatform() }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.multiplatformExtension.sourceSets.getByName("commonMain").dependencies {
            implementation(project(":producer"))
        }

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("jvmAndAndroidMain").assertMatches(
            dependsOnDependency(":consumer/commonMain"),
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/jvmAndAndroidMain"),
        )

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("jvmAndAndroidTest").assertMatches(
            dependsOnDependency(":consumer/commonTest"),
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/jvmAndAndroidMain"),
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/jvmAndAndroidMain"),
        )
    }
}
