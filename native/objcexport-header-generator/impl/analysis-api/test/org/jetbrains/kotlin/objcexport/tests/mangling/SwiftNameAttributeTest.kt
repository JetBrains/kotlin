package org.jetbrains.kotlin.objcexport.tests.mangling

import org.jetbrains.kotlin.objcexport.mangling.SwiftNameAttribute
import org.jetbrains.kotlin.objcexport.mangling.buildMangledSelectors
import org.jetbrains.kotlin.objcexport.mangling.mangleAttribute
import org.jetbrains.kotlin.objcexport.mangling.parseSwiftNameAttribute
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class SwiftNameAttributeTest {

    @Test
    fun `test - no parameters`() {
        assertEquals(
            SwiftNameAttribute("foo", emptyList()),
            parseSwiftNameAttribute("swift_name(\"foo()\")")
        )
    }

    @Test
    fun `test - multiple parameters`() {
        assertEquals(
            SwiftNameAttribute("foo", listOf("p0:", "p1:")),
            parseSwiftNameAttribute("swift_name(\"foo(p0:p1:)\")")
        )
    }

    @Test
    fun `test - receiver`() {
        assertEquals(
            SwiftNameAttribute("foo", listOf("_:", "p0:")),
            parseSwiftNameAttribute("swift_name(\"foo(_:p0:)\")")
        )
    }

    @Test
    fun `test - invalid attribute name`() {
        assertFails {
            parseSwiftNameAttribute("swift(\"foo(p0:)\")")
        }
    }

    @Test
    fun `test - no method name`() {
        assertFails {
            parseSwiftNameAttribute("swift_name(\"(p0:)\")")
        }
    }

    @Test
    fun `test - building mangled selectors with no parameters`() {
        assertEquals(
            listOf("foo"),
            buildMangledSelectors(SwiftNameAttribute("foo", emptyList(), "_"))
        )
    }

    @Test
    fun `test - building mangled selectors with 1 parameter`() {
        val attr = SwiftNameAttribute(
            methodName = "foo",
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
        val attr = SwiftNameAttribute(
            methodName = "foo",
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
        val attr = SwiftNameAttribute(
            methodName = "foo",
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
            SwiftNameAttribute(
                methodName = "foo",
                parameters = listOf("p0:"),
                postfix = "__"
            ),
            SwiftNameAttribute(
                methodName = "foo",
                parameters = listOf("p0:"),
                postfix = "_"
            ).mangleAttribute()
        )
    }
}