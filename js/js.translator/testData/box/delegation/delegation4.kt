// EXPECTED_REACHABLE_NODES: 508
// This test was adapted from compiler/testData/codegen/box/classes
package foo

interface First {
    public open fun foo(): Int
}

interface Second : First {
    public open fun bar(): Int
}

class Impl : Second {
    public override fun foo() = 1
    public override fun bar() = 2
}

class Test(s: Second) : Second by s {}

fun box(): String {
    var t = Test(Impl())
    if (t.foo() != 1)
        return "Fail #1"
    if (t.bar() != 2)
        return "Fail #2"
    if (t !is First)
        return "Fail #3"
    if (t !is Second)
        return "Fail #4"

    return "OK"
}
