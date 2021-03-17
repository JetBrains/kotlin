/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.name.FqName
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CirPackageNameTest {
    @Test
    fun createAndIntern() {
        listOf(
            "" to emptyArray(),
            "foo" to arrayOf("foo"),
            "foo.bar" to arrayOf("foo", "bar"),
            "foo.bar.baz" to arrayOf("foo", "bar", "baz")
        ).forEach { (rawPackageFqName, segments) ->
            val packageFqName = FqName(rawPackageFqName)

            val packageNames = listOf(
                CirPackageName.create(rawPackageFqName),
                CirPackageName.create(rawPackageFqName),
                CirPackageName.create(packageFqName),
                CirPackageName.create(packageFqName),
                CirPackageName.create(segments),
                CirPackageName.create(segments)
            )

            val first = packageNames.first()
            packageNames.forEach { packageName ->
                assertTrue(segments.contentEquals(packageName.segments))
                assertSame(first, packageName)
            }
        }
    }

    @Test
    fun createRoot() {
        val rootPackageNames = listOf(
            CirPackageName.create(""),
            CirPackageName.create(FqName("")),
            CirPackageName.create(FqName.ROOT),
            CirPackageName.create(emptyArray())
        )

        val first = rootPackageNames.first()
        rootPackageNames.forEach { rootPackageName ->
            assertSame(first, rootPackageName)
            assertTrue(rootPackageName.segments.isEmpty())
            assertTrue(rootPackageName.isRoot())
            assertSame(CirPackageName.ROOT, rootPackageName)
        }
    }

    @Test
    fun toStringConversion() {
        listOf(
            "" to emptyArray(),
            "foo" to arrayOf("foo"),
            "foo.bar" to arrayOf("foo", "bar"),
            "foo.bar.baz" to arrayOf("foo", "bar", "baz")
        ).forEach { (rawPackageFqName, segments) ->
            assertEquals(rawPackageFqName, CirPackageName.create(segments).toString())
        }
    }

    @Test
    fun toMetadataStringConversion() {
        listOf(
            "" to "",
            "foo" to "foo",
            "foo.bar" to "foo/bar",
            "foo.bar.baz" to "foo/bar/baz"
        ).forEach { (rawPackageFqName, metadataPackageFqName) ->
            assertEquals(metadataPackageFqName, CirPackageName.create(rawPackageFqName).toMetadataString())
        }
    }

    @Test
    fun startsWith() {
        val packageNames = listOf("", "foo", "foo.bar", "foo.bar.baz").map(CirPackageName::create)

        for (i in packageNames.indices) {
            val a = packageNames[i]
            for (j in packageNames.indices) {
                val b = packageNames[j]
                assertEquals(i >= j, a.startsWith(b))
            }
        }
    }

    @Test
    fun notStartsWith() {
        listOf(
            "aa" to "ab",
            "aa.bb" to "aa.bc",
            "aa.bb" to "aa.bb.cc"
        ).forEach { (a, b) ->
            assertFalse(CirPackageName.create(a).startsWith(CirPackageName.create(b)))
        }
    }
}
