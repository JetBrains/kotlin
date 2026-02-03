// WITH_STDLIB
// LANGUAGE: +ContextParameters

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

    suspend fun d(p: String) {
        println(p)
    }
}

val funcSet = setOf(A::a, A::b, A::c, A::d)

fun box(): String {
    assertTrue(A::a == A::a)
    assertTrue(A::b == A::b)
    assertTrue(A::c == A::c)
    assertTrue(A::d == A::d)
    assertTrue(A::a in funcSet)

    return "OK"
}