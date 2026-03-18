/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.common.legacyKlibReverseTopoSort
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.uniqueName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.*

class LegacyKlibTopoSortSanityTest : AbstractNativeSimpleTest() {
    @Test
    fun test() {
        val klibs = ArrayList<KotlinLibrary>()

        val modules = newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            addRegularModule("c") { dependsOn("a") }
            addRegularModule("d") { dependsOn("b", "c") }
        }

        modules.compileToKlibsViaCli { _, successKlib ->
            val loadingResult = KlibLoader { libraryPaths(successKlib.resultingArtifact.klibFile) }.load()
            assertFalse(loadingResult.hasProblems)
            assertEquals(1, loadingResult.librariesStdlibFirst.size)
            klibs += loadingResult.librariesStdlibFirst[0]
        }

        assertEquals(4, klibs.size)

        // Imitate stdlib.
        klibs += object : KotlinLibrary {
            override val location get() = error("Unsupported")
            override val attributes get() = error("Unsupported")
            override fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>) = error("Unsupported")
            override val libraryFile get() = error("Unsupported")
            override val versions get() = error("Unsupported")

            override val manifestProperties = Properties().apply { this[KLIB_PROPERTY_UNIQUE_NAME] = "stdlib" }
        }

        for (permutation in klibs.permutations()) {
            val sortedKlibUniqueNames = permutation.legacyKlibReverseTopoSort().map { it.uniqueName }
            assertEquals(5, sortedKlibUniqueNames.size)
            assertEquals("stdlib", sortedKlibUniqueNames[0])
            assertEquals("a", sortedKlibUniqueNames[1])
            if (sortedKlibUniqueNames[2] == "b") {
                assertEquals("c", sortedKlibUniqueNames[3])
            } else {
                assertEquals("c", sortedKlibUniqueNames[2])
                assertEquals("b", sortedKlibUniqueNames[3])
            }
            assertEquals("d", sortedKlibUniqueNames[4])
        }
    }

    companion object {
        private fun <T> List<T>.permutations(): List<List<T>> {
            val solutions = mutableListOf<List<T>>()
            permutationsRecursive(toMutableList(), 0, solutions)
            return solutions
        }

        private fun <T> permutationsRecursive(input: MutableList<T>, index: Int, answers: MutableList<List<T>>) {
            if (index == input.lastIndex) answers.add(input.toList())
            for (i in index..input.lastIndex) {
                Collections.swap(input, index, i)
                permutationsRecursive(input, index + 1, answers)
                Collections.swap(input, i, index)
            }
        }
    }
}