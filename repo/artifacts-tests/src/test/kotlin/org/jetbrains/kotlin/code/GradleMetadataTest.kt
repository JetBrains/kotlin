/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.isTeamCityBuild
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.stream.Stream
import kotlin.io.path.absolute
import kotlin.io.path.pathString
import kotlin.streams.asStream
import kotlin.test.assertTrue
import kotlin.test.fail

class GradleMetadataTest {
    @TestFactory
    fun generateGradleMetadataTests(): Stream<DynamicTest> {
        return findActualArtifacts(".module").map { actual ->
            val expectedGradleMetadataPath = actual.toExpectedPath().absolute()
            DynamicTest.dynamicTest(expectedGradleMetadataPath.fileName.toString()) {
                if ("${expectedGradleMetadataPath.parent.fileName}" !in excludedProjects) {
                    if ("${expectedGradleMetadataPath.parent.fileName}" !in nativeBundles) {
                        val actualString = actual.toFile().readText()
                        val expectedWithVersion = expectedGradleMetadataPath.toFile().readText().replace("ArtifactsTest.version", kotlinVersion)
                        val expectedObject = Json.decodeFromString<GradleMetadata>(expectedWithVersion)
                        expectedObject.sortListsRecursively()
                        val tempExpectedFile = File(expectedGradleMetadataPath.pathString + ".tmp").also { it.createNewFile() }
                        tempExpectedFile.writeText(Json.encodeToString(expectedObject))
                        val actualObject = Json.decodeFromString<GradleMetadata>(actualString)
                        actualObject.removeFilesFingerprint()
                        actualObject.sortListsRecursively()
                        assertEqualsToFile(tempExpectedFile, actualObject.toString())
                    }
                } else {
                    if (isTeamCityBuild) fail("Excluded project in actual artifacts: $actual")
                }
            }
        }.asStream()
    }

    @TestFactory
    fun allExpectedGradleMetadataPresentInActual(): Stream<DynamicTest> {
        val publishedGradleMetadata = findActualArtifacts(".module")
            .map { it.toExpectedPath() }
            .filter { "${it.parent.fileName}" !in excludedProjects }.toSet()

        return findExpectedArtifacts(".module").map { expected ->
            DynamicTest.dynamicTest(expected.fileName.toString()) {
                assertTrue(expected in publishedGradleMetadata, "Missing actual gradle metadata for expected gradle metadata: $expected")
            }
        }.asStream()
    }
}