interface Base { val id: Int }

inline class Child(override val id: Int = 1) : Base

interface Base2 { val prop: Base }
class Child2(override val prop: Child) : Base2

fun main() {
    val x : Base = Child(5)
    println(x.id)
    val y : Base2 = Child2(Child(5))
    println(y.prop)
}