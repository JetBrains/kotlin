// FREE_COMPILER_ARGS: -opt -verbose

import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

@Test
fun division () {
    assertEquals(42, 126 / 3)
}
