// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283

fun box(): String {
    var x: String? = foo()
    var r = x + bar()
    if (r != "foobar") return "fail1: $r"

    x = null
    r = x + bar()
    if (r != "nullbar") return "fail2: $r"

    x = foo()
    r = x + null
    if (r != "foonull") return "fail3: $r"

    x = foo()
    r = x + nullString()
    if (r != "foonull") return "fail4: $r"

    r = foo()
    r += bar()
    if (r != "foobar") return "fail5: $r"

    x = null
    r = x + null
    if (r != "nullnull") return "fail6: $r"

    x = foo()
    x += nullString()
    if (x != "foonull") return "fail7: $r"

    x = nullString()
    x += bar()
    if (x != "nullbar") return "fail8: $r"

    x = nullString()
    r = x + nullString()
    if (r != "nullnull") return "fail9: $r"

    x = nullString()
    x += nullString()
    if (x != "nullnull") return "fail10: $x"

    return "OK"
}

fun foo() = "foo"

fun bar() = "bar"

fun nullString(): String? = null