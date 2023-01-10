/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.TestIdeaKpmBinaryDependencyMatcher
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.assertContainsFragment
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.assertIsNotEmpty
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.assertResolvedBinaryDependencies
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmIosArm64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmJvmVariant
import org.jetbrains.kotlin.gradle.unitTests.kpm.applyKpmPlugin
import org.jetbrains.kotlin.gradle.unitTests.kpm.buildIdeaKpmProjectModel
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.junit.Test

class StdlibKotlinIdeaDependencyResolutionTest : AbstractLightweightIdeaDependencyResolutionTest() {

    @Test
    fun `test - simple ios and jvm project`() {
        val project = buildProject()

        val kotlin = project.applyKpmPlugin {
            mainAndTest {
                fragments.create("jvm", GradleKpmJvmVariant::class.java)
                val iosX64Variant = fragments.create("iosX64", GradleKpmIosX64Variant::class.java)
                val iosArm64Variant = fragments.create("iosArm64", GradleKpmIosArm64Variant::class.java)
                val iosCommon = fragments.create("iosCommon")

                iosCommon.refines(common)
                iosX64Variant.refines(iosCommon)
                iosArm64Variant.refines(iosCommon)

                dependencies {
                    implementation(kotlin("stdlib-common", "1.6.10"))
                }
            }
        }

        kotlin.buildIdeaKpmProjectModel().assertIsNotEmpty().modules.forEach { module ->
            module.assertContainsFragment("common").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "org.jetbrains.kotlin:kotlin-stdlib-common:1.6.10"
            )

            /* stdlib-common does not automatically add platform dependencies */
            module.assertContainsFragment("jvm").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
            )

            /* native fragments only references dependencies from the native distribution */
            listOf("iosCommon", "iosX64", "iosArm64").forEach { fragmentName ->
                module.assertContainsFragment(fragmentName).assertResolvedBinaryDependencies(
                    CLASSPATH_BINARY_TYPE, TestIdeaKpmBinaryDependencyMatcher.InDirectory(project.konanDistribution.root)
                )
            }
        }
    }

    /**
     * Testing common misconfiguration of depending directly to the regular stdlib in common
     */
    @Test
    fun `test - simple ios and jvm project - dependency to 'stdlib' in common`() {
        val project = buildProject()

        val kotlin = project.applyKpmPlugin {
            mainAndTest {
                fragments.create("jvm", GradleKpmJvmVariant::class.java)
                val iosX64Variant = fragments.create("iosX64", GradleKpmIosX64Variant::class.java)
                val iosArm64Variant = fragments.create("iosArm64", GradleKpmIosArm64Variant::class.java)
                val iosCommon = fragments.create("iosCommon")

                iosCommon.refines(common)
                iosX64Variant.refines(iosCommon)
                iosArm64Variant.refines(iosCommon)

                dependencies {
                    implementation(kotlin("stdlib", "1.6.10"))
                }
            }
        }

        kotlin.buildIdeaKpmProjectModel().assertIsNotEmpty().modules.forEach { module ->
            module.assertContainsFragment("common").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "org.jetbrains.kotlin:kotlin-stdlib-common:1.6.10",
                /*
                Actually not a desired dependency, however a general filtering mechanism cannot be implemented,
                since this variant does not contain attributes that would enable such filter.

                A special filter for the stdlib is not implemented yet.
                 */
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",
            )

            module.assertContainsFragment("jvm").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",
            )

            /* native fragments only references dependencie from the native distribution */
            listOf("iosCommon", "iosX64", "iosArm64").forEach { fragmentName ->
                module.assertContainsFragment(fragmentName).assertResolvedBinaryDependencies(
                    CLASSPATH_BINARY_TYPE,
                    TestIdeaKpmBinaryDependencyMatcher.InDirectory(project.konanDistribution.root),

                    /* Actually not correct as well, since those are jvm dependencies. Filtering is not easily possible here, as well */
                    "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                    "org.jetbrains:annotations:13.0",
                )
            }
        }
    }

    @Test
    fun `test simple ios and jvm project - with default stdlib dependency`() {
        val project = buildProject()
        project.enableDefaultStdlibDependency(true)

        val kotlin = project.applyKpmPlugin {
            mainAndTest {
                fragments.create("jvm", GradleKpmJvmVariant::class.java)
                val iosX64Variant = fragments.create("iosX64", GradleKpmIosX64Variant::class.java)
                val iosArm64Variant = fragments.create("iosArm64", GradleKpmIosArm64Variant::class.java)
                val iosCommon = fragments.create("iosCommon")

                iosCommon.refines(common)
                iosX64Variant.refines(iosCommon)
                iosArm64Variant.refines(iosCommon)
            }
        }

        kotlin.buildIdeaKpmProjectModel().assertIsNotEmpty().modules.forEach { module ->
            module.assertContainsFragment("common").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "org.jetbrains.kotlin:kotlin-stdlib-common:${project.getKotlinPluginVersion()}"
            )

            module.assertContainsFragment("jvm").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "org.jetbrains.kotlin:kotlin-stdlib:${project.getKotlinPluginVersion()}",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${project.getKotlinPluginVersion()}",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.getKotlinPluginVersion()}",
                Regex("""org\.jetbrains:annotations:.*"""),
            )

            listOf("iosCommon", "iosX64", "iosArm64").forEach { fragmentName ->
                module.assertContainsFragment(fragmentName).assertResolvedBinaryDependencies(
                    CLASSPATH_BINARY_TYPE,
                    TestIdeaKpmBinaryDependencyMatcher.InDirectory(project.konanDistribution.root),
                )
            }
        }
    }
}
