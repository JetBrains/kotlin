/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KaptBaseIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@DisplayName("kapt with AGP 9 com.android.legacy-kapt plugin")
@AndroidGradlePluginTests
class KaptAndroidLegacyKaptIT : KaptBaseIT() {

    @DisplayName("kapt generates sources via com.android.legacy-kapt on AGP 9 new DSL")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
    fun testLegacyKaptGeneratesSources(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                enableLegacyAgpDsl = false,
            ),
            buildJdk = jdkVersion.location,
        ) {
            setUpLegacyKaptDaggerProject()

            build(":kaptDebugKotlin") {
                assertKaptSuccessful()
                assertFileInProjectExists("build/generated/source/kapt/debug/com/example/legacykapt/DaggerAppComponent.java")
            }
        }
    }
}

/**
 * Assembles an Android library that uses AGP 9 built-in Kotlin (the standalone `kotlin-android` /
 * `kotlin-kapt` plugins are incompatible with it) plus AGP's `com.android.legacy-kapt` plugin to run
 * a kapt annotation processor (dagger). Built entirely through the build-script-injection framework so
 * no dedicated project has to be checked into test resources.
 *
 * Requires `enableLegacyAgpDsl = false` on the build options — `com.android.legacy-kapt` only has a role
 * when AGP's built-in Kotlin is active.
 */
internal fun TestProject.setUpLegacyKaptDaggerProject() {
    plugins {
        id("com.android.library")
        id("com.android.legacy-kapt")
    }
    buildScriptInjection {
        androidLibrary.compileSdk = 34
        androidLibrary.namespace = "com.example.legacykapt"
        dependencies.add("implementation", "com.google.dagger:dagger:2.24")
        dependencies.add("kapt", "com.google.dagger:dagger-compiler:2.24")
        dependencies.add("compileOnly", "javax.annotation:javax.annotation-api:1.3.2")
    }

    projectPath.resolve("src/main").also { it.createDirectories() }
        .resolve("AndroidManifest.xml").writeText("<manifest/>\n")
    kotlinSourcesDir().source("com/example/legacykapt/App.kt") {
        """
        package com.example.legacykapt

        import dagger.Component
        import dagger.Module
        import dagger.Provides
        import javax.inject.Singleton

        @Module
        class AppModule {
            @Provides
            @Singleton
            fun provideGreeting(): String = "Hello from legacy-kapt"
        }

        @Singleton
        @Component(modules = [AppModule::class])
        interface AppComponent {
            fun greeting(): String
        }
        """.trimIndent()
    }
}
