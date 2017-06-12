// EXPECTED_REACHABLE_NODES: 495
package foo

// CHECK_VARS_COUNT: function=test1 count=0
// CHECK_VARS_COUNT: function=test2 count=1
// CHECK_VARS_COUNT: function=test3 count=0

inline fun a(x: Int) = b(x)

fun b(x: Int) = x

fun test1(n: Int) = if (n > 0) a(n + 10) else a(n - 10)

fun test2(n: Int): Int {
    var result = if (n > 0) a(n + 10) else a(n - 10)
    return result
}

fun test3(n: Int): Int {
    Holder.value = if (n > 0) a(n + 10) else a(n - 10)
    return Holder.value
}

object Holder {
    var value = 0
}

fun box(): String {
    var result: Int

    result = test1(5)
    if (result != 15) return "fail1a: $result"
    result = test1(-5)
    if (result != -15) return "fail1b: $result"

    result = test2(5)
    if (result != 15) return "fail2a: $result"
    result = test2(-5)
    if (result != -15) return "fail2b: $result"

    result = test3(5)
    if (result != 15) return "fail3a: $result"
    result = test3(-5)
    if (result != -15) return "fail3b: $result"

    return "OK"
}