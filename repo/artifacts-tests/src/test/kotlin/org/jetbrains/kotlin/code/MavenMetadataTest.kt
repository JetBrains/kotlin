/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.assertTrue
import kotlin.test.fail

class MavenMetadataTest {
    @TestFactory
    fun generateMavenMetadataTests(): Stream<DynamicTest> {
        return findActualArtifacts(".pom", mustContainKotlinVersion = true).map { actual ->
            val expectedPomPath = actual.toExpectedPath()
            DynamicTest.dynamicTest(expectedPomPath.fileName.toString()) {
                if ("${expectedPomPath.parent.fileName}" !in excludedProjects) {
                    if ("${expectedPomPath.parent.fileName}" !in nativeBundles) {
                        val regex = Regex(
                            """(<groupId>org.jetbrains.kotlin\S*</groupId>\s*\S*\s*)<version>$kotlinVersion</version>"""
                        )
                        val actualString = actual.toFile().readText().replace(regex, "\$1<version>ArtifactsTest.version</version>")
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
        val publishedPoms = findActualArtifacts(".pom", mustContainKotlinVersion = true)
            .map { it.toExpectedPath() }
            .filter { "${it.parent.fileName}" !in excludedProjects }.toSet()

        return findExpectedArtifacts(".pom").map { expected ->
            DynamicTest.dynamicTest(expected.fileName.toString()) {
                assertTrue(expected in publishedPoms, "Missing actual pom for expected pom: $expected")
            }
        }.asStream()
    }
}
