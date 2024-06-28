import kotlin.test.*

fun box(): String {
    open class S
    class P(val str: String = "P") : S()

    val s: S = P()
    val p: Any = P("A")

    assertTrue(s is P)
    assertEquals("P", s.str)
    assertFalse(p !is P)
    assertEquals("A", p.str)

    val nullableT: P? = P("N")
    assertNotNull(nullableT)
    assertEquals("N", nullableT.str)

    return "OK"
}