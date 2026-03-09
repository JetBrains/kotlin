import kotlinx.powerassert.*
import kotlin.test.*

@PowerAssert
fun String.example(x: Int, y: Int = 1, @PowerAssert.Ignore z: Int = 2): CallExplanation? {
    return PowerAssert.explanation
}

fun box(): String {
    test1()
    test2()
    test3()
    return "OK"
}

fun test1() {
    val explanation = "extension".example(x = 0) ?: error("!")
    assertEquals(explanation.arguments.size, 4)
    val arguments = explanation.arguments.iterator()
    assertNotNull(arguments.next())
    assertNotNull(arguments.next())
    assertNull(arguments.next())
    assertNull(arguments.next())
}

fun test2() {
    val explanation = "extension".example(x = 0, y = 1) ?: error("!")
    assertEquals(explanation.arguments.size, 4)
    val arguments = explanation.arguments.iterator()
    assertNotNull(arguments.next())
    assertNotNull(arguments.next())
    assertNotNull(arguments.next())
    assertNull(arguments.next())
}

fun test3() {
    val explanation = "extension".example(x = 0, y = 1, z = 2) ?: error("!")
    assertEquals(explanation.arguments.size, 4)
    val arguments = explanation.arguments.iterator()
    assertNotNull(arguments.next())
    assertNotNull(arguments.next())
    assertNotNull(arguments.next())
    assertNull(arguments.next())
}
