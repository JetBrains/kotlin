import kotlin.test.*

@Test
fun checkFoo() {
    assertEquals(42, foo(intArrayOf(1, 2, 3, 4, 5, 6)) { it * 2 })
}

@Test
fun checkBar() {
    assertEquals(42, bar(intArrayOf(1, 3, 7)) { it * 6 })
}