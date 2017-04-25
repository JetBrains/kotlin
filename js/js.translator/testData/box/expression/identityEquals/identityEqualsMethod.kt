// EXPECTED_REACHABLE_NODES: 489
package foo

class X

fun box(): String {
    val a = X()
    val b = X()
    if (a !== a) return "a !== a"
    if (a === b) return "X() === X()"
    val c = a
    if (c !== a) return "c = a; c !== a"

    if (X() === a) return "X() === a"

    val t = !(X() === a)
    if (!t) return "t = !(X() === a); t == false"

    val f = !!(X() === a)
    if (f) return "f = !!(X() === null); f == true"
    return "OK";
}