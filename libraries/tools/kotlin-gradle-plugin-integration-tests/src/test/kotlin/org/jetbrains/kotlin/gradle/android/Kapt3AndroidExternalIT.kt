/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.Kapt3BaseIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.appendText

@DisplayName("android with kapt3 external dependencies tests")
@AndroidGradlePluginTests
class Kapt3AndroidExternalIT : Kapt3BaseIT() {

    // Deprecated and doesn't work with Gradle 8 + AGP 8, so keeping max Gradle version as 7.6
    // For example: https://github.com/JakeWharton/butterknife/issues/1686
    @DisplayName("kapt works with butterknife")
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_7_6)
    @AndroidTestVersions(maxVersion = TestVersions.AGP.AGP_74)
    @GradleAndroidTest
    fun testButterKnife(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-butterknife".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.java")

                val butterknifeJavaClassesDir = "app/build/intermediates/javac/debug/classes/org/example/kotlin/butterknife/"
                assertFileInProjectExists(butterknifeJavaClassesDir + "SimpleActivity\$\$ViewBinder.class")

                assertFileInProjectExists("app/build/tmp/kotlin-classes/debug/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")
            }

            build("assembleDebug") {
                assertTasksUpToDate(":app:compileDebugKotlin", ":app:compileDebugJavaWithJavac")
            }
        }
    }

    @DisplayName("kapt works with dagger")
    @GradleAndroidTest
    fun testDagger(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/DaggerApplicationComponent.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/ui/HomeActivity_MembersInjector.java")

                val daggerJavaClassesDir =
                    "app/build/intermediates/javac/debug/classes/com/example/dagger/kotlin/"

                assertFileInProjectExists(daggerJavaClassesDir + "DaggerApplicationComponent.class")

                assertFileInProjectExists("app/build/tmp/kotlin-classes/debug/com/example/dagger/kotlin/AndroidModule.class")
            }
        }
    }

    @DisplayName("kapt works with DbFlow")
    @GradleAndroidTest
    fun testDbFlow(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dbflow".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/raizlabs/android/dbflow/config/GeneratedDatabaseHolder.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/raizlabs/android/dbflow/config/AppDatabaseAppDatabase_Database.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/mobi/porquenao/poc/kotlin/core/Item_Table.java")
            }
        }
    }

    @DisplayName("kapt works with realm")
    @GradleAndroidTest
    fun testRealm(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val realmVersion = if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_7_5)) {
            "10.13.0-transformer-api"
        } else {
            "10.13.0"
        }
        project(
            "android-realm".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, freeArgs = listOf("-Prealm_version=$realmVersion")),
            buildJdk = jdkVersion.location,
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/io_realm_examples_kotlin_model_CatRealmProxy.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/io_realm_examples_kotlin_model_CatRealmProxyInterface.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/io_realm_examples_kotlin_model_DogRealmProxy.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/io_realm_examples_kotlin_model_DogRealmProxyInterface.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/io_realm_examples_kotlin_model_PersonRealmProxy.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/io_realm_examples_kotlin_model_PersonRealmProxyInterface.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModule.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModuleMediator.java")
            }
        }
    }

    @DisplayName("kapt works with databinding")
    @GradleAndroidTest
    fun testDatabinding(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-databinding".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            // TODO: remove the `if` when we drop support for [TestVersions.AGP.AGP_42]
            buildJdk = if (jdkVersion.version >= JavaVersion.VERSION_11) jdkVersion.location else File(System.getProperty("jdk11Home"))
        ) {
            // Remove the once minimal supported AGP version will be 8.1.0: https://issuetracker.google.com/issues/260059413
            gradleProperties.appendText(
                """
                |kotlin.jvm.target.validation.mode=warning
                """.trimMargin()
            )

            build(
                "assembleDebug", "assembleAndroidTest",
            ) {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/databinding/BR.java")

                // databinding compiler v2 was introduced in AGP 3.1.0, was enabled by default in AGP 3.2.0
                assertOutputContains("-Aandroid.databinding.enableV2=1")
                assertFileInProjectNotExists("library/build/generated/source/kapt/debugAndroidTest/android/databinding/DataBinderMapperImpl.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBindingImpl.java")

                // KT-23866
                assertOutputDoesNotContain("The following options were not recognized by any processor")
            }
        }
    }

    @DisplayName("KT-30735: kapt works with androidx.navigation.safeargs")
    @GradleAndroidTest
    fun testAndroidxNavigationSafeArgs(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "androidx-navigation-safe-args".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val safeArgsVersion = if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_7_0)) "2.5.3" else "2.3.5"
            build("assembleDebug", "-Psafe_args_version=$safeArgsVersion") {
                assertFileInProjectExists("build/generated/source/navigation-args/debug/test/androidx/navigation/StartFragmentDirections.java")
                assertFileInProjectExists("build/tmp/kotlin-classes/debug/test/androidx/navigation/StartFragmentKt.class")
            }
        }
    }

    @DisplayName("kapt works with androidx")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_42, maxVersion = TestVersions.AGP.AGP_42)
    fun testDatabindingWithAndroidX(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-databinding-androidX".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("kaptDebugKotlin") {
                assertKaptSuccessful()
            }
        }
    }
}