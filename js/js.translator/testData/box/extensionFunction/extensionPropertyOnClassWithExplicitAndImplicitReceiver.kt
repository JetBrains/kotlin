// EXPECTED_REACHABLE_NODES: 493
package foo

class Foo {
    fun blah(value: Int): Int {
        return value + 1
    }
}

val Foo.fooImp: Int
    get() {
        return blah(5)
    }

val Foo.fooExp: Int
    get() {
        return this.blah(10)
    }

fun box(): String {
    var a = Foo()
    if (a.fooImp != 6) return "fail1: ${a.fooImp}"
    if (a.fooExp != 11) return "fail2: ${a.fooExp}"
    return "OK";
}
