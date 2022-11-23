/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.ide

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.dependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.friendSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.regularSourceDependency
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import kotlin.test.Ignore
import kotlin.test.Test

class IdeResolveSourceDependenciesTest {
    @Test
    fun `test - multiplatform to multiplatform - sample 0`() {
        val root = buildProject()

        val producer = buildProject({ withParent(root).withName("producer") }) {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()

            multiplatformExtension.apply {
                targetHierarchy.default()
                linuxX64()
                linuxArm64()
                jvm()
            }
        }

        val consumer = buildProject({ withParent(root).withName("consumer") }) {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()

            multiplatformExtension.apply {
                targetHierarchy.default()
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

        consumer.resolveSourceDependencies("commonMain").assertMatches(
            regularSourceDependency(":producer:commonMain")
        )

        consumer.resolveSourceDependencies("nativeMain").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            dependsOnDependency(":consumer:commonMain"),
        )

        consumer.resolveSourceDependencies("nativeTest").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            friendSourceDependency(":consumer:commonMain"),
            friendSourceDependency(":consumer:nativeMain"),
            friendSourceDependency(":consumer:linuxMain"),
            dependsOnDependency(":consumer:commonTest"),
        )

        consumer.resolveSourceDependencies("linuxMain").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            dependsOnDependency(":consumer:commonMain"),
            dependsOnDependency(":consumer:nativeMain"),
        )

        consumer.resolveSourceDependencies("linuxTest").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            friendSourceDependency(":consumer:commonMain"),
            friendSourceDependency(":consumer:nativeMain"),
            friendSourceDependency(":consumer:linuxMain"),
            dependsOnDependency(":consumer:commonTest"),
            dependsOnDependency(":consumer:nativeTest"),
        )
    }

    @Ignore("No solution yet")
    @Test
    fun `test - multiplatform to multiplatform - sample 1 - jvmAndAndroid`() {
        assumeAndroidSdkAvailable()
        val root = buildProject()

        fun Project.setup() {
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()
            androidLibrary { compileSdk = 33 }

            multiplatformExtension.apply {
                targetHierarchy.default {
                    common {
                        group("jvmAndAndroid") {
                            jvm()
                            android()
                        }
                    }
                }
                linuxX64()
                linuxArm64()
                jvm()
                android()
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

        consumer.resolveSourceDependencies("jvmAndAndroidMain").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:jvmAndAndroidMain"),
            dependsOnDependency(":consumer:commonMain")
        )
    }
}

private fun Project.resolveSourceDependencies(sourceSetName: String): List<IdeaKotlinSourceDependency> {
    return kotlinIdeMultiplatformImport
        .resolveDependencies(multiplatformExtension.sourceSets.getByName(sourceSetName))
        .filterIsInstance<IdeaKotlinSourceDependency>()
}
