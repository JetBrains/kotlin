import kotlin.test.*

class A(val s: String)

class B {
    var a: A? = null
}

class C(val b: B)

fun foo(c: C) {
    c.b.a = A("zzz")
}

fun bar(b: B) {
    val c = C(b)
    foo(c)
}

@ThreadLocal
val global = B()

@Test fun runTest() {
    bar(global)
    assertEquals("zzz", global.a!!.s)
}