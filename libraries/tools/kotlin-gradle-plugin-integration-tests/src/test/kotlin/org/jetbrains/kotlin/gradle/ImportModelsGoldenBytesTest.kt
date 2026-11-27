/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.GOLDEN_FILES_ROOT
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.assertFixtureMatchesGolden
import org.jetbrains.kotlin.gradle.testbase.resultBytesFile
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@JvmGradlePluginTests
@MustRunOnChangesInGradle
class ImportModelsGoldenBytesTest : KGPBaseTest() {
    @Test
    fun `every golden has its serialized Result bytes beside it`() {
        val goldens = Files.walk(GOLDEN_FILES_ROOT).filter { it.isRegularFile() && it.toString().endsWith(".txt") }.toList()
        assertTrue(goldens.isNotEmpty(), "No golden files found under $GOLDEN_FILES_ROOT")

        goldens.forEach { golden ->
            val bytesFile = resultBytesFile(golden)
            // The bytes are the fixture for IDEA-side tests that do not run Gradle
            assertTrue(bytesFile.isRegularFile(), "Missing serialized bytes $bytesFile beside the golden file $golden")

            assertFixtureMatchesGolden(golden, bytesFile)
        }
    }

    @Test
    fun `a fixture with the same model type but different contents fails the comparison`() {
        val golden = GOLDEN_FILES_ROOT.resolve("simpleProject/projectInformation.txt")
        val bytesFile = resultBytesFile(golden)
        // The tamper keeps the model type, so only the content comparison can catch it
        val tampered = KotlinImportModelSerialization.modelResult(
            Result.parseFrom(bytesFile.readBytes()).model.unpack(ProjectModel::class.java).toBuilder()
                .clearCompilationUnitIds()
                .build()
        )
        withTempFixture(tampered) { fixtureFile ->
            assertFailsWith<AssertionError> { assertFixtureMatchesGolden(golden, fixtureFile) }
        }
    }

    @Test
    fun `a fixture packing the same model under another type URL prefix fails the comparison`() {
        val golden = GOLDEN_FILES_ROOT.resolve("simpleProject/projectInformation.txt")
        val bytesFile = resultBytesFile(golden)
        val projectModel = Result.parseFrom(bytesFile.readBytes()).model.unpack(ProjectModel::class.java)
        // Any resolves a payload by the type name after the slash, so only the URL's prefix differs here
        val differentlyPacked = Result.newBuilder()
            .apply {
                modelBuilder.typeUrl = "example.com/${ProjectModel.getDescriptor().fullName}"
                modelBuilder.value = projectModel.toByteString()
            }
            .build()
            .toByteArray()
        withTempFixture(differentlyPacked) { fixtureFile ->
            assertFailsWith<AssertionError> { assertFixtureMatchesGolden(golden, fixtureFile) }
        }
    }

    // The comparison takes paths, so a payload built in a test needs a file to live in
    private fun withTempFixture(bytes: ByteArray, assertion: (Path) -> Unit) {
        val bytesFile = Files.createTempFile("importModels", ".bin")
        try {
            bytesFile.writeBytes(bytes)
            assertion(bytesFile)
        } finally {
            Files.deleteIfExists(bytesFile)
        }
    }
}
