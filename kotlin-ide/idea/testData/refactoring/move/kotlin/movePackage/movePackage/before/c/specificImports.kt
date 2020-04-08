package c

import a.A
import a.foo
import a.x

fun bar() {
    val t: A = A()
    foo()
    println(x)
    x = ""
}
