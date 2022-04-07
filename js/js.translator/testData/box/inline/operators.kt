// EXPECTED_REACHABLE_NODES: 1301

// CHECK_FUNCTION_EXISTS: plus_za3lpa$ TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=plus_za3lpa$ scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: plus
// CHECK_NOT_CALLED_IN_SCOPE: function=plus scope=box

// CHECK_FUNCTION_EXISTS: invoke TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=invoke scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: get_za3lpa$ TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_za3lpa$ scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: set_vux9f0$ TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_vux9f0$ scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: dec TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=dec scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: minus_za3lpa$ TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=minus_za3lpa$ scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: invoke_dqglrj$ TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=invoke_dqglrj$ scope=test TARGET_BACKENDS=JS

class A {
    inline operator fun plus(a: Int) = a + 10
}

class B

inline operator fun B.plus(b: Int) = b + 20

object O {
    inline operator fun invoke() = 42

    inline operator fun Int.invoke(other: Int) = this + other

    fun test() = (1200)(34)
}

object R {
    inline operator fun get(i: Int) = 99 + i
}

object S {
    var lastResult = 0

    inline operator fun set(i: Int, value: Int) {
        lastResult = i + value
    }
}

class N(val value: Int) {
    inline operator fun minus(other: Int) = N(value - other)

    inline operator fun dec() = N(value - 1)
}

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box(): String {
    var result = A() + 1
    if (result != 11) return "fail: member operator: $result"

    result = B() + 2
    if (result != 22) return "fail: extension operator: $result"

    result = O()
    if (result != 42) return "fail: invoke operator: $result"

    result = O.test()
    if (result != 1234) return "fail: extension invoke operator: $result"

    result = R[1]
    if (result != 100) return "fail: get operator: $result"

    S[2] = 3
    result = S.lastResult
    if (result != 5) return "fail: set operator: $result"

    var n = N(10)
    n--
    if (n.value != 9) return "fail: decrement: ${n.value}"
    n -= 3
    if (n.value != 6) return "fail: augmented assignment: ${n.value}"

    return "OK"
}