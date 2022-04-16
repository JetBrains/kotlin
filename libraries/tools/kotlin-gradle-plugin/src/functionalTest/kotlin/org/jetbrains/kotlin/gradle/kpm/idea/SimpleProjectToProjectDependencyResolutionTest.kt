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

            fun ifMainModule(vararg any: Any?) =
                listOf(*any).takeIf { module.name == KotlinGradleModule.MAIN_MODULE_NAME }

            module.assertContainsFragment("common").assertSourceDependencies(
                "regular::producer/main/common",
                ifTestModule("friend::consumer/main/common")
            )

            module.assertContainsFragment("jvm").assertSourceDependencies(
                "regular::producer/main/jvm",
                "regular::producer/main/common",
                ifMainModule(
                    "refines::consumer/main/common",
                ),
                ifTestModule(
                    "friend::consumer/main/common",
                    "friend::consumer/main/jvm",
                    "refines::consumer/test/common",
                )
            )

            module.assertContainsFragment("nativeCommon").assertSourceDependencies(
                "regular::producer/main/common",
                "regular::producer/main/nativeCommon",
                ifMainModule(
                    "refines::consumer/main/common"
                ),
                ifTestModule(
                    "friend::consumer/main/common",
                    "friend::consumer/main/nativeCommon",
                    "refines::consumer/test/common",
                )
            )

            module.assertContainsFragment("appleCommon").assertSourceDependencies(
                "regular::producer/main/common",
                "regular::producer/main/appleCommon",
                "regular::producer/main/nativeCommon",
                ifMainModule(
                    "refines::consumer/main/common",
                    "refines::consumer/main/nativeCommon"
                ),
                ifTestModule(
                    "friend::consumer/main/common",
                    "friend::consumer/main/nativeCommon",
                    "friend::consumer/main/appleCommon",
                    "refines::consumer/test/common",
                    "refines::consumer/test/nativeCommon"
                )
            )

            module.assertContainsFragment("linuxX64").assertSourceDependencies(
                "regular::producer/main/common",
                "regular::producer/main/nativeCommon",
                "regular::producer/main/linuxX64",
                ifMainModule(
                    "refines::consumer/main/common",
                    "refines::consumer/main/nativeCommon",
                ),
                ifTestModule(
                    "friend::consumer/main/common",
                    "friend::consumer/main/nativeCommon",
                    "friend::consumer/main/linuxX64",
                    "refines::consumer/test/common",
                    "refines::consumer/test/nativeCommon"
                )
            )

            module.assertContainsFragment("macosX64").assertSourceDependencies(
                "regular::producer/main/macosX64",
                "regular::producer/main/common",
                "regular::producer/main/appleCommon",
                "regular::producer/main/nativeCommon",
                ifMainModule(
                    "refines::consumer/main/common",
                    "refines::consumer/main/nativeCommon",
                    "refines::consumer/main/appleCommon"
                ),
                ifTestModule(
                    "friend::consumer/main/common",
                    "friend::consumer/main/nativeCommon",
                    "friend::consumer/main/appleCommon",
                    "friend::consumer/main/macosX64",
                    "refines::consumer/test/common",
                    "refines::consumer/test/nativeCommon",
                    "refines::consumer/test/appleCommon"
                )
            )

            module.assertContainsFragment("iosX64").assertSourceDependencies(
                "regular::producer/main/iosX64",
                "regular::producer/main/common",
                "regular::producer/main/iosMain",
                "regular::producer/main/appleCommon",
                "regular::producer/main/nativeCommon",
                ifMainModule(
                    "refines::consumer/main/common",
                    "refines::consumer/main/nativeCommon",
                    "refines::consumer/main/appleCommon"
                ),
                ifTestModule(
                    "friend::consumer/main/common",
                    "friend::consumer/main/nativeCommon",
                    "friend::consumer/main/appleCommon",
                    "friend::consumer/main/iosX64",
                    "refines::consumer/test/common",
                    "refines::consumer/test/nativeCommon",
                    "refines::consumer/test/appleCommon"
                )
            )
        }
    }
}
