// EXPECTED_REACHABLE_NODES: 1281
/*
Modified test case from KT-24777
 */
package foo

inline fun condition() = false

// CHECK_BREAKS_COUNT: function=run count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=run name=$block count=0 TARGET_BACKENDS=JS_IR
fun run(x: Boolean): String {
    var y = 0
    do {
        if (y > 0)
            return "NOT OK"
        y += 1
        do {
        } while (false)
        if (x) continue
    } while (condition())
    return "OK"
}

fun box(): String {
    return run(true)
}
