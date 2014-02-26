package foo

fun A.create(init: A.() -> Unit): A {
    init()
    return this
}

fun box(): Boolean {
    val a = A().create {
        c = 1 + t
    }
    return a.c == 4
}

class A() {
    val t = 3
    var c = 2
}