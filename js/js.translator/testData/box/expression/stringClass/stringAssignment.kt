// EXPECTED_REACHABLE_NODES: 990
package foo

fun box(): String {

    val a = "bar";
    var b = "foo";
    b = a;
    return if (b == "bar") "OK" else "fail"
}

