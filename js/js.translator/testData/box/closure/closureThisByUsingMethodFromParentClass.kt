// EXPECTED_REACHABLE_NODES: 508
package foo

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

    val fs: String = eval("B\$far\$lambda").toString()
    val gs = (eval("B\$gar\$lambda").toString() as String).replaceAll("boo", "foo").replaceAll("gar", "far")

    assertEquals(gs, fs)

    return "OK"
}


// Helpers

inline fun String.replace(regexp: RegExp, replacement: String): String = asDynamic().replace(regexp, replacement)

fun String.replaceAll(regexp: String, replacement: String): String = replace(RegExp(regexp, "g"), replacement)

external class RegExp(regexp: String, flags: String)
