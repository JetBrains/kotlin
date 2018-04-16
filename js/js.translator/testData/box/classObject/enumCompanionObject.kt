// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1141
// See KT-6326, KT-6777
package foo

enum class Foo {
    A;

    companion object {
        val a = A
    }
}

fun box(): String {
    assertEquals("A", Foo.a.name)
    return "OK"
}