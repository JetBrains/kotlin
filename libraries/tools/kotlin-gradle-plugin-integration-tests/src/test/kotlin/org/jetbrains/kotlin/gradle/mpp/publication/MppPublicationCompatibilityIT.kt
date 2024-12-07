/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.cartesianProductOf
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.jetbrains.kotlin.gradle.util.x
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

@ExtendWith(GradleParameterResolver::class)
class MppPublicationCompatibilityIT : KGPBaseTest() {
    companion object {
        private val agpVersions = listOf(
            TestVersions.AGP.MIN_SUPPORTED,
            TestVersions.AGP.MAX_SUPPORTED,
        )

        private val kotlinVersions = listOf(
            TestVersions.Kotlin.STABLE_RELEASE,
            TestVersions.Kotlin.CURRENT
        )

        private val projectVariants = with(ProjectVariant) {
            listOf(
                native + jvm,
                native + android,
                native + jvm + android,
                javaOnly,
                androidOnly,
            )
        }

        private fun generateScenarios(gradleVersions: List<String>): Set<Scenario> {
            val projects = cartesianProductOf(gradleVersions, agpVersions, kotlinVersions, projectVariants).map {
                ScenarioProject(
                    gradleVersion = it[0] as String,
                    agpVersion = it[1] as String,
                    kotlinVersion = it[2] as String,
                    variant = it[3] as ProjectVariant,
                )
            }
                .filter(Scenario.Project::hasValidVersionCombo)
                .toSet()

            val scenarios = (projects x projects)
                .map { (consumer, producer) -> Scenario(consumer, producer) }
                .filter(Scenario::hasMasterKmp) // we are not interested in AndroidOnly <-> JavaOnly compatibility
                .filter(Scenario::isConsumable)
                .toSet()

            println("Total scenarios: ${scenarios.size}")
            println("Total unique publications: ${scenarios.map { it.producer }.toSet().size}")
            println("Total unique consumer projects: ${scenarios.map { it.consumer }.toSet().size}")

            return scenarios
        }

        private val expectedDataPath = Paths.get(
            "src",
            "test",
            "resources",
            "testProject",
            "mppPublicationCompatibility",
            "expectedData",
        )

        private val Scenario.expectedScenarioDataDir
            get() = expectedDataPath.resolve("consumer_" + consumer.id).resolve("producer_" + producer.id)

        private fun Scenario.expectedResolvedConfigurationTestReport(configurationName: String): Path = expectedScenarioDataDir
            .resolve("$configurationName.txt")

        private val ProjectVariant.sampleDirectoryName: String
            get() = when (this) {
                ProjectVariant.AndroidOnly -> "androidOnly"
                ProjectVariant.JavaOnly -> "javaOnly"
                is ProjectVariant.Kmp -> "kmp"
            }

        @JvmStatic
        fun scenarios(specificGradleVersion: GradleVersion?): Iterable<Scenario> {
            val supportedGradleVersions = listOf(
                TestVersions.Gradle.MIN_SUPPORTED,
                TestVersions.Gradle.MAX_SUPPORTED,
            )

            if (specificGradleVersion != null) {
                if (specificGradleVersion.version !in supportedGradleVersions) return emptyList()
                println("Generate scenarios for $specificGradleVersion Gradle version")
                return generateScenarios(listOf(specificGradleVersion.version))
            } else {
                println("Generate scenarios for $supportedGradleVersions Gradle versions")
                return generateScenarios(supportedGradleVersions)
            }
        }

        @JvmStatic
        fun rerunScenariosForDebugging(specificGradleVersion: GradleVersion?): Iterable<Scenario> {
            val rerunIndex = 2
            return listOf(scenarios(specificGradleVersion).toList()[rerunIndex])
        }

        @JvmStatic
        @TempDir
        lateinit var localRepoDir: Path
    }

    override val defaultBuildOptions: BuildOptions
        // resolveDependencies is the utility test task, configuration cache support is not required there
        get() = super.defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED)

    // ATTENTION! Test data could be regenerated by removing subdirectories in "expectedData" directory in the project dir
    @DisplayName("test compatibility between published libraries by kotlin multiplatform, java and android")
    @TestMetadata("mppPublicationCompatibility")
    @MppGradlePluginTests
    @ParameterizedTest
    @Suppress("JUnitMalformedDeclaration") // FIXME: IDEA-320187
    @MethodSource("scenarios") /** For debugging use [rerunScenariosForDebugging] */
    fun testKmpPublication(scenario: Scenario) {
        scenario.producer.publish(localRepoDir)
        scenario.testConsumption(localRepoDir)
    }

    @TestMetadata("mppPublicationCompatibility")
    @MppGradlePluginTests
    @Test
    fun checkThereIsNoUnusedTestData() {
        val autoCleanUp = false // set it to true to automatically clean up unused test data
        if (isTeamCityRun && autoCleanUp) fail { "Auto cleanup can't be used during TeamCity run" }

        val existingDataDirs = expectedDataPath.walk().map { it.parent.relativeTo(expectedDataPath) }.toMutableSet()
        val expectedDataDirs = scenarios(null).map { it.expectedScenarioDataDir.relativeTo(expectedDataPath) }.toSet()

        val unexpectedDataDirs = existingDataDirs - expectedDataDirs
        if (unexpectedDataDirs.isEmpty()) return

        if (autoCleanUp) unexpectedDataDirs.forEach { expectedDataPath.resolve(it).deleteRecursively() }

        fail {
            val unexpectedDataDirsString = unexpectedDataDirs.joinToString("\n") { "   $it" }
            "Following data files are registered in $expectedDataPath but aren't used by test ${this::class}:\n" +
                    unexpectedDataDirsString + "\nPlease remove them or update test."
        }
    }

    private fun Scenario.Project.publish(repoDir: Path) {
        // check if already published
        if (repoDir.resolve(packageName.replace(".", "/")).resolve(artifactName).toFile().exists()) return

        val sampleDirectoryName = variant.sampleDirectoryName
        val scenarioProject = this
        project(
            projectName = "mppPublicationCompatibility/sampleProjects/$sampleDirectoryName",
            gradleVersion = gradleVersion,
            localRepoDir = repoDir,
            buildJdk = File(System.getProperty("jdk${JavaVersion.VERSION_17.majorVersion}Home"))
        ) {
            prepareProjectForPublication(scenarioProject)
            val buildOptions = if (hasAndroid) {
                val androidVersion = scenarioProject.agpVersionString!!
                defaultBuildOptions.copy(androidVersion = androidVersion)
            } else {
                defaultBuildOptions
            }
            build("publish", buildOptions = buildOptions)
        }
    }

    private fun Scenario.testConsumption(repoDir: Path) {
        val consumerDirectory = consumer.variant.sampleDirectoryName

        project(
            projectName = "mppPublicationCompatibility/sampleProjects/$consumerDirectory",
            gradleVersion = consumer.gradleVersion,
            localRepoDir = repoDir,
            buildJdk = File(System.getProperty("jdk${JavaVersion.VERSION_17.majorVersion}Home"))
        ) {
            prepareConsumerProject(consumer, listOf(producer), repoDir)
            val buildOptions = if (consumer.hasAndroid) {
                val androidVersion = consumer.agpVersionString!!
                defaultBuildOptions.copy(androidVersion = androidVersion)
            } else {
                defaultBuildOptions
            }

            build("resolveDependencies", buildOptions = buildOptions)

            fun assertResolvedDependencies(configurationName: String) {
                val actualReport = projectPath.resolve("resolvedDependenciesReports")
                    .resolve("${configurationName}.txt")
                    .readText()

                val expectedReportFile = expectedResolvedConfigurationTestReport(configurationName)
                val actualReportSanitized = actualReport
                    .lineSequence()
                    .filterNot { it.contains("stdlib") }
                    .map { it.replace(TestVersions.Kotlin.CURRENT, "SNAPSHOT") }
                    .joinToString("\n")

                assertEqualsToFile(expectedReportFile.toFile(), actualReportSanitized, withTrailingEOF = false)
            }
            assertAll(consumer.resolvedConfigurationsNames.map { configurationName -> { assertResolvedDependencies(configurationName) } })
        }
    }
}