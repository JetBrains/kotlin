/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertTrue

@DisplayName("KAR publication consumption from Maven")
class KarMavenPublicationIT : KGPBaseTest() {
    companion object {
        private const val MAVEN_VERSION = "3.9.12"

        @JvmStatic
        @TempDir
        lateinit var publicationRoot: Path
    }

    @GradleTest
    @MppGradlePluginTests
    fun testJavaMavenConsumesJvmTargetPublication(gradleVersion: GradleVersion) {
        testMavenConsumption(gradleVersion, "mavenJavaConsumer")
    }

    @GradleTest
    @MppGradlePluginTests
    fun testKotlinMavenConsumesJvmTargetPublication(gradleVersion: GradleVersion) {
        testMavenConsumption(gradleVersion, "mavenKotlinConsumer", KOTLIN_2_4_20_BETA1)
    }

    private fun testMavenConsumption(
        gradleVersion: GradleVersion,
        consumerName: String,
        kotlinVersion: String? = null,
    ) {
        val repository = publishKarOnce(gradleVersion, publicationRoot)
        project(
            projectName = "karPublication/$consumerName",
            gradleVersion = gradleVersion,
            localRepoDir = repository,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = ConfigurationCacheValue.DISABLED,
                isolatedProjects = IsolatedProjectsMode.DISABLED,
            ),
        ) {
            val pom = projectPath.resolve("pom.xml")
            pom.replaceText("LOCAL_REPOSITORY_URL", repository.toUri().toString())
            pom.replaceText("KOTLIN_BUILD_REPOSITORY_URL", kotlinBuildRepository().toUri().toString())
            kotlinVersion?.let { pom.replaceText("KOTLIN_VERSION", it) }

            configureMavenWrapper(projectPath)
            val mavenUserHome = projectPath.resolve("maven-user-home").createDirectories()
            val mavenLocalRepository = projectPath.resolve("maven-local-repository").createDirectories()
            val command = mavenCommand(projectPath) + listOf(
                "--batch-mode",
                "--no-transfer-progress",
                "--errors",
                "-Dmaven.repo.local=${mavenLocalRepository.absolutePathString()}",
                "compile",
            )

            runProcess(
                command,
                projectPath.toFile(),
                environmentVariables = mapOf("MAVEN_USER_HOME" to mavenUserHome.absolutePathString()),
            ).assertProcessRunResult {
                assertTrue(isSuccessful, "Maven compilation failed")
            }
        }
    }

    // TODO: replace with proper maven wrapper utilities, and maybe move the whole test-suite to some other place.
    private fun configureMavenWrapper(projectDirectory: Path) {
        val wrapperResources = Path("../kotlin-maven-plugin-test/src/test/resources/maven-wrapper")
        assertTrue(wrapperResources.exists(), "Maven wrapper resources are missing at $wrapperResources")

        val unixScript = wrapperResources.resolve("mvnw").copyTo(projectDirectory.resolve("mvnw"))
        wrapperResources.resolve("mvnw.cmd").copyTo(projectDirectory.resolve("mvnw.cmd"))
        projectDirectory.resolve(".mvn/wrapper").createDirectories()
            .resolve("maven-wrapper.properties")
            .writeText(
                """
                wrapperVersion=3.3.4
                distributionType=only-script
                distributionUrl=https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip
                """.trimIndent() + "\n"
            )

        try {
            Files.setPosixFilePermissions(
                unixScript,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE,
                )
            )
        } catch (_: UnsupportedOperationException) {
            // POSIX permissions are unavailable on Windows.
        }
    }

    private fun mavenCommand(projectDirectory: Path): List<String> =
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            listOf("cmd", "/c", projectDirectory.resolve("mvnw.cmd").absolutePathString())
        } else {
            listOf(projectDirectory.resolve("mvnw").absolutePathString())
        }

    private fun kotlinBuildRepository(): Path = System.getProperty("maven.repo.local")
        ?.let(::Path)
        ?: Path(System.getProperty("user.home"), ".m2", "repository")
}
