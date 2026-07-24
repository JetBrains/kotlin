import kotlin.test.*
import test.*

@Test
fun runTest() {
    val user = User(20)
    assertEquals("Extension function: 20", bar(user))
}
