package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl
import org.jetbrains.kotlin.objcexport.reorderExtensionsIfNeeded
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestExtensionsReordering {
    @Test
    fun `test - simple valid order with no reordering`() {
        val foo = ObjCInterfaceImpl(name = "Foo", categoryName = null)
        val fooExtension = ObjCInterfaceImpl(name = "Foo", categoryName = "extensions")

        assertEquals(
            listOf(foo, fooExtension),
            listOf(foo, fooExtension).reorderExtensionsIfNeeded()
        )
    }

    @Test
    fun `test - simple invalid order with reordering`() {
        val foo = ObjCInterfaceImpl(name = "Foo", categoryName = null)
        val fooExtension = ObjCInterfaceImpl(name = "Foo", categoryName = "extensions")

        assertEquals(
            listOf(foo, fooExtension),
            listOf(fooExtension, foo).reorderExtensionsIfNeeded()
        )
    }

    @Test
    fun `test - keeping index of extension without defined interface`() {
        val fooA = ObjCInterfaceImpl(name = "FooA", categoryName = null)
        val fooB = ObjCInterfaceImpl(name = "FooB", categoryName = null)
        val bar = ObjCInterfaceImpl(name = "Bar", categoryName = "extensions")

        assertEquals(
            listOf(fooA, bar, fooB),
            listOf(fooA, bar, fooB).reorderExtensionsIfNeeded()
        )
    }
}

private fun ObjCInterfaceImpl(
    name: String,
    categoryName: String?,
) = ObjCInterfaceImpl(
    name = name,
    categoryName = categoryName,
    comment = null,
    origin = null,
    attributes = emptyList(),
    superProtocols = emptyList(),
    members = emptyList(),
    generics = emptyList(),
    superClass = null,
    superClassGenerics = emptyList(),
)