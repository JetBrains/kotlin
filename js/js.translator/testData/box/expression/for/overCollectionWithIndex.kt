// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1652
// CHECK_NOT_CALLED_IN_SCOPE: function=component1 scope=test1
// CHECK_NOT_CALLED_IN_SCOPE: function=component1 scope=test2
// CHECK_NOT_CALLED_IN_SCOPE: function=component1 scope=test3
// CHECK_CONTAINS_NO_CALLS: test4 except=toString

fun test1(a: Sequence<String>): String {
    var s = ""
    for ((i, x) in a.withIndex()) {
        s += "$i:$x;"
    }
    return s
}

fun test2(a: Collection<String>): String {
    var s = ""
    for ((_, x) in a.withIndex()) {
        s += "$x;"
    }
    return s
}

fun test3(a: List<String>): String {
    var s = ""
    for ((i, _) in a.withIndex()) {
        s += "$i;"
    }
    return s
}

fun test4(a: Set<String>): String {
    var s = ""
    for (i in a.indices) {
        s += "$i;"
    }
    return s
}

fun box(): String {
    val list = listOf("foo", "bar", "baz")

    var r = test1(list.asSequence())
    if (r != "0:foo;1:bar;2:baz;") return "fail1: $r"

    r = test2(list)
    if (r != "foo;bar;baz;") return "fail2: $r"

    r = test3(list)
    if (r != "0;1;2;") return "fail3: $r"

    r = test4(list.toSet())
    if (r != "0;1;2;") return "fail4: $r"

    return "OK"
}