// EXPECTED_REACHABLE_NODES: 501
package foo

interface Named {
    companion object Bar {
        val g = "a";
    }
}

class Foo {
    companion object {
        val g = "b";
    }
}

fun box(): String {
    assertEquals("a", Named.Bar.g, "Named.Bar.g")
    assertEquals("a", Named.g, "Named.g")

    assertEquals("b", Foo.Companion.g, "Foo.Companion.g")
    assertEquals("b", Foo.g, "Foo.g")

    assertEquals("b", foo(Foo), "foo(Foo)")
    assertEquals("b", foo(Foo.Companion), "foo(Foo.Companion)")

    assertEquals("c", Named.ext(), "Named.ext()")
    assertEquals("c", Named.Bar.ext(), "Named.Bar.ext()")

    return "OK"
}

fun foo(f: Foo.Companion) = f.g

fun Named.Bar.ext() = "c"