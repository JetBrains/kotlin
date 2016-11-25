package foo

// HACKS

external const val PATH_TO_F_CREATOR = "B\$far\$lambda"
external const val PATH_TO_G_CREATOR = "B\$gar\$lambda"

@JsName("$PATH_TO_F_CREATOR")
external val F_CREATOR: Any = noImpl

@JsName("$PATH_TO_G_CREATOR")
external val G_CREATOR: Any = noImpl


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

external fun String.replace(regexp: RegExp, replacement: String): String = noImpl

fun String.replaceAll(regexp: String, replacement: String): String = replace(RegExp(regexp, "g"), replacement)

external class RegExp(regexp: String, flags: String)
