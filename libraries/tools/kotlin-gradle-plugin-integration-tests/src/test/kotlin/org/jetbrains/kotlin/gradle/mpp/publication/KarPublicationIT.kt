/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.AndroidTestVersions
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleAndroidTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestExtraStringArguments
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.JdkVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksFailed
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("KAR publication")
class KarPublicationIT : KGPBaseTest() {
    companion object {
        private const val CURRENT = "current"

        @JvmStatic
        @TempDir
        lateinit var publicationRoot: Path
    }

    /**
     * We can't use project isolation and configuration cache, as they are incompatible with JS publications
     */
    private fun BuildOptions.disableConfigurationCacheAndProjectIsolation() = copy(
        configurationCache = ConfigurationCacheValue.DISABLED,
        isolatedProjects = IsolatedProjectsMode.DISABLED,
    )

    @GradleTest
    @MppGradlePluginTests
    fun testCurrentKgpConsumesKar(gradleVersion: GradleVersion) {
        testKmpConsumption(
            gradleVersion,
            TestVersions.Kotlin.CURRENT,
            "compileCommonMainKotlinMetadata",
            "compileKotlinJvm",
            "compileKotlinJs",
            "compileKotlinLinuxX64",
        )
    }

    @GradleAndroidTest
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_9_4,
        maxVersion = TestVersions.Gradle.G_9_5,
    )
    @AndroidTestVersions(
        minVersion = TestVersions.AGP.AGP_92,
        maxVersion = TestVersions.AGP.AGP_92,
    )
    @MppGradlePluginTests
    fun testCurrentKgpConsumesAndroidPublication(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdk: JdkVersions.ProvidedJdk,
    ) {
        val repository = publicationRoot.resolve("android-${gradleVersion.version}")
        val buildOptions = defaultBuildOptions.copy(
            androidVersion = androidVersion,
            enableLegacyAgpDsl = false,
        ).disableConfigurationCacheAndProjectIsolation()

        project(
            projectName = "karPublication/androidProducer",
            gradleVersion = gradleVersion,
            buildOptions = buildOptions,
            buildJdk = jdk.location,
            localRepoDir = repository,
        ) {
            build("publishAllPublicationsToMavenRepository") {
                assertTasksExecuted(":packKar")
            }
        }

        project(
            projectName = "karPublication/androidConsumer",
            gradleVersion = gradleVersion,
            buildOptions = buildOptions,
            buildJdk = jdk.location,
            localRepoDir = repository,
        ) {
            build("compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
            }
        }
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlin2420Beta1JvmConsumptionSucceeds(gradleVersion: GradleVersion) {
        testKmpConsumption(gradleVersion, KOTLIN_2_4_20_BETA1, "compileKotlinJvm")
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlin2420Beta1JsConsumptionFails(gradleVersion: GradleVersion) {
        testKmpConsumptionFails(gradleVersion, KOTLIN_2_4_20_BETA1, "compileKotlinJs")
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlin2420Beta1NativeConsumptionFails(gradleVersion: GradleVersion) {
        testKmpConsumptionFails(gradleVersion, KOTLIN_2_4_20_BETA1, "compileKotlinLinuxX64")
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlin240JvmConsumptionSucceeds(gradleVersion: GradleVersion) {
        testKmpConsumption(gradleVersion, KOTLIN_2_4_0, "compileKotlinJvm")
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlin240JsConsumptionFails(gradleVersion: GradleVersion) {
        testKmpConsumptionFails(gradleVersion, KOTLIN_2_4_0, "compileKotlinJs")
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlin240NativeConsumptionFails(gradleVersion: GradleVersion) {
        testKmpConsumptionFails(gradleVersion, KOTLIN_2_4_0, "compileKotlinLinuxX64")
    }

    @GradleTest
    @GradleTestExtraStringArguments(KOTLIN_2_4_0, KOTLIN_2_4_20_BETA1, CURRENT)
    @MppGradlePluginTests
    fun testKotlinJvmConsumesTargetPublication(gradleVersion: GradleVersion, kotlinVersion: String) {
        val repository = publishKarOnce(gradleVersion, publicationRoot)
        val resolvedKotlinVersion = if (kotlinVersion == CURRENT) TestVersions.Kotlin.CURRENT else kotlinVersion
        project(
            projectName = "karPublication/kotlinJvmConsumer",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kotlinVersion = resolvedKotlinVersion,
            ).disableConfigurationCacheAndProjectIsolation(),
            localRepoDir = repository,
            projectPathAdditionalSuffix = resolvedKotlinVersion,
        ) {
            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")
            }
        }
    }

    @GradleTest
    @MppGradlePluginTests
    fun testJavaConsumesTargetPublication(gradleVersion: GradleVersion) {
        val repository = publishKarOnce(gradleVersion, publicationRoot)
        project(
            projectName = "karPublication/javaConsumer",
            gradleVersion = gradleVersion,
            localRepoDir = repository,
            buildOptions = defaultBuildOptions.disableConfigurationCacheAndProjectIsolation(),
        ) {
            build("compileJava") {
                assertTasksExecuted(":compileJava")
            }
        }
    }

    private fun testKmpConsumption(
        gradleVersion: GradleVersion,
        kotlinVersion: String,
        vararg compilationTasks: String,
    ) {
        val repository = publishKarOnce(gradleVersion, publicationRoot)
        project(
            projectName = "karPublication/kmpConsumer",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kotlinVersion = kotlinVersion,
            ).disableConfigurationCacheAndProjectIsolation(),
            localRepoDir = repository,
        ) {
            build(*compilationTasks) {
                assertTasksExecuted(compilationTasks.map { ":$it" })
            }
        }
    }

    private fun testKmpConsumptionFails(
        gradleVersion: GradleVersion,
        kotlinVersion: String,
        compilationTask: String,
    ) {
        val repository = publishKarOnce(gradleVersion, publicationRoot)
        project(
            projectName = "karPublication/kmpConsumer",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kotlinVersion = kotlinVersion,
            ).disableConfigurationCacheAndProjectIsolation(),
            localRepoDir = repository,
        ) {
            buildAndFail(compilationTask) {
                assertTasksFailed(":$compilationTask")
            }
        }
    }
}
