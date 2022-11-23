/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.buildProject
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.dependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.friendSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.regularSourceDependency
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
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

        fun resolveConsumerSourceSet(sourceSetName: String) = consumer.kotlinIdeMultiplatformImport
            .resolveDependencies(consumer.multiplatformExtension.sourceSets.getByName(sourceSetName))
            .filterIsInstance<IdeaKotlinSourceDependency>()

        resolveConsumerSourceSet("commonMain").assertMatches(
            regularSourceDependency(":producer:commonMain")
        )

        resolveConsumerSourceSet("nativeMain").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            dependsOnDependency(":consumer:commonMain"),
        )

        resolveConsumerSourceSet("nativeTest").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            friendSourceDependency(":consumer:commonMain"),
            friendSourceDependency(":consumer:nativeMain"),
            friendSourceDependency(":consumer:linuxMain"),
            dependsOnDependency(":consumer:commonTest"),
        )

        resolveConsumerSourceSet("linuxMain").assertMatches(
            regularSourceDependency(":producer:commonMain"),
            regularSourceDependency(":producer:nativeMain"),
            regularSourceDependency(":producer:linuxMain"),
            dependsOnDependency(":consumer:commonMain"),
            dependsOnDependency(":consumer:nativeMain"),
        )

        resolveConsumerSourceSet("linuxTest").assertMatches(
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
}
