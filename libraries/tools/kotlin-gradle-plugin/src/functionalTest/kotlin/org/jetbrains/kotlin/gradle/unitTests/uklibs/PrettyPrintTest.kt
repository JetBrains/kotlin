/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.prettyPrintedFileCollectionOf
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PrettyPrintTest {
    @Test
    fun testList() {
        assertEquals(
            """
                listOf(
                  "1",
                  listOf(
                    1,
                  ),
                )
            """.trimIndent(),
            listOf("1", listOf(1)).prettyPrinted.toString(),
        )
        // Sanity check values are not sorted in a list
        assertEquals(
            """
                listOf(
                  "b",
                  "a",
                )
            """.trimIndent(),
            listOf("b", "a").prettyPrinted.toString()
        )
    }

    @Test
    fun testMap() {
        assertEquals(
            """
                mapOf(
                  "baz" to mapOf(
                    "a" to 1,
                  ),
                  "foo" to "bar",
                )
            """.trimIndent(),
            mapOf("foo" to "bar", "baz" to mapOf("a" to 1)).prettyPrinted.toString(),
        )
        // Keys must be sorted for proper diffing
        assertEquals(
            """
                mapOf(
                  "a" to "1",
                  "b" to "2",
                )
            """.trimIndent(),
            mapOf(
                "b" to "2",
                "a" to "1",
            ).prettyPrinted.toString()
        )
    }

    @Test
    fun testSet() {
        // Values must be sorted for proper diffing
        assertEquals(
            """
                setOf(
                  "a",
                  "b",
                )
            """.trimIndent(),
            setOf("b", "a").prettyPrinted.toString()
        )
    }

    @Test
    fun testClass() {
        data class A(
            val a: Int,
            val b: String,
        )
        data class B(val m: Map<String, A>)

        assertEquals(
            """
                mapOf(
                  A(
                  a = 1,
                  b = "1",
                ) to B(
                    m = mapOf(
                      "baz" to A(
                        a = 2,
                        b = "2",
                      ),
                    ),
                  ),
                )
            """.trimIndent(),
            mapOf(
                A(1, "1") to B(mapOf("baz" to A(2, "2")))
            ).prettyPrinted.toString(),
        )
    }

    @Test
    fun testFileCollection() {
        val project = buildProject {}
        val fc = project.files("file1.txt", "dir/file2.txt")
        assertEquals(
            """
                prettyPrintedFileCollectionOf(
                  "${project.projectDir}/file1.txt",
                  "${project.projectDir}/dir/file2.txt",
                )
            """.trimIndent(),
            fc.prettyPrinted.toString(),
        )
    }

    @Test
    fun testPrettyPrintedFileCollectionOf() {
        val project = buildProject {}
        val fc = project.files("file1.txt", "dir/file2.txt")

        assertEquals(
            prettyPrintedFileCollectionOf(
                "${project.projectDir}/file1.txt",
                "${project.projectDir}/dir/file2.txt",
            ).toString(),
            fc.prettyPrinted.toString(),
        )
    }

    @Test
    fun equalityComparisonOfPrettyPrintedTypes() {
        data class C(
            val value: String
        ) {
            // Equality of a.prettyPrinted == b.prettyPrinted must not depend on toString conversion
            override fun toString(): String {
                return "E"
            }
        }

        // Pretty printed wrapper type is not expected to be equal to the underlying type, so that equals is symmetric
        assertFalse(
            C("A").prettyPrinted.equals(C("A"))
        )
        assertFalse(
            C("A").equals(C("A").prettyPrinted)
        )
        assertEquals(
            C("A").prettyPrinted,
            C("A").prettyPrinted,
        )

        // assertThrows only accepts Exception
        assertEquals(
            AssertionError::class.java,
            runCatching {
                assertEquals(
                    C("A").prettyPrinted,
                    C("B").prettyPrinted,
                )
            }.exceptionOrNull()?.let { it::class.java },
        )
    }

    @Test
    fun prettyPrintedHashUsesUnderlyingTypesHashCode() {
        class C {
            override fun hashCode(): Int {
                return 0
            }
        }
        assertEquals(
            0,
            C().prettyPrinted.hashCode(),
        )
    }
}