/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("infrastructure")
class InfrastructureNamedEntitiesTest {
    @Test
    fun parsePackageName() {
        listOf(
            "" to emptyList(),
            "foo" to listOf("foo"),
            "foo.bar" to listOf("foo", "bar"),
            "foo.bar.baz" to listOf("foo", "bar", "baz")
        ).forEach { (fqn, segments) ->
            assertEquals(segments, PackageName(fqn).segments)
            assertEquals(fqn, PackageName(segments).toString())
        }
    }

    @Test
    fun parseTestName() {
        listOf(
            "" to Triple(PackageName.EMPTY, null, ""),
            "foo" to Triple(PackageName.EMPTY, null, "foo"),
            "foo.bar" to Triple(PackageName("foo"), null, "bar"),
            "foo.bar.baz" to Triple(PackageName("foo.bar"), null, "baz"),
            "Foo.bar" to Triple(PackageName("Foo"), null, "bar"),
            "Foo.Bar.baz" to Triple(PackageName("Foo.Bar"), null, "baz"),
            "FooKt.bar" to Triple(PackageName.EMPTY, "FooKt", "bar"),
            "a.b.c.FooKt.bar" to Triple(PackageName("a.b.c"), "FooKt", "bar"),
            "__launcher__Kt.bar" to Triple(PackageName.EMPTY, "__launcher__Kt", "bar"),
            "a.b.c.__launcher__Kt.bar" to Triple(PackageName("a.b.c"), "__launcher__Kt", "bar"),
        ).forEach { (fqn, triple) ->
            val (packageName, packagePartClassName, functionName) = triple
            val testName = TestName(fqn)
            assertEquals(packageName, testName.packageName)
            assertEquals(packagePartClassName, testName.packagePartClassName)
            assertEquals(functionName, testName.functionName)
        }
    }
}
