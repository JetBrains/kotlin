// EXPECTED_REACHABLE_NODES: 497
// CHECK_VARS_COUNT: function=test1 count=0
// CHECK_VARS_COUNT: function=test2 count=1
// CHECK_VARS_COUNT: function=test3 count=0

class A {
    var result = 1

    inline var z: Int
        get() = result
        set(value) {
            result = value
        }
}

val a = A()

fun test1(): Int {
    a.z += 1
    return a.z
}

fun test2(): Int {
    return a.z++
}

fun test3(): Int {
    return ++a.z
}

fun box(): String {
    if (test1() != 2) return "fail 1: ${a.z}"

    var p = test2()
    if (a.z != 3) return "fail 2: ${a.z}"
    if (p != 2) return "fail 3: $p"

    p = test3()
    if (a.z != 4) return "fail 4: ${a.z}"
    if (p != 4) return "fail 5: $p"

    return "OK"
}