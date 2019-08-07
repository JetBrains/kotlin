// EXPECTED_REACHABLE_NODES: 1283
// CHECK_VARS_COUNT: function=test count=1

// TODO: Support classes

fun test(): String {

    var i = 23

    var x = ++i
    if (x != 24) return "fail1:"

    i++
    if (i != 25) return "fail2:"

    // a.i++, used as expression, requires temporary variable

    return "OK"
}

fun box(): String = test()