package foo

object Obj

fun box(): String {
    val r: Any = Obj

// TODO: fix KT-13465
//    if (r !is Obj) return "r !is Obj"

    return "OK"
}
