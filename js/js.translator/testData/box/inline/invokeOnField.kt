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

// CHECK_BREAKS_COUNT: function=testDispatch count=0
// CHECK_LABELS_COUNT: function=testDispatch name=$l$block count=0
fun testDispatch(foo: Foo): String {
    foo.bar { return "O" }
    return "FailDispatch;"
}

// CHECK_BREAKS_COUNT: function=testExtension count=0
// CHECK_LABELS_COUNT: function=testExtension name=$l$block count=0
fun testExtension(foo: Foo): String {
    foo.baz { return "K" }
    return "FailExtension;"
}

fun box(): String {
    val f = Foo()
    return testDispatch(f) + testExtension(f)
}