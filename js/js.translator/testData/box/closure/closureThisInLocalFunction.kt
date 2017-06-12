// EXPECTED_REACHABLE_NODES: 495
package foo

class Foo {
    val o = "O"
    val k = "K"
    fun test(): String {
        fun bar() = o
        fun Int.baz() = k + this

        val boo = { k }
        val cux: Int.()->String = { o + this }

        return bar() + 17.baz() + 23.cux() + boo()
    }
}

fun box(): String {
    val a = Foo().test()
    if (a != "OK17O23K") return "$a"

    return "OK"
}
