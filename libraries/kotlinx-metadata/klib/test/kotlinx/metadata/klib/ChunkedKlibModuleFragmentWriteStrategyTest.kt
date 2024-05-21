/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChunkedKlibModuleFragmentWriteStrategyTest {
    @Test
    fun testChunking() {
        val classes: Map</* top-level class name */ ClassName, /* number of subclasses */ Int> = mapOf(
            "sample/Foo" to 10,
            "sample/Bar" to 100,
            "sample/Baz" to 1000,
        )

        val numberOfClasses = classes.size + classes.values.sum()
        val numberOfTypeAliases = 25
        val numberOfTopLevelProperties = 300
        val numberOfTopLevelFunctions = 200

        val numberOfTopLevelCallables = numberOfTopLevelProperties + numberOfTopLevelFunctions

        val testData = listOf(
            20 to 20,
            40 to 40,
            64 to 5,
            64 to 50,
            64 to 128,
        )

        for ((classifiersPerFile, callablesPerFile) in testData) {
            val originalFragment = KmModuleFragment().apply {
                fqName = "sample"
                pkg = KmPackage().apply { fqName = "sample" }
            }
            originalFragment.generateDeclarations(classes, numberOfTypeAliases, numberOfTopLevelProperties, numberOfTopLevelFunctions)

            assertEquals(numberOfClasses, originalFragment.classes.size)
            assertEquals(numberOfClasses, originalFragment.className.size)
            assertEquals(numberOfTypeAliases, originalFragment.pkg!!.typeAliases.size)
            assertEquals(numberOfTopLevelProperties, originalFragment.pkg!!.properties.size)
            assertEquals(numberOfTopLevelFunctions, originalFragment.pkg!!.functions.size)

            val chunkedFragments = ChunkedKlibModuleFragmentWriteStrategy(
                topLevelClassifierDeclarationsPerFile = classifiersPerFile,
                topLevelCallableDeclarationsPerFile = callablesPerFile
            ).processPackageParts(listOf(originalFragment))

            assertEquals(numberOfClasses, chunkedFragments.sumOf { it.classes.size })
            assertEquals(numberOfClasses, chunkedFragments.sumOf { it.className.size })
            assertEquals(numberOfTypeAliases, chunkedFragments.sumOf { it.pkg?.typeAliases?.size ?: 0 })
            assertEquals(numberOfTopLevelProperties, chunkedFragments.sumOf { it.pkg?.properties?.size ?: 0 })
            assertEquals(numberOfTopLevelFunctions, chunkedFragments.sumOf { it.pkg?.functions?.size ?: 0 })

            val chunksWithClassifiers =
                chunkedFragments.filter { it.classes.isNotEmpty() || it.pkg?.typeAliases?.isNotEmpty() == true }
            val chunksWithTopLevelCallables =
                chunkedFragments.filter { it.pkg?.let { pkg -> pkg.properties.isNotEmpty() || pkg.functions.isNotEmpty() } ?: false }

            assertEquals(chunksWithClassifiers.size + chunksWithTopLevelCallables.size, chunkedFragments.size)

            val distributionOfTopLevelClassNamesOverChunks =
                hashMapOf</* top-level class name */ ClassName, /* chunks */ MutableList<KmModuleFragment>>()

            for (chunk in chunksWithClassifiers) {
                val classNames = chunk.className
                val topLevelClassNames = classNames.mapToSetOrEmpty { it.substringBefore('.') }
                for (topLevelClassName in topLevelClassNames) {
                    distributionOfTopLevelClassNamesOverChunks.computeIfAbsent(topLevelClassName) { mutableListOf() } += chunk
                }
            }

            val repeatedTopLevelClassNamesInChunks =
                distributionOfTopLevelClassNamesOverChunks.filterValues { chunks -> chunks.size > 1 }
            assertTrue { repeatedTopLevelClassNamesInChunks.isEmpty() }

            val expectedNumberOfChunksWithTopLevelCallables = numberOfTopLevelCallables / callablesPerFile +
                    (if (numberOfTopLevelCallables % callablesPerFile > 0) 1 else 0)
            assertEquals(expectedNumberOfChunksWithTopLevelCallables, chunksWithTopLevelCallables.size)
        }
    }

    companion object {
        private fun KmModuleFragment.generateDeclarations(
            classes: Map</* top-level class name */ ClassName, /* number of subclasses */ Int>,
            numberOfTypeAliases: Int,
            numberOfTopLevelProperties: Int,
            numberOfTopLevelFunctions: Int,
        ) {
            assertTrue { classes.isNotEmpty() }
            assertTrue { numberOfTypeAliases > 0 }
            assertTrue { numberOfTopLevelProperties > 0 }
            assertTrue { numberOfTopLevelFunctions > 0 }

            fun generateClass(className: ClassName) {
                this.classes += KmClass().apply { name = className }
                this.className += className
            }

            for ((topLevelClassName, numberOfSubclasses) in classes) {
                generateClass(topLevelClassName)
                repeat(numberOfSubclasses) { generateClass("$topLevelClassName.Subclass${it + 1}") }
            }

            val pkg = pkg
            assertNotNull(pkg)

            repeat(numberOfTypeAliases) { pkg.typeAliases += KmTypeAlias("TypeAlias${it + 1}") }
            repeat(numberOfTopLevelProperties) { pkg.properties += KmProperty("property${it + 1}") }
            repeat(numberOfTopLevelFunctions) { pkg.functions += KmFunction("function${it + 1}") }
        }
    }
}
