// EXPECTED_REACHABLE_NODES: 1286
package foo

inline fun run(func: () -> Int): Int {
    return func()
}

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun bar(): Int {
    var f = { -> 0 }
    var get0 = f

    f = { -> 1 }
    var get1 = f

    var get2 = get1
    f = { -> 2 }
    get2 = f

    return run(get0) + run(get1) + run(get2)
}

fun box(): String {
    assertEquals(3, bar())

    return "OK"
}