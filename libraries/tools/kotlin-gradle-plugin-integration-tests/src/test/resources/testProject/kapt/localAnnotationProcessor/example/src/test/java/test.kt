package org.kotlin.test

import org.junit.Test
import org.junit.Assert.*
import test.generated.simpleClassName

class AnnotationTest {
    @Test fun testSimple() {
        assertEquals("SimpleClass", SimpleClass().simpleClassName)
    }
}