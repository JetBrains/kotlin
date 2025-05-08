package org.jetbrains.kotlin.objcexport.tests.mangling

import org.jetbrains.kotlin.objcexport.mangling.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ParseSwiftNameAttributeTest {

    @Test
    fun `test - no parameters`() {
        assertEquals(
            ObjCMemberDetails("foo", emptyList()),
            parseSwiftMethodNameAttribute("swift_name(\"foo()\")")
        )
    }

    @Test
    fun `test - multiple parameters`() {
        assertEquals(
            ObjCMemberDetails("foo", listOf("p0:", "p1:")),
            parseSwiftMethodNameAttribute("swift_name(\"foo(p0:p1:)\")")
        )
    }

    @Test
    fun `test - receiver`() {
        assertEquals(
            ObjCMemberDetails("foo", listOf("_:", "p0:")),
            parseSwiftMethodNameAttribute("swift_name(\"foo(_:p0:)\")")
        )
    }

    @Test
    fun `test - invalid attribute name`() {
        assertFails {
            parseSwiftMethodNameAttribute("swift(\"foo(p0:)\")")
        }
    }

    @Test
    fun `test - no method name`() {
        assertFails {
            parseSwiftMethodNameAttribute("swift_name(\"(p0:)\")")
        }
    }

    @Test
    fun `test - building non mangled selectors with no parameters`() {
        assertEquals(
            listOf("foo"),
            buildMangledSelectors(ObjCMemberDetails("foo", emptyList(), false, ""))
        )
    }

    @Test
    fun `test - building mangled selectors with no parameters`() {
        assertEquals(
            listOf("foo_"),
            buildMangledSelectors(ObjCMemberDetails("foo", emptyList(), false, "_"))
        )
    }

    @Test
    fun `test - building mangled selectors with 1 parameter`() {
        val attr = ObjCMemberDetails(
            name = "foo",
            parameters = listOf("p0:"),
            postfix = "_"
        )
        assertEquals(
            listOf("fooP0_:"),
            buildMangledSelectors(attr)
        )
    }

    @Test
    fun `test - building mangled selectors with 2 parameters`() {
        val attr = ObjCMemberDetails(
            name = "foo",
            parameters = listOf("p0:", "p1:"),
            postfix = "_"
        )
        assertEquals(
            listOf("fooP0:", "p1_:"),
            buildMangledSelectors(attr)
        )
    }

    @Test
    fun `test - building mangled selectors with 3 parameters`() {
        val attr = ObjCMemberDetails(
            name = "foo",
            parameters = listOf("p0:", "p1:", "p2:"),
            postfix = "_"
        )
        assertEquals(
            listOf("fooP0:", "p1:", "p2_:"),
            buildMangledSelectors(attr)
        )
    }

    @Test
    fun `test - attribute mangling`() {
        assertEquals(
            ObjCMemberDetails(
                name = "foo",
                parameters = listOf("p0:"),
                postfix = "__"
            ),
            ObjCMemberDetails(
                name = "foo",
                parameters = listOf("p0:"),
                postfix = "_"
            ).mangleAttribute()
        )
    }

    @Test
    fun `test - method name with mangling prefix`() {

        assertEquals(
            ObjCMemberDetails("_pack", emptyList()),
            parseSwiftMethodNameAttribute("swift_name(\"_pack()\")")
        )

        assertEquals(
            ObjCMemberDetails("_foo", listOf("bar:")),
            parseSwiftMethodNameAttribute("swift_name(\"_foo(bar)\")")
        )
    }

    @Test
    fun `test - dropping parameters with brackets`() {
        assertEquals("foo", "foo()".extractMethodName())
        assertEquals("foo", "foo(a:)".extractMethodName())
        assertEquals("foo", "foo(a:b:)".extractMethodName())
    }

    @Test
    fun `test - swift name parameters parsing`() {
        assertEquals(emptyList(), parseSwiftNameParameters("foo()"))
        assertEquals(listOf("a:"), parseSwiftNameParameters("foo(a:)"))
        assertEquals(listOf("a:", "b:"), parseSwiftNameParameters("foo(a:b:)"))
    }

    @Test
    fun `test - invalid swift_name method format`() {
        assertEquals(emptyList(), parseSwiftNameParameters("foo"))
        assertEquals(emptyList(), parseSwiftNameParameters(""))
        assertEquals(emptyList(), parseSwiftNameParameters("foo("))
        assertEquals(emptyList(), parseSwiftNameParameters("foo)"))
    }
}