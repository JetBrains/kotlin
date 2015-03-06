package foo

trait Named {
    default object Bar {
        val g = "a";
    }
}

class Foo {
    default object {
        val g = "b";
    }
}

fun box(): String {
    assertEquals("a", Named.Bar.g, "Named.Bar.g")
    assertEquals("a", Named.g, "Named.g")

    assertEquals("b", Foo.Default.g, "Foo.Default.g")
    assertEquals("b", Foo.g, "Foo.g")

    assertEquals("b", foo(Foo), "foo(Foo)")
    assertEquals("b", foo(Foo.Default), "foo(Foo.Default)")

    assertEquals("c", Named.ext(), "Named.ext()")
    assertEquals("c", Named.Bar.ext(), "Named.Bar.ext()")

    return "OK"
}

fun foo(f: Foo.Default) = f.g

fun Named.Bar.ext() = "c"