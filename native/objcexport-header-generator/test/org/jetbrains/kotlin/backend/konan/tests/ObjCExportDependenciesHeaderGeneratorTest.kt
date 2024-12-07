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
    fun `test - kotlinxSerializationJson`() {
        doTest(
            dependenciesDir.resolve("kotlinxSerializationJson"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationJson),
                exportedDependencies = setOf(testLibraryKotlinxSerializationJson)
            )
        )
    }

    /**
     * See KT-68478
     */
    @Test
    @TodoAnalysisApi
    fun `test - kotlinxSerializationCore`() {
        doTest(
            dependenciesDir.resolve("kotlinxSerializationCore"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationCore),
                exportedDependencies = setOf(testLibraryKotlinxSerializationCore)
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
                dependencies = listOfNotNull(testLibraryKotlinxCoroutines, testLibraryAtomicFu),
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

    @Test
    fun `test - property with companion type from dependency`() {
        doTest(dependenciesDir.resolve("propertyWithCompanionTypeFromDependency"))
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

    @Test
    fun `test - completionCoroutinesHandlerException`() {
        doTest(
            dependenciesDir.resolve("completionCoroutinesHandlerException"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxCoroutines)
            )
        )
    }

    @Test
    fun `test - testInternalLibrary`() {
        doTest(
            dependenciesDir.resolve("testInternalLibrary"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testInternalKlibFile),
                exportedDependencies = setOf(testInternalKlibFile)
            )
        )
    }

    @Test
    fun `test - extensions library`() {
        doTest(
            dependenciesDir.resolve("extensionsLibrary"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testExtensionsKlibFile),
                exportedDependencies = setOf(testExtensionsKlibFile)
            )
        )
    }

    /**
     * Disabled because of:
     * - KT-70319 annotation doc translation
     * - KT-69742 mangling
     */
    @Test
    @TodoAnalysisApi
    fun `test - DateTimeUnit`() {
        doTest(
            dependenciesDir.resolve("dateTimeUnit"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxDatetime, testLibraryKotlinxSerializationCore)
            )
        )
    }

    /**
     * Depends on unimplemented AA deprecation message: KT-67823
     */
    @Test
    @TodoAnalysisApi
    fun `test - MapLikeSerializer`() {
        doTest(
            dependenciesDir.resolve("mapLikeSerializer"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxSerializationCore)
            )
        )
    }

    @Test
    fun `test - top level function and extension with the same dependency doesn't generate duplicate`() {
        doTest(
            dependenciesDir.resolve("topLevelFunctionAndExtensionWithDependency"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testExtensionsKlibFile)
            )
        )
    }

    @Test
    fun `test - one type extensions from multiple files merged into the same category`() {
        doTest(
            dependenciesDir.resolve("oneTypeExtensionsFromMultipleFilesMergedIntoTheSameCategory"),
            configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testExtensionsKlibFile),
                exportedDependencies = setOf(testExtensionsKlibFile)
            )
        )
    }

    /**
     * Disabled because of
     * - KT-66510 init order
     * - KT-67823 deprecation message
     */
    @Test
    @TodoAnalysisApi
    fun `test - CoroutineDispatcherKey`() {
        doTest(
            dependenciesDir.resolve("coroutineDispatcherKey"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxCoroutines)
            )
        )
    }

    @Test
    fun `test - class name mangling`() {
        doTest(
            dependenciesDir.resolve("classNameMangling"), configuration = HeaderGenerator.Configuration(
                dependencies = listOf(testLibraryCKlibFile),
                exportedDependencies = setOf(testLibraryCKlibFile),
            )
        )
    }

    @Test
    fun `test - suspend handler translated as result and error parameters`() {
        doTest(
            root = dependenciesDir.resolve("suspendHandlerTranslatedAsResultAndErrorParameters"),
            configuration = HeaderGenerator.Configuration(dependencies = listOf(testLibraryKotlinxCoroutines))
        )
    }

    private fun doTest(root: File, configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration()) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root, configuration).toString()
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}