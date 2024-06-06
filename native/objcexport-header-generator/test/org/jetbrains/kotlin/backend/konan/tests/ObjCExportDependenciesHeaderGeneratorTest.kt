/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.fail

/**
 * Currently analysis api doesn't fails until full support of stable order and other parts
 *
 * Test intended to verify how dependencies are translated, which included
 * - stable orders
 * - stubs orders
 * - depth of traversing (some types must be skipped
 */
class ObjCExportDependenciesHeaderGeneratorTest(
    private val generator: HeaderGenerator,
) {

    /**
     * - Missing implementation of mangling
     */
    @Test
    @TodoAnalysisApi
    fun `test - stringBuilder`() {
        doTest(dependenciesDir.resolve("stringBuilder"))
    }

    @Test
    fun `test - iterator`() {
        doTest(dependenciesDir.resolve("iterator"))
    }

    @Test
    fun `test - array`() {
        doTest(dependenciesDir.resolve("array"))
    }

    @Test
    fun `test - arrayList`() {
        doTest(dependenciesDir.resolve("arrayList"))
    }

    @Test
    fun `test - implementIterator`() {
        doTest(dependenciesDir.resolve("implementIterator"))
    }


    /**
     * See KT-68478
     */
    @Test
    @TodoAnalysisApi
    fun `test - kotlinxSerialization`() {
        doTest(
            dependenciesDir.resolve("kotlinxSerialization"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationJson),
                exportedDependencies = setOf(testLibraryKotlinxSerializationJson)
            )
        )
    }

    /**
     * Depends on unimplemented AA deprecation message: KT-67823
     */
    @Test
    @TodoAnalysisApi
    fun `test - serializersModule`() {
        doTest(
            dependenciesDir.resolve("serializersModule"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationCore)
            )
        )
    }

    /**
     * See KT-68479
     */
    @Test
    @TodoAnalysisApi
    fun `test - kotlinxDatetime`() {
        doTest(
            dependenciesDir.resolve("kotlinxDatetime"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxDatetime),
                exportedDependencies = setOf(testLibraryKotlinxDatetime)
            )
        )
    }

    /**
     * See KT-68480
     */
    @Test
    @TodoAnalysisApi
    fun `test - kotlinxCoroutines`() {
        doTest(
            dependenciesDir.resolve("kotlinxCoroutines"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxCoroutines),
                exportedDependencies = setOf(testLibraryKotlinxCoroutines)
            )
        )
    }

    @Test
    fun `test - propertyAnnotation`() {
        doTest(
            dependenciesDir.resolve("propertyAnnotation"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationCore, testLibraryKotlinxSerializationJson)
            )
        )
    }

    @Test
    fun `test - jsonNamingStrategy`() {
        doTest(
            dependenciesDir.resolve("jsonNamingStrategy"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationCore, testLibraryKotlinxSerializationJson)
            )
        )
    }

    @Test
    fun `test - notExportedDependency`() {
        doTest(
            dependenciesDir.resolve("notExportedDependency"), configuration = HeaderGenerator.Configuration(
                frameworkName = "MyApp",
                withObjCBaseDeclarationStubs = true,
                dependencies = listOf(testLibraryAKlibFile, testLibraryBKlibFile),
            )
        )
    }

    /**
     * https://youtrack.jetbrains.com/issue/KT-65327/Support-reading-klib-contents-in-Analysis-API
     * Requires being able to use AA to iterate over symbols to 'export' the dependency
     */
    @Test
    fun `test - exportedAndNotExportedDependency`() {
        doTest(
            dependenciesDir.resolve("exportedAndNotExportedDependency"), configuration = HeaderGenerator.Configuration(
                frameworkName = "MyApp",
                withObjCBaseDeclarationStubs = true,
                dependencies = listOf(testLibraryAKlibFile, testLibraryBKlibFile),
                exportedDependencies = setOf(testLibraryAKlibFile)
            )
        )
    }

    private fun doTest(root: File, configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration()) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root, configuration).toString()
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}