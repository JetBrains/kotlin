// WITH_STDLIB

class A {
    fun a() {
        println("Hello, world!")
    }

    fun b(p: String) {
        println(p)
    }

    private var bla: String = "123"
    fun c(p: String) {
        println(p + bla)
    }

//    context(v: String)
//    fun c(p: String) {
//        println(c + p)
//    }
}

fun box(): String {
    assertTrue(A::a == A::a)
    assertTrue(A::b == A::b)
    assertTrue(A::c == A::c)

    return "OK"
}