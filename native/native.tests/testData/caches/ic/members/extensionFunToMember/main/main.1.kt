import kotlin.test.*
import test.*

@Test
fun runTest() {
    val user = User(30)
    assertEquals("Member function: 30", bar(user))
}
