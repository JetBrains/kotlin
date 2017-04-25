// EXPECTED_REACHABLE_NODES: 498
// KT-4218 Nested function literal on singleton object fails

package foo

object SomeObject {
    val values = create()
    fun create() = Array<Array<String>>(1) { y ->
        Array<String>(1) { x ->
            "(${x}, ${y})"
        }
    }
}

fun box(): String {
    if (SomeObject.values[0][0] != "(0, 0)") return SomeObject.values[0][0]

    return "OK"
}