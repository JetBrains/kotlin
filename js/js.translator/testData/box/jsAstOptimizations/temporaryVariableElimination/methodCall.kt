// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=init;foo;test1;test2;test3;test4;test5;test6;box expect=methodCall.optimized.js TARGET_BACKENDS=JS_IR_ES6

var log = ""


fun init() {
    log = ""
}

fun foo(n: Int): Int {
    log += "{$n}"
    return n
}

fun test1(): Int {
    init()

    val tmp1 = foo(1)
    val tmp2 = foo(2)

    return foo(tmp1 + tmp2)
}

fun test2(): Int {
    init()

    val tmp2 = foo(2)
    val tmp1 = foo(1)

    return foo(tmp1 + tmp2)
}

fun test3(): Int {
    init()

    val tmp1 = foo(1)
    val tmp2 = foo(2)
    val tmp3 = foo(3)

    return foo(tmp1 + tmp2 + foo(tmp3))
}

fun test4(): Int {
    init()

    val tmp1 = foo(1)
    val tmp2 = foo(2)
    val tmp3 = foo(3)

    return foo(foo(tmp1) + tmp2 + tmp3)
}

fun test5(): Int {
    init()

    val tmp1 = foo(1)
    val tmp2 = foo(2)
    val tmp3 = foo(3)
    foo(4)

    return tmp1 + tmp2 + tmp3
}

fun test6(): Int {
    init()

    val tmp = foo(1)
    return foo(2) + tmp
}

fun box(): String {
    var result = test1()
    if (result != 3) return "fail1a: $result"
    if (log != "{1}{2}{3}") return "fail1b: $log"

    result = test2()
    if (result != 3) return "fail2a: $result"
    if (log != "{2}{1}{3}") return "fail2b: $log"

    result = test3()
    if (result != 6) return "fail3a: $result"
    if (log != "{1}{2}{3}{3}{6}") return "fail3b: $log"

    result = test4()
    if (result != 6) return "fail4a: $result"
    if (log != "{1}{2}{3}{1}{6}") return "fail4b: $log"

    result = test5()
    if (result != 6) return "fail5a: $result"
    if (log != "{1}{2}{3}{4}") return "fail5b: $log"

    result = test6()
    if (result != 3) return "fail6a: $result"
    if (log != "{1}{2}") return "fail6b: $log"

    return "OK"
}
