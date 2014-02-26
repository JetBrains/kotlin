package foo

class X

fun box(): String {
    val a = X()
    val b = X()
    if (!a.identityEquals(a)) return "a !== a"
    if (a.identityEquals(b)) return "X() === X()"
    val c = a
    if (!c.identityEquals(a)) return "c = a; c !== a"

    if (X() identityEquals a) return "X() identityEquals a"

    val t = !(X() identityEquals a)
    if (!t) return "t = !(X() identityEquals a); t == false"

    val f = !!(X() identityEquals a)
    if (f) return "f = !!(X() identityEquals null); f == true"
    return "OK";
}