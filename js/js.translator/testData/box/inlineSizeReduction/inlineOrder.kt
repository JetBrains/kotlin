// EXPECTED_REACHABLE_NODES: 499
package foo

// CHECK_VARS_COUNT: function=test1 count=0
// CHECK_VARS_COUNT: function=test2 count=0
// CHECK_VARS_COUNT: function=test3 count=0
// CHECK_VARS_COUNT: function=test4 count=0
// CHECK_VARS_COUNT: function=test5 count=2

var global = ""
var globalNum = 1
var returnValue = 0

fun pure(n: Int) = n

fun x(n: Int) = globalNum++ * n

inline fun a(n: Int) = x(n)

fun b(n: Int) {
    returnValue = n
}

fun c(first: Int, second: Int, third: Int) = first + second + third

inline fun d(first: Int, second: Int, third: Int) {
    returnValue = first + second + third
}

fun test1(): Int {
    globalNum = 1
    return x(a(1) + a(10) + a(100))
}

fun test2() {
    globalNum = 1
    b(a(1) + a(10) + a(100))
}

fun test3(): Int {
    globalNum = 1
    return c(a(1), a(10), a(100))
}

fun test4() {
    globalNum = 1
    d(a(1), a(10), a(100))
}

fun test5(): Int {
    globalNum = 1
    return globalNum++ + a(10) + (globalNum++ * 100)
}

fun box(): String {
    var result = test1()
    if (result != 1284) return "fail1: $result"

    test2()
    result = returnValue
    if (result != 321) return "fail2: $result"

    result = test3()
    if (result != 321) return "fail3: $result"

    test4()
    result = returnValue
    if (result != 321) return "fail4: $result"

    result = test5()
    if (result != 321) return "fail5: $result"

    return "OK"
}