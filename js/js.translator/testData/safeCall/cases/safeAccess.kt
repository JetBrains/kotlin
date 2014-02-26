package foo

class A() {
    var c = 3
}

fun box(): Boolean {
    var a1: A? = A()
    var a2: A? = null
    a1?.c = 4
    a2?.c = 5
    if (a1?.c != 4) {
        return false;
    }
    a2 = a1
    a2?.c = 5
    return (a2?.c == 5) && (a1?.c == 5)
}