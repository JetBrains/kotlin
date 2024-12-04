package org.jetbrains.kotlin.objcexport.tests.mangling

import org.jetbrains.kotlin.objcexport.mangling.isReceiver
import org.jetbrains.kotlin.objcexport.mangling.isSwiftNameMethod
import org.jetbrains.kotlin.objcexport.mangling.isSwiftNameProperty
import org.jetbrains.kotlin.objcexport.mangling.mangleSelector
import org.jetbrains.kotlin.objcexport.testUtils.objCInitMethod
import org.jetbrains.kotlin.objcexport.testUtils.objCMethod
import org.jetbrains.kotlin.objcexport.testUtils.objCProperty
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManglingUtilsTest {
    @Test
    fun `test - selector mangling`() {
        assertEquals("a_:", "a:".mangleSelector("_"))
        assertEquals("a_", "a".mangleSelector("_"))
    }

    @Test
    fun `test - isSwiftNameProperty`() {
        assertTrue(objCProperty(declarationAttributes = listOf("swift_name")).isSwiftNameProperty())
        assertFalse(objCProperty(declarationAttributes = listOf("deprecated")).isSwiftNameProperty())
    }

    @Test
    fun `test - isSwiftNameMethod`() {
        assertTrue(
            objCMethod(
                selector = "foo",
                attributes = listOf("swift_name(\"foo()\")")
            ).isSwiftNameMethod()
        )

        assertFalse(objCMethod(selector = "foo", attributes = listOf("deprecated")).isSwiftNameMethod())
        assertFalse(objCInitMethod().isSwiftNameMethod())
    }

    @Test
    fun `test - isReceiver`() {
        assertTrue("_:".isReceiver)
    }
}