// EXPECTED_REACHABLE_NODES: 996
package foo

class A {
    val x: String

    constructor() {
    }

    init {
        val o = "O"
        fun baz() = o + "K"
        x = baz()
    }
}

fun box() = A().x