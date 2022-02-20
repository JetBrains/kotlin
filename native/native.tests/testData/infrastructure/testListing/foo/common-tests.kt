package infrastructure.testListing.foo

import kotlin.test.*

@Test
fun topLevel() = Unit

class Outer {
    @Test
    fun outer() = Unit

    class Nested {
        @Test
        fun nested() = Unit
    }
}

object O {
    @Test
    fun testObject() = Unit
}
