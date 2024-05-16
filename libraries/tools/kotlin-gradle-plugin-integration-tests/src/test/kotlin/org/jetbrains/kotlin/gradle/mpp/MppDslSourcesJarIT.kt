/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.appendText
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.test.assertEquals

@MppGradlePluginTests
class MppDslSourcesJarIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testSourceJars(gradleVersion: GradleVersion) {

        val localRepoDir = defaultLocalRepo(gradleVersion)

        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            // TODO KT-67566 once all MppDslPublishedMetadataIT test are updated to new test DSL, update the projects to use `<localRepo>`
            buildGradle.appendText(
                """
                publishing {
                  repositories {
                    maven {
                      name = "LocalRepo"
                      url = uri("${localRepoDir.invariantSeparatorsPathString}")
                    }
                  }
                }
                """.trimIndent()
            )

            build("publish") {
                val repoGroupDir = localRepoDir.resolve("com/example")
                assertDirectoryExists(repoGroupDir)

                val targetArtifactIds = listOf(
                    "sample-lib",
                    "sample-lib-jvm6",
                    "sample-lib-nodejs",
                    "sample-lib-linux64",
                )

                val sourceJarSourceRoots: Map<String, Set<String>> =
                    targetArtifactIds.associateWith { artifactId ->
                        val sourcesJarPath = repoGroupDir.resolve("$artifactId/1.0/$artifactId-1.0-sources.jar")
                        assertFileExists(sourcesJarPath)
                        sourcesJarPath.jarFileRootDirs() - "META-INF"
                    }

                assertEquals(setOf("commonMain", "nativeMain"), sourceJarSourceRoots["sample-lib"])
                assertEquals(setOf("commonMain", "jvm6Main"), sourceJarSourceRoots["sample-lib-jvm6"])
                assertEquals(setOf("commonMain", "nodeJsMain"), sourceJarSourceRoots["sample-lib-nodejs"])
                assertEquals(setOf("commonMain", "nativeMain", "linux64Main"), sourceJarSourceRoots["sample-lib-linux64"])
            }
        }
    }

    companion object {
        /** Names of the root directories within a JAR file. */
        private fun Path.jarFileRootDirs(): Set<String> {
            JarFile(toFile()).use { sourcesJar ->
                return sourcesJar.entries().asSequence().map { it.name.substringBefore("/") }.toSet()
            }
        }
    }
}
