// EXPECTED_REACHABLE_NODES: 1285
package foo

// CHECK_NOT_CALLED: inc
// CHECK_NOT_CALLED: run

class Countable {
    var count = 0
}

inline fun inc(countable: Countable) = countable.count++

inline fun run(func: () -> Unit) = func()

// CHECK_BREAKS_COUNT: function=incNoInline count=0
// CHECK_LABELS_COUNT: function=incNoInline name=$l$block count=0
fun incNoInline(countable: Countable) = run { inc(countable) }

fun box(): String {
    val c = Countable()

    incNoInline(c)
    assertEquals(1, c.count)

    return "OK"
}