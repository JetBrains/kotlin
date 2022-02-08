// EXPECTED_REACHABLE_NODES: 1375
// CHECK_CONTAINS_NO_CALLS: testDispatch except=Unit_getInstance
// CHECK_CONTAINS_NO_CALLS: testExtension except=Unit_getInstance
class Bar {
    inline operator fun invoke(f: () -> String) { f() }
}

class Baz
inline operator fun Baz.invoke(f: () -> String) { f() }

class Foo {
    val bar = Bar()
    val baz = Baz()
}

fun testDispatch(foo: Foo): String {
    foo.bar { return "O" }
    return "FailDispatch;"
}

fun testExtension(foo: Foo): String {
    foo.baz { return "K" }
    return "FailExtension;"
}

fun box(): String {
    val f = Foo()
    return testDispatch(f) + testExtension(f)
}