// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/local/.
package foo

var state = 23

fun box(): String {
    fun incrementState(inc: Int) {
        state += inc
    }

    val inc = ::incrementState
    inc(12)
    inc(-5)
    inc(27)
    inc(-15)

    return if (state == 42) "OK" else "Fail $state"
}
