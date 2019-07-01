import kotlin.test.*

import com.example.exported

@Test
fun foo() {
    val exp = exported()
    assertTrue(exp % 7 == 0, "Not divisible by 7")
    println("tests.foo: exp = $exp")
}