package foo

// HACKS

native
val ROOT = "Kotlin.modules.JS_TESTS"
native
val PATH_TO_F_CREATOR = "foo.B.B\$f"
native
val PATH_TO_G_CREATOR = "foo.B.B\$f_0"

native("$ROOT.$PATH_TO_F_CREATOR")
val F_CREATOR: Any = noImpl
native("$ROOT.$PATH_TO_G_CREATOR")
val G_CREATOR: Any = noImpl


// Test

open class A {
    fun foo() = "A::foo"
}

class B : A() {
    fun boo() = "B::boo"

    val f = { foo() }
    val g = { boo() }
}


fun box(): String {
    val b = B()
    val f = b.f
    val g = b.g

    assertEquals("A::foo", f())
    assertEquals("B::boo", g())

    val fs = F_CREATOR.toString()
    val gs = G_CREATOR.toString().replaceAll("boo", "foo")

    assertEquals(gs, fs)

    return "OK"
}


// Helpers

native
fun String.replace(regexp: RegExp, replacement: String): String = noImpl

fun String.replaceAll(regexp: String, replacement: String): String = replace(RegExp(regexp, "g"), replacement)

native
class RegExp(regexp: String, flags: String)