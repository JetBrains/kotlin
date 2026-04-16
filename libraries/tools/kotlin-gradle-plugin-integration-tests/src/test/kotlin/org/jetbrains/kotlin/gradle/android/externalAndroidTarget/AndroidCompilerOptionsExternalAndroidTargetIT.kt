/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.androidLibrary
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.testbase.*

@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_813)
@AndroidGradlePluginTests
class AndroidCompilerOptionsExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `androidLibrary compilerOptions propagate to Android compilation`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                        compilerOptions {
                            optIn.add("kotlin.RequiresOptIn")
                            freeCompilerArgs.add("-Xexpect-actual-classes")
                            progressiveMode.set(true)
                            allWarningsAsErrors.set(true)
                        }
                    }
                    iosArm64()

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample
                        class SimpleSource
                        """.trimIndent()
                    )
                }
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-opt-in=kotlin.RequiresOptIn", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-Xexpect-actual-classes", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-progressive", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-Werror", LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `kotlinMultiplatform compilerOptions propagate to Android compilation`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                    }
                    iosArm64()

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample
                        class SimpleSource
                        """.trimIndent()
                    )

                    compilerOptions {
                        progressiveMode.set(true)
                        allWarningsAsErrors.set(true)
                        optIn.add("kotlin.RequiresOptIn")
                        freeCompilerArgs.add("-Xexpect-actual-classes")
                    }
                }
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-opt-in=kotlin.RequiresOptIn", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-Xexpect-actual-classes", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-progressive", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-Werror", LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `compilation-level compilerOptions propagate to Android compilation`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                    }
                    iosArm64()

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample
                        class CompilationLevelProbe
                        """.trimIndent()
                    )

                    val androidMainCompilation = targets.getByName("android").compilations.getByName("main")
                    androidMainCompilation.compileTaskProvider.configure {
                        compilerOptions {
                            progressiveMode.set(true)
                            allWarningsAsErrors.set(true)
                        }
                    }
                }
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-progressive", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-Werror", LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `compilation-level compilerOptions propagate to Android host test compilation`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                        withHostTest { }
                    }
                    iosArm64()

                    sourceSets.getByName("androidHostTest").compileSource(
                        """
                        package sample
                        class HostCompilationLevelProbe
                        """.trimIndent()
                    )

                    val androidHostTestCompilation = targets.getByName("android").compilations.getByName("hostTest")
                    androidHostTestCompilation.compileTaskProvider.configure {
                        compilerOptions {
                            progressiveMode.set(true)
                            allWarningsAsErrors.set(true)
                        }
                    }
                }
            }
            build(":compileAndroidHostTest") {
                assertTasksExecuted(":compileAndroidHostTest")
                assertCompilerArgument(":compileAndroidHostTest", "-progressive", LogLevel.INFO)
                assertCompilerArgument(":compileAndroidHostTest", "-Werror", LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `androidLibrary compilerOptions override kotlinMultiplatform allWarningsAsErrors`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    compilerOptions {
                        allWarningsAsErrors.set(false)
                    }
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                        compilerOptions {
                            allWarningsAsErrors.set(true)
                        }
                    }
                    iosArm64()

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample
                        class OverrideProbe
                        """.trimIndent()
                    )
                }
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Werror", LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `androidLibrary jvmTarget is honored`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                        compilerOptions {
                            jvmTarget.set(JvmTarget.JVM_1_8)
                        }
                    }
                    iosArm64()

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample
                        class JvmTargetProbe { fun ok() = 42 }
                        """.trimIndent()
                    )
                }
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-jvm-target 1.8", LogLevel.INFO)
            }
        }
    }
}
