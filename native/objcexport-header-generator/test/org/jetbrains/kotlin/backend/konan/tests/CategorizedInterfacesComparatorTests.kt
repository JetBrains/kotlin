package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceOrder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CategorizedInterfacesComparatorTests {

    @Test
    fun `test - no extensions remained in the same order`() {
        val fooA = ObjCInterfaceImpl(name = "A")
        val fooB = ObjCInterfaceImpl(name = "B")
        val fooC = ObjCInterfaceImpl(name = "C")

        assertEquals(
            listOf(fooA, fooB, fooC),
            listOf(fooA, fooB, fooC).sortedWith(ObjCInterfaceOrder)
        )
    }

    @Test
    fun `test - all extensions remained in the same order`() {
        val fooA = ObjCInterfaceImpl(name = "A", categoryName = "A")
        val fooB = ObjCInterfaceImpl(name = "B", categoryName = "B")
        val fooC = ObjCInterfaceImpl(name = "C", categoryName = "C")

        assertEquals(
            listOf(fooA, fooB, fooC),
            listOf(fooA, fooB, fooC).sortedWith(ObjCInterfaceOrder)
        )
    }

    @Test
    fun `test - valid order with no reordering`() {
        val foo = ObjCInterfaceImpl(name = "Foo", categoryName = null)
        val fooExtension = ObjCInterfaceImpl(name = "Foo", categoryName = "extensions")

        assertEquals(
            listOf(foo, fooExtension),
            listOf(foo, fooExtension).sortedWith(ObjCInterfaceOrder)
        )
    }

    @Test
    fun `test - invalid order with reordering`() {
        val foo = ObjCInterfaceImpl(name = "Foo", categoryName = null)
        val fooExtension = ObjCInterfaceImpl(name = "Foo", categoryName = "extensions")

        assertEquals(
            listOf(foo, fooExtension),
            listOf(fooExtension, foo).sortedWith(ObjCInterfaceOrder)
        )
    }

    @Test
    fun `test - same interfaces with different categories remain the same order`() {
        val fooA = ObjCInterfaceImpl(name = "Foo", categoryName = "CategoryA")
        val fooB = ObjCInterfaceImpl(name = "Foo", categoryName = "CategoryB")

        assertEquals(
            listOf(fooA, fooB),
            listOf(fooA, fooB).sortedWith(ObjCInterfaceOrder)
        )
    }
}

private fun ObjCInterfaceImpl(
    name: String,
    categoryName: String? = null,
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