// EXPECTED_REACHABLE_NODES: 500
package foo

fun Any.foo1(): () -> String {
    return { "239" + this }
}

fun Int.foo2(): (i: Int) -> Int {
    return { x -> x + this }
}

fun <T> fooT1(t: T) = { t.toString() }

fun <T> fooT2(t: T) = { x: T -> t.toString() + x.toString() }

fun box(): Any? {
    if ( (10.foo1())() != "23910") return "foo1 fail"
    if ( (10.foo2())(1) != 11 ) return "foo2 fail"

    if (1.(fun Int.(): Int = this + 1)() != 2) return "test 3 failed";
    if (  { 1 }() != 1) return "test 4 failed";
    if (  { x: Int -> x }(1) != 1) return "test 5 failed";
    if (  1.(fun Int.(x: Int): Int = x + this)(1) != 2) return "test 6 failed";
    val tmp = 1.(fun Int.(): Int = this)()
    if (+tmp != 1) return "test 7 failed, res: $tmp ${tmp is Int}";
    if (  (fooT1<String>("mama"))() != "mama") return "test 8 failed";
    if (  (fooT2<String>("mama"))("papa") != "mamapapa") return "test 9 failed";
    return "OK"
}
