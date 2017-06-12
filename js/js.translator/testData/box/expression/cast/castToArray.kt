// EXPECTED_REACHABLE_NODES: 496
package foo

fun box(): String {
    val ints: Any? = arrayOf(1, 2)
    val strings: Any? = arrayOf("a", "b")
    val nil: Any? = null
    val obj: Any? = object{}

    success("ints") { ints as Array<*> }
    success("strings") { strings as Array<*> }
    failsClassCast("null") { nil as Array<*> }
    failsClassCast("obj") { obj as Array<*> }

    return "OK"
}