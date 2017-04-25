// EXPECTED_REACHABLE_NODES: 887
package foo


fun test(f: () -> String): String {
    val funLit = { f() }
    return funLit()
}


fun box(): String {
    val l = ArrayList<String>()
    l.add("1 ")
    l.add("foobar ")
    l.add("baz")

    val f = {
        var s = ""
        for (e in l) s += e
        s
    }

    val r = f()
    if (r != "1 foobar baz") return "$r"

    return "OK"
}