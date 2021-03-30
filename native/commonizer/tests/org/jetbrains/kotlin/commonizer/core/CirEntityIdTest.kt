/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.name.ClassId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CirEntityIdTest {
    @Test
    fun createAndIntern() {
        class TestRow(
            val rawEntityId: String,
            val packageSegments: Array<String>,
            val rawRelativeNameSegments: List<String>
        )

        listOf(
            TestRow(
                rawEntityId = "",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = emptyList()
            ),
            TestRow(
                rawEntityId = "/",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = emptyList()
            ),
            TestRow(
                rawEntityId = "foo/",
                packageSegments = arrayOf("foo"),
                rawRelativeNameSegments = emptyList()
            ),
            TestRow(
                rawEntityId = "foo/bar/",
                packageSegments = arrayOf("foo", "bar"),
                rawRelativeNameSegments = emptyList()
            ),
            TestRow(
                rawEntityId = "foo/bar/baz/",
                packageSegments = arrayOf("foo", "bar", "baz"),
                rawRelativeNameSegments = emptyList()
            ),
            TestRow(
                rawEntityId = "My",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = listOf("My")
            ),
            TestRow(
                rawEntityId = "My.Test",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = listOf("My", "Test")
            ),
            TestRow(
                rawEntityId = "My.Test.Class",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = listOf("My", "Test", "Class")
            ),
            TestRow(
                rawEntityId = "/My",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = listOf("My")
            ),
            TestRow(
                rawEntityId = "/My.Test",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = listOf("My", "Test")
            ),
            TestRow(
                rawEntityId = "/My.Test.Class",
                packageSegments = emptyArray(),
                rawRelativeNameSegments = listOf("My", "Test", "Class")
            ),
            TestRow(
                rawEntityId = "foo/My",
                packageSegments = arrayOf("foo"),
                rawRelativeNameSegments = listOf("My")
            ),
            TestRow(
                rawEntityId = "foo/My.Test",
                packageSegments = arrayOf("foo"),
                rawRelativeNameSegments = listOf("My", "Test")
            ),
            TestRow(
                rawEntityId = "foo/My.Test.Class",
                packageSegments = arrayOf("foo"),
                rawRelativeNameSegments = listOf("My", "Test", "Class")
            ),
            TestRow(
                rawEntityId = "foo/bar/My",
                packageSegments = arrayOf("foo", "bar"),
                rawRelativeNameSegments = listOf("My")
            ),
            TestRow(
                rawEntityId = "foo/bar/My.Test",
                packageSegments = arrayOf("foo", "bar"),
                rawRelativeNameSegments = listOf("My", "Test")
            ),
            TestRow(
                rawEntityId = "foo/bar/My.Test.Class",
                packageSegments = arrayOf("foo", "bar"),
                rawRelativeNameSegments = listOf("My", "Test", "Class")
            ),
            TestRow(
                rawEntityId = "foo/bar/baz/My",
                packageSegments = arrayOf("foo", "bar", "baz"),
                rawRelativeNameSegments = listOf("My")
            ),
            TestRow(
                rawEntityId = "foo/bar/baz/My.Test",
                packageSegments = arrayOf("foo", "bar", "baz"),
                rawRelativeNameSegments = listOf("My", "Test")
            ),
            TestRow(
                rawEntityId = "foo/bar/baz/My.Test.Class",
                packageSegments = arrayOf("foo", "bar", "baz"),
                rawRelativeNameSegments = listOf("My", "Test", "Class")
            )
        ).forEach { testRow ->
            with(testRow) {
                // nullable, because ClassId may not have empty class name
                val classifierId: ClassId? = if (rawRelativeNameSegments.isNotEmpty()) ClassId.fromString(rawEntityId) else null

                val packageName = CirPackageName.create(packageSegments)
                val relativeNameSegments = rawRelativeNameSegments.map(CirName::create).toTypedArray()

                val entityIds = listOfNotNull(
                    CirEntityId.create(rawEntityId),
                    CirEntityId.create(rawEntityId),
                    classifierId?.let(CirEntityId::create),
                    classifierId?.let(CirEntityId::create),
                    CirEntityId.create(packageName, relativeNameSegments),
                    CirEntityId.create(packageName, relativeNameSegments)
                )

                val first = entityIds.first()
                entityIds.forEach { entityId ->
                    assertSame(first, entityId)
                    assertSame(packageName, entityId.packageName)
                    assertTrue(relativeNameSegments.contentEquals(entityId.relativeNameSegments))
                }
            }
        }
    }

    @Test
    fun toStringConversion() {
        listOf(
            "" to "/",
            "/" to "/",
            "foo/" to "foo/",
            "foo/bar/" to "foo/bar/",
            "foo/bar/baz/" to "foo/bar/baz/",
            "My" to "/My",
            "My.Test" to "/My.Test",
            "My.Test.Class" to "/My.Test.Class",
            "foo/My" to "foo/My",
            "foo/bar/My.Test" to "foo/bar/My.Test",
            "foo/bar/baz/My.Test.Class" to "foo/bar/baz/My.Test.Class"
        ).forEach { (rawEntityId, asStringRepresentation) ->
            assertEquals(asStringRepresentation, CirEntityId.create(rawEntityId).toString())
        }
    }

    @Test
    fun isNested() {
        listOf(
            "" to false,
            "/" to false,
            "foo/" to false,
            "foo/bar/" to false,
            "My" to false,
            "My.Test" to true,
            "My.Test.Class" to true,
            "/My" to false,
            "/My.Test" to true,
            "/My.Test.Class" to true,
            "foo/My" to false,
            "foo/My.Test" to true,
            "foo/My.Test.Class" to true,
            "foo/bar/My" to false,
            "foo/bar/My.Test" to true,
            "foo/bar/My.Test.Class" to true,
        ).forEach { (rawEntityId, isNested) ->
            assertEquals(isNested, CirEntityId.create(rawEntityId).isNestedEntity)
        }
    }

    @Test
    fun createNested() {
        val nested1 = CirName.create("Nested1")
        val nested2 = CirName.create("Nested2")

        listOf(
            "",
            "/",
            "foo/",
            "foo/bar/",
            "Outer",
            "/Outer",
            "foo/Outer",
            "foo/bar/Outer",
            "Outer.Nested",
            "/Outer.Nested",
            "foo/Outer.Nested",
            "foo/bar/Outer.Nested"
        ).forEach { rawEntityId ->
            val entityId = CirEntityId.create(rawEntityId)
            val n1 = entityId.createNestedEntityId(nested1)
            assertSame(entityId.packageName, n1.packageName)
            assertEquals(entityId.relativeNameSegments.isNotEmpty(), n1.isNestedEntity)
            assertEquals(entityId.relativeNameSegments.toList(), n1.relativeNameSegments.dropLast(1))

            val n2 = n1.createNestedEntityId(nested2)
            assertSame(entityId.packageName, n2.packageName)
            assertTrue(n2.isNestedEntity)
            assertEquals(entityId.relativeNameSegments.toList(), n2.relativeNameSegments.dropLast(2))
        }
    }

    @Test
    fun createParent() {
        listOf(
            "" to null,
            "foo/" to null,
            "foo/bar/" to null,
            "Outer" to null,
            "foo/Outer" to null,
            "foo/bar/Outer" to null,
            "Outer.Nested1" to "Outer",
            "foo/Outer.Nested1" to "foo/Outer",
            "foo/bar/Outer.Nested1" to "foo/bar/Outer",
            "Outer.Nested1.Nested2" to "Outer.Nested1",
            "foo/Outer.Nested1.Nested2" to "foo/Outer.Nested1",
            "foo/bar/Outer.Nested1.Nested2" to "foo/bar/Outer.Nested1"
        ).forEach { (rawEntityId, rawParentEntityId) ->
            val entityId = CirEntityId.create(rawEntityId)
            val parentEntityId = rawParentEntityId?.let(CirEntityId::create)
            assertSame(parentEntityId, entityId.getParentEntityId())
        }
    }
}

