/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.cartesianProductOf
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.x
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.runners.model.MultipleFailureException
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class MppPublicationCompatibilityIT : KGPBaseTest() {
    private val gradleVersions = listOf(
        TestVersions.Gradle.MIN_SUPPORTED,
        TestVersions.Gradle.MAX_SUPPORTED,
    )

    private val agpVersions = listOf(
        TestVersions.AGP.MIN_SUPPORTED,
        TestVersions.AGP.MAX_SUPPORTED,
    )

    private val agpSupportedGradleVersions = agpVersions.associateWith { agpVersion ->
        val compatibilityEntry = TestVersions.AgpCompatibilityMatrix.entries.single { it.version == agpVersion }
        compatibilityEntry.minSupportedGradleVersion.version
    }

    private val kgpSupportedGradleVersions = mapOf(
        TestVersions.Kotlin.CURRENT to gradleVersions + agpSupportedGradleVersions.values
    )

    private val kotlinVersions = listOf(
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

    private fun generateScenarios(): Set<Scenario> {
        val projects = cartesianProductOf(gradleVersions, agpVersions, kotlinVersions, projectVariants).map {
            ScenarioProject(
                gradleVersion = it[0] as String,
                agpVersion = it[1] as String,
                kotlinVersion = it[2] as String,
                variant = it[3] as ProjectVariant,
            )
        }
            // Override Gradle version to match AGP version
            .map { if (it.isWithAndroid) it.copy(gradleVersionString = agpSupportedGradleVersions[it.agpVersionString]!!) else it }
            // Don't use newer gradle with old KGP (they may not be entirely compatible)
            .filter { if (it.isKmp) kgpSupportedGradleVersions.get(it.kotlinVersionString)!!.contains(it.gradleVersionString) else true }
            .toSet()

        val scenarios = (projects x projects)
            .map { (consumer, producer) -> Scenario(consumer, producer) }
            // ensure that each scenario we test master KGP i.e. we are not interested in AndroidOnly <-> JavaOnly compatibility
            .filter { scenario -> scenario.consumer.isMasterKmp || scenario.producer.isMasterKmp }
            // ensure that JavaOnly can consume KMP project i.e. it should have JVM part
            .filter { scenario -> if (scenario.consumer.isJavaOnly) scenario.producer.isWithJvm else true }
            .filter { scenario -> if (scenario.consumer.isWithAndroid) scenario.producer.isWithAndroid else true }
            .toSet()
        println("Total scenarios: ${scenarios.size}")
        println("Total unique publications: ${scenarios.map { it.producer }.toSet().size}")
        println("Total unique consumer projects: ${scenarios.map { it.consumer }.toSet().size}")

        for (scenario in scenarios) {
            val consumerId = "${scenario.consumer.packageName}:${scenario.consumer.artifactName}"
            val producerId = "${scenario.producer.packageName}:${scenario.producer.artifactName}"
            println("$consumerId consumes $producerId")
        }

        return scenarios
    }

    @DisplayName("test compatibility between published libraries by kotlin multiplatform, java and android")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED, maxVersion = TestVersions.Gradle.MAX_SUPPORTED)
    @MppGradlePluginTests
    fun testKmpPublication() {
        val scenarios = generateScenarios()
        val producers = scenarios.map { it.producer }.toSet()
        val localRepoDir = workingDir.resolve("mavenRepo")
        publishAllProjects(producers, localRepoDir)
        val testReports: Map<Scenario.Project, Map<String, Path>> = consumeAllProjects(scenarios, localRepoDir)

        val failures = mutableListOf<Throwable>()
        scenarios.map { it.consumer }.toSet().forEach { consumer ->
            val resolvedConfigurationsNames = consumer.resolvedConfigurationsNames
            for (resolvedConfigurationName in resolvedConfigurationsNames) {
                val expectedReportFile = consumer.expectedResolvedConfigurationTestReport(resolvedConfigurationName)
                val actualReport = testReports[consumer]!![resolvedConfigurationName]!!.readText()

                val actualReportSanitized = actualReport
                    .lineSequence()
                    .filterNot { it.contains("stdlib") }
                    .map { it.replace(TestVersions.Kotlin.CURRENT, "SNAPSHOT") }
                    .joinToString("\n")

                val result = kotlin.runCatching {
                    KotlinTestUtils.assertEqualsToFile(expectedReportFile.toFile(), actualReportSanitized)
                }
                failures.addIfNotNull(result.exceptionOrNull())
            }
        }

        if (failures.isNotEmpty()) {
            throw MultipleFailureException(failures)
        }
    }

    private fun publishAllProjects(projects: Collection<Scenario.Project>, localRepoDir: Path) {
        projects.groupBy { it.gradleVersion }.map { (gradleVersion, projects) ->
            val allSubprojects = mutableListOf<String>()
            project(
                projectName = "mppPublicationCompatibility/sampleProjects/base",
                gradleVersion = gradleVersion,
                buildJdk = File(System.getProperty("jdk${JavaVersion.VERSION_17.majorVersion}Home"))
            ) {
                for (project in projects) {
                    val sampleDirectoryName = project.variant.sampleDirectoryName
                    val projectName = project.artifactName
                    allSubprojects += projectName

                    includeOtherProjectAsIncludedBuild(
                        sampleDirectoryName,
                        "mppPublicationCompatibility/sampleProjects",
                        projectName
                    )
                    subProject(projectName).apply {
                        prepareProject(project, localRepoDir)
                        prepareProjectForPublication(project)
                    }
                }

                build(*allSubprojects.map { ":$it:publish" }.toTypedArray(), forceOutput = true)
            }
        }
    }

    private fun consumeAllProjects(scenarios: Collection<Scenario>, localRepoDir: Path): Map<Scenario.Project, Map<String, Path>> {
        val testReports = mutableMapOf<Scenario.Project, Map<String, Path>>()
        val consumers = scenarios.map { it.consumer }.toSet()
        val consumerDependencies = scenarios.groupBy(keySelector = { it.consumer }) { it.producer }
        consumers.groupBy { it.gradleVersion }.map { (gradleVersion, projects) ->
            val allSubprojects = mutableListOf<String>()
            project(
                projectName = "mppPublicationCompatibility/sampleProjects/base",
                gradleVersion = gradleVersion,
                buildJdk = File(System.getProperty("jdk${JavaVersion.VERSION_17.majorVersion}Home"))
            ) {
                for (project in projects) {
                    val sampleDirectoryName = project.variant.sampleDirectoryName
                    val projectName = project.artifactName
                    allSubprojects += projectName

                    includeOtherProjectAsIncludedBuild(
                        sampleDirectoryName,
                        "mppPublicationCompatibility/sampleProjects",
                        projectName
                    )
                    subProject(projectName).apply {
                        prepareProject(project, localRepoDir)
                        prepareProjectForConsumption(project, consumerDependencies[project]!!, localRepoDir)

                        testReports[project] = project.resolvedConfigurationsNames.associateWith { configurationName ->
                            projectPath.resolve("resolvedDependenciesReports").resolve("${configurationName}.txt")
                        }
                    }
                }

                build(*allSubprojects.map { ":$it:resolveDependencies" }.toTypedArray())
            }
        }

        return testReports
    }
}

private fun Scenario.Project.expectedResolvedConfigurationTestReport(configurationName: String): Path {
    return Paths.get(
        "src",
        "test",
        "resources",
        "testProject",
        "mppPublicationCompatibility",
        "expectedData",
        packageName,
        artifactName,
        "$configurationName.txt"
    )
}

private val ProjectVariant.sampleDirectoryName: String
    get() = when (this) {
        ProjectVariant.AndroidOnly -> "androidOnly"
        ProjectVariant.JavaOnly -> "javaOnly"
        is ProjectVariant.Kmp -> "kmp"
    }

private fun GradleProject.prepareProject(scenarioProject: Scenario.Project, localRepoDir: Path) {
    configureLocalRepository(localRepoDir)
    projectPath.enableAndroidSdk()
    settingsGradleKts.replaceText(
        "val kotlin_version: String by settings",
        """val kotlin_version = "${scenarioProject.kotlinVersionString}" """
    )
    settingsGradleKts.replaceText(
        "val android_tools_version: String by settings",
        """val android_tools_version = "${scenarioProject.agpVersionString}" """
    )
}