/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.applyKpmPlugin
import org.jetbrains.kotlin.gradle.kpm.buildIdeaKotlinProjectModel
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.assertContainsFragment
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.assertIsNotEmpty
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.assertSourceDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.junit.Test

class SimpleProjectToProjectDependencyResolutionTest : AbstractLightweightIdeaDependencyResolutionTest() {

    @Test
    fun `test - simple producer and consumer projects`() {
        val root = buildProject()
        val producer = buildProject { withParent(root).withName("producer") }
        val consumer = buildProject { withParent(root).withName("consumer") }
        consumer.projectDir.mkdirs()
        consumer.projectDir.deleteOnExit()

        producer.applyKpmPlugin {
            mainAndTest {
                fragments.create("jvm", KotlinJvmVariant::class.java)
                val linuxX64 = fragments.create("linuxX64", KotlinLinuxX64Variant::class.java)
                val iosX64 = fragments.create("iosX64", KotlinIosX64Variant::class.java)
                val iosArm64 = fragments.create("iosArm64", KotlinIosArm64Variant::class.java)
                val macos64 = fragments.create("macosX64", KotlinMacosX64Variant::class.java)

                val iosCommon = fragments.create("iosMain") {
                    it.refines(common)
                    iosX64.refines(it)
                    iosArm64.refines(it)
                }

                val appleCommon = fragments.create("appleCommon") {
                    it.refines(common)
                    iosCommon.refines(it)
                    macos64.refines(it)
                }

                fragments.create("nativeCommon") {
                    it.refines(common)
                    appleCommon.refines(it)
                    linuxX64.refines(it)
                }
            }
        }

        val consumerKotlin = consumer.applyKpmPlugin {
            mainAndTest {
                fragments.create("jvm", KotlinJvmVariant::class.java)
                val linuxX64 = fragments.create("linuxX64", KotlinLinuxX64Variant::class.java)
                val iosX64 = fragments.create("iosX64", KotlinIosX64Variant::class.java)
                val macosX64 = fragments.create("macosX64", KotlinMacosX64Variant::class.java)

                val appleCommon = fragments.create("appleCommon") {
                    it.refines(common)
                    iosX64.refines(it)
                    macosX64.refines(it)
                }

                fragments.create("nativeCommon") {
                    it.refines(common)
                    appleCommon.refines(it)
                    linuxX64.refines(it)
                }

                dependencies {
                    implementation(project(":producer"))
                }
            }
        }

        consumerKotlin.buildIdeaKotlinProjectModel().assertIsNotEmpty().modules.forEach { module ->
            fun ifTestModule(vararg any: Any?) =
                listOf(*any).takeIf { module.name == KotlinGradleModule.TEST_MODULE_NAME }

            module.assertContainsFragment("common").assertSourceDependencies(
                ":producer/main/common",
                ifTestModule(":consumer/main/common")
            )

            module.assertContainsFragment("jvm").assertSourceDependencies(
                ":producer/main/jvm",
                ":producer/main/common",
                ifTestModule(
                    ":consumer/main/common",
                    ":consumer/main/jvm"
                )
            )

            module.assertContainsFragment("nativeCommon").assertSourceDependencies(
                ":producer/main/common",
                ":producer/main/nativeCommon",
                ifTestModule(
                    ":consumer/main/common",
                    ":consumer/main/nativeCommon"
                )
            )

            module.assertContainsFragment("appleCommon").assertSourceDependencies(
                ":producer/main/common",
                ":producer/main/appleCommon",
                ":producer/main/nativeCommon",
                ifTestModule(
                    ":consumer/main/common",
                    ":consumer/main/nativeCommon",
                    ":consumer/main/appleCommon"
                )
            )

            module.assertContainsFragment("linuxX64").assertSourceDependencies(
                ":producer/main/common",
                ":producer/main/nativeCommon",
                ":producer/main/linuxX64",
                ifTestModule(
                    ":consumer/main/common",
                    ":consumer/main/nativeCommon",
                    ":consumer/main/linuxX64"
                )
            )

            module.assertContainsFragment("macosX64").assertSourceDependencies(
                ":producer/main/macosX64",
                ":producer/main/common",
                ":producer/main/appleCommon",
                ":producer/main/nativeCommon",
                ifTestModule(
                    ":consumer/main/common",
                    ":consumer/main/nativeCommon",
                    ":consumer/main/appleCommon",
                    ":consumer/main/macosX64"
                )
            )

            module.assertContainsFragment("iosX64").assertSourceDependencies(
                ":producer/main/iosX64",
                ":producer/main/common",
                ":producer/main/iosMain",
                ":producer/main/appleCommon",
                ":producer/main/nativeCommon",
                ifTestModule(
                    ":consumer/main/common",
                    ":consumer/main/nativeCommon",
                    ":consumer/main/appleCommon",
                    ":consumer/main/iosX64"
                )
            )
        }
    }
}
