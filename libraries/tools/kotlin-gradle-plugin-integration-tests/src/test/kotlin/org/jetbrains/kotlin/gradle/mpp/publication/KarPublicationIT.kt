/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import org.jetbrains.kotlin.gradle.testbase.assertTasksAreNotInTaskGraph
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksFailed
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
    private fun BuildOptions.disableConfigurationCacheAndProjectIsolation(
        enableKarPublication: Boolean = true,
    ) = copy(
        configurationCache = ConfigurationCacheValue.DISABLED,
        isolatedProjects = IsolatedProjectsMode.DISABLED,
        freeArgs = freeArgs + if (enableKarPublication) listOf(ENABLE_KAR_PUBLICATION) else emptyList(),
    )

    @GradleTest
    @MppGradlePluginTests
    fun testLegacyPublicationLayoutIsUsedByDefault(gradleVersion: GradleVersion) {
        val repository = publicationRoot.resolve("legacy-${gradleVersion.version}")
        project(
            projectName = "karPublication/producer",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableConfigurationCacheAndProjectIsolation(enableKarPublication = false),
            localRepoDir = repository,
        ) {
            build("publishAllPublicationsToMavenRepository") {
                assertTasksAreNotInTaskGraph(":packKotlinArchive")
            }
        }

        val publicationDirectory = repository.resolve("org/jetbrains/kotlin/kar/test")
        assertFalse(publicationDirectory.resolve("sample/1.0/sample-1.0.kar.xz").exists())
    }

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

    @GradleTest
    @MppGradlePluginTests
    fun testKarReplacesLegacyTargetPublication(gradleVersion: GradleVersion) {
        val repository = publicationRoot.resolve("compatibility-${gradleVersion.version}")
        val buildOptions = defaultBuildOptions.disableConfigurationCacheAndProjectIsolation()

        project(
            projectName = "karPublication/compatibility/libraryV1",
            gradleVersion = gradleVersion,
            buildOptions = buildOptions.copy(kotlinVersion = KOTLIN_2_4_0),
            localRepoDir = repository,
        ) {
            build("publishAllPublicationsToMavenRepository")
        }

        project(
            projectName = "karPublication/compatibility/libraryV2",
            gradleVersion = gradleVersion,
            buildOptions = buildOptions,
            localRepoDir = repository,
        ) {
            build("publishAllPublicationsToMavenRepository") {
                assertTasksExecuted(":packKotlinArchive")
            }
        }

        val sampleModule = repository.resolve(
            "org/jetbrains/kotlin/kar/test/sample/2.0/sample-2.0.module"
        )
        val linuxX64Capabilities = Json.parseToJsonElement(sampleModule.readText())
            .jsonObject.getValue("variants").jsonArray
            .single { variant -> variant.jsonObject.getValue("name").jsonPrimitive.content == "linuxX64ApiElements-published" }
            .jsonObject.getValue("capabilities").jsonArray
            .map { capability -> capability.jsonObject.getValue("name").jsonPrimitive.content }
            .toSet()
        assertEquals(setOf("sample", "sample-linuxx64"), linuxX64Capabilities)

        project(
            projectName = "karPublication/compatibility/intermediate",
            gradleVersion = gradleVersion,
            buildOptions = buildOptions,
            localRepoDir = repository,
        ) {
            build("publishAllPublicationsToMavenRepository") {
                assertTasksExecuted(":packKotlinArchive")
            }
        }

        val intermediateModule = repository.resolve(
            "org/jetbrains/kotlin/kar/test/intermediate/1.0/intermediate-1.0.module"
        )
        assertContains(intermediateModule.readText(), "sample-linuxx64")

        project(
            projectName = "karPublication/compatibility/app",
            gradleVersion = gradleVersion,
            buildOptions = buildOptions,
            localRepoDir = repository,
        ) {
            build("compileKotlinLinuxX64", "assertLegacyPublicationIsNotSelected") {
                assertTasksExecuted(":compileKotlinLinuxX64")
                assertTasksExecuted(":assertLegacyPublicationIsNotSelected")
            }
        }
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
                assertTasksExecuted(":packKotlinArchive")
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
