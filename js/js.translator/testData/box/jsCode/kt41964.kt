// EXPECTED_REACHABLE_NODES: 1280

// MODULE: lib
// FILE: aaa.kt
package bar

fun aaa() {}

fun test1(aaa: String): String? {
    return js("aaa")
}

// MODULE: main(lib)
// FILE: bbb.kt
package foo

import bar.aaa
import bar.test1

fun bbb() {}

fun test2(aaa: String, bbb: Int): String {
    return "" + js("aaa") + js("bbb")
}

fun keep() {
    aaa()
    bbb()
}

fun box(): String {
    val a = test1("first")
    if (a != "first") return "fail1: $a"

    val b = test2("second", 31)
    if (b != "second31") return "fail2: $b"

    keep()

    return "OK"
}
