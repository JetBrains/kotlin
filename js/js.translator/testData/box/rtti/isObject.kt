// EXPECTED_REACHABLE_NODES: 994
package foo

object Obj

fun box(): String {
    val r: Any = Obj

    if (r !is Obj) return "r !is Obj"

    return "OK"
}
