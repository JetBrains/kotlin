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
import kotlin.test.assertNull

class SwiftNameIndexingTest : IndexerTestsBase() {

    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `class without swift_name attribute`() {
        val clazz = indexObjCClass("""
            @interface Foo
            @end
        """.trimIndent())
        assertNull(clazz.swiftName)
    }

    @Test
    fun `class with swift_name attribute`() {
        val clazz = indexObjCClass("""
            __attribute__((swift_name("RenamedClass")))
            @interface Foo
            @end
        """.trimIndent())
        assertEquals("RenamedClass", clazz.swiftName)
    }

    @Test
    fun `protocol without swift_name attribute`() {
        val protocol = indexObjCProtocol("""
            @protocol Foo
            @end
        """.trimIndent())
        assertNull(protocol.swiftName)
    }

    @Test
    fun `protocol with swift_name attribute`() {
        val protocol = indexObjCProtocol("""
            __attribute__((swift_name("SwiftProtocol")))
            @protocol Foo
            @end
        """.trimIndent())
        assertEquals("SwiftProtocol", protocol.swiftName)
    }

    @Test
    fun `method without swift_name`() {
        val method = indexObjCMethod("""
            @interface Foo
            - (void)doSomething;
            @end
        """.trimIndent())
        assertNull(method.swiftName)
    }

    @Test
    fun `method with swift_name attribute`() {
        val method = indexObjCMethod("""
            @interface Foo
            - (void)doSomething __attribute__((swift_name("performAction()")));
            @end
        """.trimIndent())
        assertEquals("performAction()", method.swiftName)
    }

    @Test
    fun `method with swift_name and parameters`() {
        val method = indexObjCMethod("""
            @interface Foo
            - (void)doSomething:(int)value with:(char *)name __attribute__((swift_name("perform(value:name:)")));
            @end
        """.trimIndent())
        assertEquals("perform(value:name:)", method.swiftName)
    }

    @Test
    fun `init method with swift_name`() {
        val method = indexObjCMethod("""
            @interface Foo
            - (instancetype)initWithValue:(int)value
                __attribute__((swift_name("swiftNameInit(value:)")));
            @end
        """.trimIndent())
        assertEquals("swiftNameInit(value:)", method.swiftName)
    }

    @Test
    fun `class method with swift_name`() {
        val method = indexObjCMethod("""
            @interface Foo
            + (void)factoryMethod __attribute__((swift_name("create()")));
            @end
        """.trimIndent())
        assertEquals("create()", method.swiftName)
    }

    @Test
    fun `property without swift_name`() {
        val property = indexObjCProperty("""
            @interface Foo
            @property int value;
            @end
        """.trimIndent())
        assertNull(property.swiftName)
    }

    @Test
    fun `property with swift_name attribute`() {
        val property = indexObjCProperty("""
            @interface Foo
            @property int value __attribute__((swift_name("renamedValue")));
            @end
        """.trimIndent())
        assertEquals("renamedValue", property.swiftName)
    }

    @Test
    fun `readonly property with swift_name`() {
        val property = indexObjCProperty("""
            @interface Foo
            @property (readonly) int count __attribute__((swift_name("numberOfItems")));
            @end
        """.trimIndent())
        assertEquals("numberOfItems", property.swiftName)
    }

    private fun indexObjCClass(headerContents: String, className: String = "Foo"): ObjCClass =
            indexObjCHeader(headerContents).index.objCClasses.single { it.name == className }

    private fun indexObjCProtocol(headerContents: String, protocolName: String = "Foo"): ObjCProtocol =
            indexObjCHeader(headerContents).index.objCProtocols.single { it.name == protocolName }

    private fun indexObjCMethod(headerContents: String, className: String = "Foo"): ObjCMethod =
            indexObjCClasses(headerContents).single { it.name == className }.methods.single()

    private fun indexObjCProperty(headerContents: String, className: String = "Foo"): ObjCProperty =
            indexObjCClasses(headerContents).single { it.name == className }.properties.single()

    private fun indexObjCClasses(headerContents: String): Collection<ObjCClass> =
            indexObjCHeader(headerContents).index.objCClasses

    private fun indexObjCHeader(headerContents: String): IndexerResult =
            index(headerContents, language = Language.OBJECTIVE_C)
}

