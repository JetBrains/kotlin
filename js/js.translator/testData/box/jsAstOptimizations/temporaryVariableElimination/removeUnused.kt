// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=test1;test2;test3;box expect=removeUnused.optimized.js TARGET_BACKENDS=JS_IR_ES6

var global = 0

fun test1(): Int {
    val tmp = global++
    return global
}

fun test2(): Int {
    val tmp: Int
    tmp = global++
    return global
}

fun test3(): Int {
    val a = global++
    val b = global--
    return b + b
}

fun box(): String {
    var result = test1()
    if (result != 1) return "fail1: $result"

    result = test2()
    if (result != 2) return "fail2: $result"

    result = test3()
    if (result != 6) return "fail3: $result"

    return "OK"
}
