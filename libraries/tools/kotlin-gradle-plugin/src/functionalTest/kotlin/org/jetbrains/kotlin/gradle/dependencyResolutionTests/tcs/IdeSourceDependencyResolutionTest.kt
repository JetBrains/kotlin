/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

class IdeSourceDependencyResolutionTest {
    @Test
    fun `test - multiplatform to multiplatform - sample 0`() {
        val root = buildProject()

        val producer = buildProject({ withParent(root).withName("producer") }) {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()

            multiplatformExtension.apply {
                applyDefaultHierarchyTemplate()
                linuxX64()
                linuxArm64()
                jvm()
            }
        }

        val consumer = buildProject({ withParent(root).withName("consumer") }) {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()

            multiplatformExtension.apply {
                applyDefaultHierarchyTemplate()
                linuxX64()
                linuxArm64()
                jvm()

                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":producer"))
                }
            }
        }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.resolveDependencies("commonMain").assertMatches(
            regularSourceDependency(":producer/commonMain")
        )

        consumer.resolveDependencies("nativeMain").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/nativeMain"),
            regularSourceDependency(":producer/linuxMain"),
            dependsOnDependency(":consumer/commonMain"),
        )

        consumer.resolveDependencies("nativeTest").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/nativeMain"),
            regularSourceDependency(":producer/linuxMain"),
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/nativeMain"),
            friendSourceDependency(":consumer/linuxMain"),
            dependsOnDependency(":consumer/commonTest"),
        )

        consumer.resolveDependencies("linuxMain").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/nativeMain"),
            regularSourceDependency(":producer/linuxMain"),
            dependsOnDependency(":consumer/commonMain"),
            dependsOnDependency(":consumer/nativeMain"),
        )

        consumer.resolveDependencies("linuxTest").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/nativeMain"),
            regularSourceDependency(":producer/linuxMain"),
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/nativeMain"),
            friendSourceDependency(":consumer/linuxMain"),
            dependsOnDependency(":consumer/commonTest"),
            dependsOnDependency(":consumer/nativeTest"),
        )

        consumer.resolveDependencies("linuxX64Main").assertMatches(
            dependsOnDependency(":consumer/commonMain"),
            dependsOnDependency(":consumer/nativeMain"),
            dependsOnDependency(":consumer/linuxMain"),
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/linuxX64/main/klib/producer.klib"))
        )

        consumer.resolveDependencies("linuxX64Test").assertMatches(
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/nativeMain"),
            friendSourceDependency(":consumer/linuxMain"),
            friendSourceDependency(":consumer/linuxX64Main"),
            dependsOnDependency(":consumer/commonTest"),
            dependsOnDependency(":consumer/nativeTest"),
            dependsOnDependency(":consumer/linuxTest"),
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/linuxX64/main/klib/producer.klib"))
        )

        consumer.resolveDependencies("linuxArm64Main").assertMatches(
            dependsOnDependency(":consumer/commonMain"),
            dependsOnDependency(":consumer/nativeMain"),
            dependsOnDependency(":consumer/linuxMain"),
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/linuxArm64/main/klib/producer.klib"))
        )

        consumer.resolveDependencies("linuxArm64Test").assertMatches(
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/nativeMain"),
            friendSourceDependency(":consumer/linuxMain"),
            friendSourceDependency(":consumer/linuxArm64Main"),
            dependsOnDependency(":consumer/commonTest"),
            dependsOnDependency(":consumer/nativeTest"),
            dependsOnDependency(":consumer/linuxTest"),
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/linuxArm64/main/klib/producer.klib"))
        )
    }

    @Test
    fun `test - multiplatform to multiplatform - sample 1 - jvmAndAndroid`() {
        assertAndroidSdkAvailable()
        val root = buildProject()

        fun Project.setup() {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()
            androidLibrary { compileSdk = 33 }

            multiplatformExtension.apply {
                applyDefaultHierarchyTemplate()
                linuxX64()
                linuxArm64()
                jvm()
                androidTarget()

                sourceSets.getByName("commonMain").let { commonMain ->
                    sourceSets.create("jvmAndAndroidMain").let { jvmAndAndroidMain ->
                        jvmAndAndroidMain.dependsOn(commonMain)
                        sourceSets.getByName("jvmMain").dependsOn(jvmAndAndroidMain)
                        sourceSets.getByName("androidMain").dependsOn(jvmAndAndroidMain)
                    }
                }
            }
        }

        val producer = buildProject({ withParent(root).withName("producer") }, Project::setup)
        val consumer = buildProject({ withParent(root).withName("consumer") }, Project::setup)
        consumer.multiplatformExtension.sourceSets.getByName("commonMain").dependencies {
            implementation(project(":producer"))
        }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.resolveDependencies("jvmAndAndroidMain").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/jvmAndAndroidMain"),
            dependsOnDependency(":consumer/commonMain"),
        )
    }

    @Test
    fun `test - multiplatform to kotlin jvm - sample 0`() {
        val root = buildProject()

        val producer = buildProject({ withParent(root).withName("producer") }) {
            enableDefaultStdlibDependency(false)
            applyKotlinJvmPlugin()
        }

        val consumer = buildProject({ withParent(root).withName("consumer") }) {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()
            multiplatformExtension.jvm()
            multiplatformExtension.sourceSets.getByName("commonMain").dependencies {
                implementation(project(":producer"))
            }
        }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.resolveDependencies("commonMain").assertMatches(
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/build/libs/producer.jar"))
        )

        consumer.resolveDependencies("jvmMain").assertMatches(
            dependsOnDependency(":consumer/commonMain"),
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/build/libs/producer.jar"))
        )

        consumer.resolveDependencies("jvmTest").assertMatches(
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/jvmMain"),
            dependsOnDependency(":consumer/commonTest"),
            projectArtifactDependency(Regular, ":producer", FilePathRegex(".*/build/libs/producer.jar"))
        )
    }
}

private fun Project.resolveDependencies(sourceSetName: String): Iterable<IdeaKotlinDependency> {
    return kotlinIdeMultiplatformImport
        .resolveDependencies(multiplatformExtension.sourceSets.getByName(sourceSetName))
        .filter { it !is IdeaKotlinBinaryDependency }
}
