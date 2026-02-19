/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypeParametersIndexingTest : IndexerTestsBase() {

    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `class without type parameters`() {
        val clazz = indexObjCClass("""
            @interface Foo
            @end
        """.trimIndent())
        assertEquals(emptyList(), clazz.typeParameters)
    }

    @Test
    fun `class with single type parameter`() {
        val clazz = indexObjCClass("""
            @interface Foo<ObjectType : id>
            @end
        """.trimIndent())
        assertEquals(listOf("ObjectType"), clazz.typeParameters)
    }

    @Test
    fun `class with multiple type parameters`() {
        val clazz = indexObjCClass("""
            @interface Foo<KeyType : id, ValueType : id>
            @end
        """.trimIndent())
        assertEquals(listOf("KeyType", "ValueType"), clazz.typeParameters)
    }

    private fun indexObjCClass(headerContents: String, className: String = "Foo"): ObjCClass =
            indexObjCHeader(headerContents).index.objCClasses.single { it.name == className }

    private fun indexObjCHeader(headerContents: String): IndexerResult =
            index(headerContents, language = Language.OBJECTIVE_C)
}
