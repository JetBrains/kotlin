// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1284
// CHECK_CONTAINS_NO_CALLS: test1 except=toString
// CHECK_CONTAINS_NO_CALLS: test2 except=toString
// CHECK_CONTAINS_NO_CALLS: test3 except=toString
// CHECK_CONTAINS_NO_CALLS: test4 except=toString

fun test1(a: Array<String>): String {
    var s = ""
    for ((i, x) in a.withIndex()) {
        s += "$i:$x;"
    }
    return s
}

fun test2(a: Array<String>): String {
    var s = ""
    for ((_, x) in a.withIndex()) {
        s += "$x;"
    }
    return s
}

fun test3(a: Array<String>): String {
    var s = ""
    for ((i, _) in a.withIndex()) {
        s += "$i;"
    }
    return s
}

fun test4(a: Array<String>): String {
    var s = ""
    for (i in a.indices) {
        s += "$i;"
    }
    return s
}

fun box(): String {
    val array = arrayOf("foo", "bar", "baz")

    var r = test1(array)
    if (r != "0:foo;1:bar;2:baz;") return "fail1: $r"

    r = test2(array)
    if (r != "foo;bar;baz;") return "fail2: $r"

    r = test3(array)
    if (r != "0;1;2;") return "fail3: $r"

    r = test4(array)
    if (r != "0;1;2;") return "fail4: $r"

    return "OK"
}