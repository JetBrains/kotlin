/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.tests.integration.utils.IntegrationTestFiles
import org.jetbrains.kotlin.konan.test.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.test.fail

/**
 * Generated test data is then verified by [ObjCExportIntegrationTest]
 */
class GenerateObjCExportIntegrationTestData(private val generator: HeaderGenerator) {

    @Test
    fun `generate headers`() {
        generateAndStoreObjCHeader(testLibraryKotlinxDatetime.name, listOf(testLibraryKotlinxDatetime))
        generateAndStoreObjCHeader(testLibraryKotlinxCoroutines.name, listOf(testLibraryKotlinxCoroutines))
        generateAndStoreObjCHeader(testLibraryAtomicFu.name, listOf(testLibraryAtomicFu))
        generateAndStoreObjCHeader(testLibraryKotlinxSerializationCore.name, listOf(testLibraryKotlinxSerializationCore))
        generateAndStoreObjCHeader(testLibraryKotlinxSerializationJson.name, listOf(testLibraryKotlinxSerializationJson))

        generateAndStoreObjCHeader(
            "combined",
            listOf(
                testLibraryKotlinxDatetime,
                testLibraryKotlinxCoroutines,
                testLibraryAtomicFu,
                testLibraryKotlinxSerializationCore,
                testLibraryKotlinxSerializationJson
            )
        )
    }

    private fun generateAndStoreObjCHeader(name: String, libraries: List<Path>) {
        val header = generateObjCHeader(libraries)
        IntegrationTestFiles.storeHeader(name, header)
    }

    private fun generateObjCHeader(libraries: List<Path>): String {
        return generateHeader(
            root = IntegrationTestFiles.integrationDir,
            configuration = HeaderGenerator.Configuration(
                dependencies = libraries,
                exportedDependencies = libraries.toSet(),
                frameworkName = "",
                withObjCBaseDeclarationStubs = false
            )
        )
    }

    private fun generateHeader(root: File, configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration()): String {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        return generator.generateHeaders(root, configuration).toString()
    }
}