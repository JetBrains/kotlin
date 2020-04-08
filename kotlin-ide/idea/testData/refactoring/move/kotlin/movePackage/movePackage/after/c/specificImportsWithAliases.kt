package c

import b.a.A as AA
import b.a.foo as foofoo
import b.a.x as xx

fun bar() {
    val t: AA = AA()
    foofoo()
    println(xx)
    xx = ""
}
