package infrastructure.testListing.bar

import kotlin.test.*
import infrastructure.testListing.foo.fortyTwo

@Test
fun testFortyTwo() {
    assertEquals(42, fortyTwo())
}
