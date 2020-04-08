// WITH_RUNTIME
// SUGGESTED_NAMES: i, getKm
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: public final val meters: kotlin.Int? defined in A
class A {
    val meters: Int? = 1
}

fun test() {
    val a = A()
    with (a) {
        if (meters == null) return
        val km = <selection>meters / 10</selection>
    }
}