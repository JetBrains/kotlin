import kotlin.test.*
import objclib.*

fun main() {
    assertEquals(1, getFrameworkInt())
    assertEquals(2, getDefInt())
    assertEquals(1, getFrameworkIntFromDef())
}
