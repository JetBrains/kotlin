/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*

// Used AGP 9.0 as the minimal stable version supported for the android library
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
@AndroidGradlePluginTests
class ExplicitApiExternalAndroidTargetIT : KGPBaseTest() {

    // Uses `com.android.kotlin.multiplatform.library`, requires AGP new DSL.
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(enableLegacyAgpDsl = false)

    @GradleAndroidTest
    fun `test - disabled - builds with implicit declarations`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                }
            }
            buildScriptInjection {
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    import android.content.Context
                    
                    class AndroidMain(val context: Context) {
                        val counter = 0
                        fun increment() = counter + 1
                    }
                    """.trimIndent()
                )
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertNoCompilerArgument(":compileAndroidMain", "-Xexplicit-api=", LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `test - strict - fails with implicit declarations in androidMain`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                    explicitApi()
                }
            }
            buildScriptInjection {
                val commonMain = kotlinMultiplatform.sourceSets.getByName("commonMain")
                commonMain.compileSource(
                    """
                    public object CommonMain {
                        public val greeting: String = "Hello"
                        public fun greet(name: String): String = "${'$'}greeting, ${'$'}name"
                    }
                    """.trimIndent()
                )
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    import android.content.Context
                    
                    class AndroidMain(val context: Context) {
                        fun increment() = 1
                    }
                    """.trimIndent()
                )
            }
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=strict", LogLevel.INFO)
                assertOutputDoesNotContain("Visibility must be specified in explicit API mode")
                assertOutputDoesNotContain("Return type must be specified in explicit API mode")
            }
            buildAndFail(":compileAndroidMain") {
                assertTasksFailed(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Xexplicit-api=strict", LogLevel.INFO)
                assertOutputContains("Visibility must be specified in explicit API mode")
                assertOutputContains("Return type must be specified in explicit API mode")
            }
        }
    }

    @GradleAndroidTest
    fun `test - strict - fails with implicit declarations in commonMain`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                    explicitApi()
                }
            }
            buildScriptInjection {
                val commonMain = kotlinMultiplatform.sourceSets.getByName("commonMain")
                commonMain.compileSource(
                    """
                    val version = 1
                    fun compute() = version + 1
                    """.trimIndent()
                )
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    import android.content.Context
                    
                    public class AndroidMain(public val context: Context) {
                        public fun increment(): Int = 1
                    }
                    """.trimIndent()
                )
            }
            buildAndFail(":compileCommonMainKotlinMetadata") {
                assertTasksFailed(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=strict", LogLevel.INFO)
                assertOutputContains("Visibility must be specified in explicit API mode")
                assertOutputContains("Return type must be specified in explicit API mode")
            }
            buildAndFail(":compileAndroidMain") {
                assertTasksFailed(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Xexplicit-api=strict", LogLevel.INFO)
                assertOutputContains("Visibility must be specified in explicit API mode")
                assertOutputContains("Return type must be specified in explicit API mode")
            }
        }
    }

    @GradleAndroidTest
    fun `test - warning - warns on implicit declarations in androidMain`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                    explicitApiWarning()
                }
            }
            buildScriptInjection {
                val commonMain = kotlinMultiplatform.sourceSets.getByName("commonMain")
                commonMain.compileSource(
                    """
                    public object CommonMain {
                        public val greeting: String = "Hello"
                        public fun greet(name: String): String = "${'$'}greeting, ${'$'}name"
                    }
                    """.trimIndent()
                )
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    import android.content.Context
                    
                    class AndroidMain(val context: Context) {
                        fun increment() = 1
                    }
                    """.trimIndent()
                )
            }
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=warning", LogLevel.INFO)
                assertOutputDoesNotContain("Visibility must be specified in explicit API mode")
                assertOutputDoesNotContain("Return type must be specified in explicit API mode")
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Xexplicit-api=warning", LogLevel.INFO)
                assertOutputContains("Visibility must be specified in explicit API mode")
                assertOutputContains("Return type must be specified in explicit API mode")
            }
        }
    }

    @GradleAndroidTest
    fun `test - warning - warns on implicit declarations in commonMain`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                    explicitApiWarning()
                }
            }
            buildScriptInjection {
                val commonMain = kotlinMultiplatform.sourceSets.getByName("commonMain")
                commonMain.compileSource(
                    """
                    val version = 1
                    fun compute() = version + 1
                    """.trimIndent()
                )
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    import android.content.Context
                    
                    public class AndroidMain(public val context: Context) {
                        public fun increment(): Int = 1
                    }
                    """.trimIndent()
                )
            }
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=warning", LogLevel.INFO)
                assertOutputContains("Visibility must be specified in explicit API mode")
                assertOutputContains("Return type must be specified in explicit API mode")
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Xexplicit-api=warning", LogLevel.INFO)
                assertOutputContains("Visibility must be specified in explicit API mode")
                assertOutputContains("Return type must be specified in explicit API mode")
            }
        }
    }

    @GradleAndroidTest
    fun `test - warning - builds on explicit declarations`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                    explicitApiWarning()
                }
            }
            buildScriptInjection {
                val commonMain = kotlinMultiplatform.sourceSets.getByName("commonMain")
                commonMain.compileSource(
                    """
                    public object CommonMain {
                        public fun greet(name: String): String = name
                    }
                    """.trimIndent()
                )
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    public class AndroidMain {
                        public fun increment(): Int = 1
                    }
                    """.trimIndent()
                )
            }
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=warning", LogLevel.INFO)
                assertOutputDoesNotContain("Visibility must be specified in explicit API mode")
                assertOutputDoesNotContain("Return type must be specified in explicit API mode")
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Xexplicit-api=warning", LogLevel.INFO)
                assertOutputDoesNotContain("Visibility must be specified in explicit API mode")
                assertOutputDoesNotContain("Return type must be specified in explicit API mode")
            }
        }
    }

    @GradleAndroidTest
    fun `test - strict - builds on explicit declarations`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample",
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosArm64()
                    explicitApi()
                }
            }
            buildScriptInjection {
                val commonMain = kotlinMultiplatform.sourceSets.getByName("commonMain")
                commonMain.compileSource(
                    """
                    public object CommonMain {
                        public fun greet(name: String): String = name
                    }
                    """.trimIndent()
                )
                val androidMain = kotlinMultiplatform.sourceSets.getByName("androidMain")
                androidMain.compileSource(
                    """
                    public class AndroidMain {
                        public fun increment(): Int = 1
                    }
                    """.trimIndent()
                )
            }
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertCompilerArgument(":compileCommonMainKotlinMetadata", "-Xexplicit-api=strict", LogLevel.INFO)
                assertOutputDoesNotContain("Visibility must be specified in explicit API mode")
                assertOutputDoesNotContain("Return type must be specified in explicit API mode")
            }
            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-Xexplicit-api=strict", LogLevel.INFO)
                assertOutputDoesNotContain("Visibility must be specified in explicit API mode")
                assertOutputDoesNotContain("Return type must be specified in explicit API mode")
            }
        }
    }

    private fun externalAndroidLibraryProject(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        namespace: String,
        configureProject: TestProject.() -> Unit = {},
    ): TestProject = project(
        "empty",
        gradleVersion = gradleVersion,
        buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        buildJdk = jdkVersion.location,
    ) {
        plugins {
            kotlin("multiplatform")
            id("com.android.kotlin.multiplatform.library")
        }
        buildScriptInjection {
            kotlinMultiplatform.apply {
                targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                    target.compileSdk = 34
                    target.namespace = namespace
                    target.withJava()
                }
            }
        }
        configureProject()
    }

}
