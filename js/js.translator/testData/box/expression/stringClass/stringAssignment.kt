// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {

    val a = "bar";
    var b = "foo";
    b = a;
    return if (b == "bar") "OK" else "fail"
}

