/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.isTeamCityBuild
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.asStream
import kotlin.test.assertTrue
import kotlin.test.fail

class ArtifactsTest {

    private val kotlinVersion = System.getProperty("kotlin.version")
    private val mavenLocal = System.getProperty("maven.repo.local")
    private val localRepoPath = Paths.get(mavenLocal, "org/jetbrains/kotlin")
    private val expectedRepoPath = Paths.get("repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin")

    /**
     * Kotlin native bundles are present in TC artifacts but should not be checked until kotlin native enabled project-wide
     */
    private val nativeBundles = setOf(
        "kotlin-native",
        "kotlin-native-compiler-embeddable",
        "kotlin-native-prebuilt",
    )

    private val excludedProjects = setOf(
        "android-test-fixes",
        "annotation-processor-example",
        "fus-statistics-gradle-plugin",
        "gradle-warnings-detector",
        "kotlin-compiler-args-properties",
        "kotlin-gradle-plugin-tcs-android",
        "kotlin-gradle-subplugin-example",
        "kotlin-java-example",
        "kotlin-maven-plugin-test",
        "org.jetbrains.kotlin.fus-statistics-gradle-plugin.gradle.plugin",
        "org.jetbrains.kotlin.gradle-subplugin-example.gradle.plugin",
        "org.jetbrains.kotlin.test.fixes.android.gradle.plugin",
        "org.jetbrains.kotlin.test.gradle-warnings-detector.gradle.plugin",
        "org.jetbrains.kotlin.test.kotlin-compiler-args-properties.gradle.plugin",
    )

    @TestFactory
    fun generateArtifactTests(): Stream<DynamicTest> {
        return findActualPoms().map { actual ->
            val expectedPomPath = actual.toExpectedPath()
            DynamicTest.dynamicTest(expectedPomPath.fileName.toString()) {
                if ("${expectedPomPath.parent.fileName}" !in excludedProjects) {
                    if ("${expectedPomPath.parent.fileName}" !in nativeBundles) {
                        val actualString = actual.toFile().readText().replace(kotlinVersion, "ArtifactsTest.version")
                        assertEqualsToFile(expectedPomPath, actualString)
                    }
                } else {
                    if (isTeamCityBuild) fail("Excluded project in actual artifacts: $actual")
                }
            }
        }.asStream()
    }

    @TestFactory
    fun allExpectedPomsPresentInActual(): Stream<DynamicTest> {
        val publishedPoms = findActualPoms()
            .map { it.toExpectedPath() }
            .filter { "${it.parent.fileName}" !in excludedProjects }.toSet()

        return findExpectedPoms().map { expected ->
            DynamicTest.dynamicTest(expected.fileName.toString()) {
                assertTrue(expected in publishedPoms, "Missing actual pom for expected pom: $expected")
            }
        }.asStream()
    }

    private fun findActualPoms() = Files.find(
        localRepoPath,
        Integer.MAX_VALUE,
        { path: Path, fileAttributes: BasicFileAttributes ->
            fileAttributes.isRegularFile
                    && "${path.fileName}".endsWith(".pom", ignoreCase = true)
                    && path.contains(Paths.get(kotlinVersion))
        }).asSequence()

    private fun findExpectedPoms() = Files.find(
        expectedRepoPath,
        Integer.MAX_VALUE,
        { path: Path, fileAttributes: BasicFileAttributes ->
            fileAttributes.isRegularFile
                    && "${path.fileName}".endsWith(".pom", ignoreCase = true)
        }).asSequence()

    /**
     * convert:
     * ${mavenLocal}/org/jetbrains/kotlin/artifact/version/artifact-version.pom
     * to:
     * ${expectedRepository}/org/jetbrains/kotlin/artifact/artifact.pom
     */
    private fun Path.toExpectedPath(): Path {
        val artifactDirPath = localRepoPath.relativize(this).parent.parent
        val expectedFileName = "${artifactDirPath.fileName}.pom"
        return expectedRepoPath.resolve(artifactDirPath.resolve(expectedFileName))
    }
}