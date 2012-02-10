package foo

class A() {
    var c = 3
}

fun box() {
    var a1 : A? = A()
    var a2 : A? = null
    a1?.c++
    a2?.c++
    a2 = a1
    a2?.c++
    return (a2?.c == 5)
}