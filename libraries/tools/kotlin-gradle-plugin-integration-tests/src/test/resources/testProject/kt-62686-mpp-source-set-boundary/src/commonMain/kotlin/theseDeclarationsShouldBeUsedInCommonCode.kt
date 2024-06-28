
open class Base
class Child: Base()

fun foo(x: Base): String = ""

expect class A() {
    fun foo(x: Base): String
}
