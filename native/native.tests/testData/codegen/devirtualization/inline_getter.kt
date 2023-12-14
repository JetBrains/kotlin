import kotlin.test.*

interface Base { val id: Int }

inline class Child(override val id: Int = 1) : Base

interface Base2 { val prop: Base }
class Child2(override val prop: Child) : Base2

fun box(): String {
    val x : Base = Child(5)
    assertEquals(5, x.id)
    val y : Base2 = Child2(Child(5))
    assertEquals("Child(id=5)", y.prop.toString())

    return "OK"
}