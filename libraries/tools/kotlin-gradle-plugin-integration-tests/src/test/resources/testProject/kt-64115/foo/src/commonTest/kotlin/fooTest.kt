package org.example.foo

import org.example.bar.Bar
import kotlin.test.Test
import kotlin.test.assertEquals

class FooTest {
    @Test
    fun test() {
        assertEquals(
            "Bar(foo=Foo(value=hello))",
            Bar(Foo("hello")).toString()
        )
    }
}
