import kotlin.test.*
import test.*

@Test
fun runTest() {
    val user = User(30)
    assertEquals("Extension function v2: 30", bar(user))
}
