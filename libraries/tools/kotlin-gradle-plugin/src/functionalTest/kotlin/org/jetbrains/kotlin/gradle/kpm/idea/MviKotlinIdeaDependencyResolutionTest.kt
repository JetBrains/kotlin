/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.kpm.idea

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.android.androidPrototype
import org.jetbrains.kotlin.gradle.kpm.applyKpmPlugin
import org.jetbrains.kotlin.gradle.kpm.buildIdeaKotlinProjectModel
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinIosArm64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinJvmVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test

class MviKotlinIdeaDependencyResolutionTest : AbstractLightweightIdeaDependencyResolutionTest() {

    @Test
    fun `test - simple ios and jvm project`() {
        val project = buildProject()

        val kotlin = project.applyKpmPlugin {
            mainAndTest {
                fragments.create("jvm", KotlinJvmVariant::class.java)
                val iosX64Variant = fragments.create("iosX64", KotlinIosX64Variant::class.java)
                val iosArm64Variant = fragments.create("iosArm64", KotlinIosArm64Variant::class.java)
                val iosCommon = fragments.create("iosCommon")

                iosCommon.refines(common)
                iosX64Variant.refines(iosCommon)
                iosArm64Variant.refines(iosCommon)

                dependencies {
                    implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.0-beta01")
                }
            }
        }

        kotlin.buildIdeaKotlinProjectModel().assertIsNotEmpty().modules.forEach { module ->
            module.assertContainsFragment("common").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "com.arkivanov.mvikotlin:mvikotlin:3.0.0-beta01:main:commonMain",
                "com.arkivanov.essenty:lifecycle:0.2.2:main:commonMain",
                "com.arkivanov.essenty:instance-keeper:0.2.2:main:commonMain",
                "org.jetbrains.kotlin:kotlin-stdlib-common:1.6.10"
            )

            module.assertContainsFragment("jvm").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "com.arkivanov.mvikotlin:mvikotlin-jvm:3.0.0-beta01",
                "com.arkivanov.essenty:lifecycle-jvm:0.2.2",
                "com.arkivanov.essenty:instance-keeper-jvm:0.2.2",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.10",
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",
            )

            module.assertContainsFragment("iosCommon").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                project.konanDistribution.stdlib,
                "com.arkivanov.mvikotlin:mvikotlin:3.0.0-beta01:main:commonMain",
                "com.arkivanov.mvikotlin:mvikotlin:3.0.0-beta01:main:jsNativeMain",
                "com.arkivanov.essenty:lifecycle:0.2.2:main:commonMain",
                "com.arkivanov.essenty:instance-keeper:0.2.2:main:commonMain",
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",

                /*
                The following dependencies are actually marked as 'implementation' on mvikotlin.
                We still resolve those dependencies, because this source set is shared native and
                the shared native compiler expects those dependencies as well.

                Including those dependencies for IDE analysis is up for discussion.
                */
                "com.arkivanov.mvikotlin:utils-internal:3.0.0-beta01:main:commonMain",
                "com.arkivanov.mvikotlin:utils-internal:3.0.0-beta01:main:darwinMain",
                "com.arkivanov.mvikotlin:utils-internal:3.0.0-beta01:main:nativeMain",
                "com.arkivanov.mvikotlin:rx:3.0.0-beta01:main:commonMain",
                "com.arkivanov.mvikotlin:rx-internal:3.0.0-beta01:main:commonMain",
                "com.arkivanov.mvikotlin:rx-internal:3.0.0-beta01:main:darwinMain",
                "com.arkivanov.mvikotlin:rx-internal:3.0.0-beta01:main:nativeMain",
                "com.arkivanov.essenty:utils-internal:0.2.2:main:commonMain",
                "com.arkivanov.essenty:utils-internal:0.2.2:main:nativeMain",
            )

            module.assertContainsFragment("iosX64").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                project.konanDistribution.stdlib,
                IdeaKotlinDependencyMatcher.InDirectory(project.konanDistribution.platformLibsDir.resolve(KonanTarget.IOS_X64.name)),
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",
                "com.arkivanov.mvikotlin:mvikotlin-iosx64:3.0.0-beta01",
                "com.arkivanov.essenty:lifecycle-iosx64:0.2.2",
                "com.arkivanov.essenty:instance-keeper-iosx64:0.2.2",

                /* Internals are listed here as well. See comment above */
                "com.arkivanov.mvikotlin:rx-internal-iosx64:3.0.0-beta01",
                "com.arkivanov.mvikotlin:utils-internal-iosx64:3.0.0-beta01",
                "com.arkivanov.mvikotlin:rx-iosx64:3.0.0-beta01",
                "com.arkivanov.essenty:utils-internal-iosx64:0.2.2",
            )

            module.assertContainsFragment("iosArm64").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                project.konanDistribution.stdlib,
                IdeaKotlinDependencyMatcher.InDirectory(project.konanDistribution.platformLibsDir.resolve(KonanTarget.IOS_ARM64.name)),
                "com.arkivanov.mvikotlin:mvikotlin-iosarm64:3.0.0-beta01",
                "com.arkivanov.essenty:lifecycle-iosarm64:0.2.2",
                "com.arkivanov.essenty:instance-keeper-iosarm64:0.2.2",

                /* Internals are listed here as well. See comment above */
                "com.arkivanov.mvikotlin:rx-internal-iosarm64:3.0.0-beta01",
                "com.arkivanov.mvikotlin:utils-internal-iosarm64:3.0.0-beta01",
                "com.arkivanov.mvikotlin:rx-iosarm64:3.0.0-beta01",
                "com.arkivanov.essenty:utils-internal-iosarm64:0.2.2",
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",
            )
        }
    }

    @Test
    fun `test - android and jvm`() {
        val project = buildProject()

        /* Setup Android */
        project.plugins.apply(LibraryPlugin::class.java)
        val android = project.extensions.getByType(LibraryExtension::class.java)
        android.compileSdkVersion(30)


        val kotlin = project.applyKpmPlugin {
            androidPrototype()
            jvm {}
            mainAndTest {
                dependencies {
                    implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.0-beta01")
                }
            }
        }

        /* Android requires project to evaluate */
        project.evaluate()

        kotlin.buildIdeaKotlinProjectModel().assertIsNotEmpty().assertContainsModule("main").let { module ->
            listOf("common", "jvm").forEach { fragmentName ->
                module.assertContainsFragment(fragmentName).assertResolvedBinaryDependencies(
                    CLASSPATH_BINARY_TYPE,
                    "com.arkivanov.mvikotlin:mvikotlin-jvm:3.0.0-beta01",
                    "com.arkivanov.essenty:lifecycle-jvm:0.2.2",
                    "com.arkivanov.essenty:instance-keeper-jvm:0.2.2",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.10",
                    "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                    "org.jetbrains:annotations:13.0",
                )
            }

            listOf("androidCommon", "androidRelease").forEach { fragmentName ->
                module.assertContainsFragment(fragmentName).assertResolvedBinaryDependencies(
                    CLASSPATH_BINARY_TYPE,
                    "com.arkivanov.mvikotlin:mvikotlin-android:3.0.0-beta01",
                    "com.arkivanov.essenty:lifecycle-android:0.2.2",
                    "com.arkivanov.essenty:instance-keeper-android:0.2.2",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.10",
                    "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                    "org.jetbrains:annotations:13.0",
                    android.bootClasspath
                )
            }

            module.assertContainsFragment("androidDebug").assertResolvedBinaryDependencies(
                CLASSPATH_BINARY_TYPE,
                "com.arkivanov.mvikotlin:mvikotlin-android-debug:3.0.0-beta01",
                "com.arkivanov.essenty:lifecycle-android-debug:0.2.2",
                "com.arkivanov.essenty:instance-keeper-android-debug:0.2.2",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.10",
                "org.jetbrains.kotlin:kotlin-stdlib:1.6.10",
                "org.jetbrains:annotations:13.0",
                android.bootClasspath
            )
        }
    }
}
