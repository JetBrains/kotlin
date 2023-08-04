/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.isTeamCityBuild
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class ArtifactsTest {

    private val kotlinVersion = System.getProperty("kotlin.version")
    private val mavenLocal = System.getProperty("maven.repo.local")
    private val localRepoPath = Paths.get(mavenLocal, "org/jetbrains/kotlin")
    private val expectedRepoPath = Paths.get("repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin")

    /**
     * Experimental projects that might be published locally but are not epxected to be published by default.
     * They still might be added to TeamCity maven.zip final artifacts with an additional code.
     */
    private val experimentalProjects = setOf(
        "compiler-hosted",
        "compiler-daemon",
        "compiler",
    )

    private val excludedProjects = setOf(
        "annotation-processor-example",
        "android-test-fixes",
        "org.jetbrains.kotlin.gradle-subplugin-example.gradle.plugin",
        "gradle-warnings-detector",
        "kotlin-compiler-args-properties",
        "kotlin-gradle-plugin-kpm-android",
        "kotlin-gradle-plugin-tcs-android",
        "kotlin-gradle-subplugin-example",
        "kotlin-java-example",
        "kotlin-maven-plugin-test",
        "org.jetbrains.kotlin.test.fixes.android.gradle.plugin",
        "org.jetbrains.kotlin.test.gradle-warnings-detector.gradle.plugin",
        "org.jetbrains.kotlin.test.kotlin-compiler-args-properties.gradle.plugin",
    )

    @Test
    fun verifyArtifactFiles() {
        val visitedPoms = mutableSetOf<Path>()
        val actualPoms = Files.find(
            localRepoPath,
            Integer.MAX_VALUE,
            { path: Path, fileAttributes: BasicFileAttributes ->
                fileAttributes.isRegularFile
                        && "${path.fileName}".endsWith(".pom", ignoreCase = true)
                        && path.contains(Paths.get(kotlinVersion))
            })
        actualPoms.forEach { actual ->
            val expectedPomPath = actual.toExpectedPath()
            val parentDirName = expectedPomPath.parent.fileName.toString()

            if (parentDirName !in experimentalProjects) {
                if (parentDirName !in excludedProjects) {
                    val actualString = actual.toFile().readText().replace(kotlinVersion, "ArtifactsTest.version")
                    assertEqualsToFile(expectedPomPath, actualString)
                    visitedPoms.add(expectedPomPath)
                } else {
                    if (isTeamCityBuild) fail("Excluded project in actual artifacts: $actual")
                }
            }
        }

        val expectedPoms = Files.find(
            expectedRepoPath,
            Integer.MAX_VALUE,
            { path: Path, fileAttributes: BasicFileAttributes ->
                fileAttributes.isRegularFile
                        && "${path.fileName}".endsWith(".pom", ignoreCase = true)
            })
        expectedPoms.forEach {
            assertTrue(it in visitedPoms, "Missing actual pom for expected pom: $it")
        }
    }

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