package foo

// HACKS

@native
const val PATH_TO_F_CREATOR = "B\$far\$lambda"
@native
const val PATH_TO_G_CREATOR = "B\$gar\$lambda"

@native("$PATH_TO_F_CREATOR")
val F_CREATOR: Any = noImpl
@native("$PATH_TO_G_CREATOR")
val G_CREATOR: Any = noImpl


// Test

open class A {
    fun foo() = "A::foo"
}

class B : A() {
    fun boo() = "B::boo"

    val far = { foo() }
    val gar = { boo() }
}


fun box(): String {
    val b = B()
    val f = b.far
    val g = b.gar

    assertEquals("A::foo", f())
    assertEquals("B::boo", g())

    val fs = F_CREATOR.toString()
    val gs = G_CREATOR.toString().replaceAll("boo", "foo").replaceAll("gar", "far")

    assertEquals(gs, fs)

    return "OK"
}


// Helpers

@native
fun String.replace(regexp: RegExp, replacement: String): String = noImpl

fun String.replaceAll(regexp: String, replacement: String): String = replace(RegExp(regexp, "g"), replacement)

@native
class RegExp(regexp: String, flags: String)
