/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.cartesianProductOf
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.jetbrains.kotlin.gradle.util.x
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * All scenarios sharing the same consumer project: they are checked by a single Gradle build.
 */
data class ConsumerGroup(
    val consumer: Scenario.Project,
    val producers: List<Scenario.Project>,
) {
    override fun toString(): String = consumer.id + " consumes " + producers.size + " producers: " +
            producers.joinToString { it.id }
}

@ExtendWith(GradleParameterResolver::class)
class MppPublicationCompatibilityIT : KGPBaseTest() {
    companion object {
        private val agpVersions = listOf(
            TestVersions.AGP.MIN_SUPPORTED,
            // AGP 9+ rejects the KMP+`com.android.library` and standalone `kotlin("android")` combinations
            // used by the sample projects under `mppPublicationCompatibility/sampleProjects/`.
            // Cap at the last AGP 8 release until the fixtures migrate to `com.android.kotlin.multiplatform.library`.
            TestVersions.AGP.AGP_813,
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
                .map { [consumer, producer] -> Scenario(consumer, producer) }
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

        /**
         * AGP declares the same dependency edge twice in the debug variant compile classpath, so resolving it reports
         * the very same unresolved dependency twice, while resolving it in an isolated configuration reports it once.
         * The multiplicity of identical "ERROR:" lines carries no information, so it is ignored on both sides of the
         * comparison, which keeps the checked-in test data unchanged.
         */
        private fun String.collapseRepeatedErrors(): String {
            val lines = lines()
            return lines
                .filterIndexed { index, line -> !(line.startsWith("ERROR: ") && index > 0 && lines[index - 1] == line) }
                .joinToString("\n")
        }

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
        fun consumerGroups(specificGradleVersion: GradleVersion?): Iterable<ConsumerGroup> = scenarios(specificGradleVersion)
            .groupBy { it.consumer }
            .map { [consumer, consumerScenarios] ->
                ConsumerGroup(consumer, consumerScenarios.map { it.producer }.sortedBy { it.id })
            }

        /** Runs a single (consumer, producer) pair, which is convenient for debugging one failing scenario. */
        @JvmStatic
        fun rerunSingleScenarioForDebugging(specificGradleVersion: GradleVersion?): Iterable<ConsumerGroup> {
            val rerunIndex = 2
            val scenario = scenarios(specificGradleVersion).toList()[rerunIndex]
            return listOf(ConsumerGroup(scenario.consumer, listOf(scenario.producer)))
        }

        @JvmStatic
        @TempDir
        lateinit var localRepoDir: Path
    }

    // ATTENTION! Test data could be regenerated by removing subdirectories in "expectedData" directory in the project dir
    @DisplayName("test compatibility between published libraries by kotlin multiplatform, java and android")
    @TestMetadata("mppPublicationCompatibility")
    @MppGradlePluginTests
    @ParameterizedTest
    @Suppress("JUnitMalformedDeclaration") // FIXME: IDEA-320187
    @MethodSource("consumerGroups") /** For debugging use [rerunSingleScenarioForDebugging] */
    fun testKmpPublication(consumerGroup: ConsumerGroup) {
        consumerGroup.producers.forEach { it.publish(localRepoDir) }
        consumerGroup.testConsumption(localRepoDir)
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
            buildJdk = jdk17Info.javaHome
        ) {
            prepareProjectForPublication(scenarioProject)
            val buildOptions = if (hasAndroid) {
                val androidVersion = scenarioProject.agpVersionString!!
                defaultBuildOptions.copy(androidVersion = androidVersion)
            } else {
                defaultBuildOptions
            }
            // WarningMode.None because of AGP issue: https://issuetracker.google.com/399393875
            build("publish", buildOptions = buildOptions.copy(warningMode = WarningMode.None))
        }
    }

    private fun ConsumerGroup.testConsumption(repoDir: Path) {
        val consumerDirectory = consumer.variant.sampleDirectoryName

        project(
            projectName = "mppPublicationCompatibility/sampleProjects/$consumerDirectory",
            gradleVersion = consumer.gradleVersion,
            localRepoDir = repoDir,
            buildJdk = jdk17Info.javaHome
        ) {
            prepareConsumerProject(consumer, producers, repoDir)
            val buildOptions = if (consumer.hasAndroid) {
                val androidVersion = consumer.agpVersionString!!
                defaultBuildOptions.copy(androidVersion = androidVersion)
            } else {
                defaultBuildOptions
            }

            // WarningMode.None because of AGP issue: https://issuetracker.google.com/399393875
            build("resolveDependencies", buildOptions = buildOptions.copy(warningMode = WarningMode.None))

            fun assertResolvedDependencies(scenario: Scenario, configurationName: String) {
                val actualReportFile = projectPath.resolve("resolvedDependenciesReports")
                    .resolve(scenario.producer.id)
                    .resolve("${configurationName}.txt")
                if (!actualReportFile.exists()) fail {
                    "No resolution report was produced for \"$scenario\", configuration \"$configurationName\": " +
                            "$actualReportFile doesn't exist"
                }

                val expectedReportFile = scenario.expectedResolvedConfigurationTestReport(configurationName)
                val actualReportSanitized = actualReportFile.readText()
                    .lineSequence()
                    .filterNot { it.contains("stdlib") }
                    .map { it.replace(TestVersions.Kotlin.CURRENT, "SNAPSHOT") }
                    .joinToString("\n")

                assertEqualsToFile(expectedReportFile.toFile(), actualReportSanitized) {
                    (if (it.endsWith("\n")) it.dropLast(1) else it).collapseRepeatedErrors()
                }
            }

            assertAll(
                producers.flatMap { producer ->
                    val scenario = Scenario(consumer, producer)
                    consumer.resolvedConfigurationsNames.map { configurationName ->
                        { assertResolvedDependencies(scenario, configurationName) }
                    }
                }
            )
        }
    }
}
