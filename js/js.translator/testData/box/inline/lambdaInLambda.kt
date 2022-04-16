// EXPECTED_REACHABLE_NODES: 1284
package foo

// CHECK_FUNCTION_EXISTS: multiplyBy2$lambda
// CHECK_CALLED_IN_SCOPE: scope=multiplyBy2 function=multiplyBy2$lambda TARGET_BACKENDS=JS
// HAS_NO_CAPTURED_VARS: function=multiplyBy2 except=multiplyBy2$lambda
// CHECK_NOT_CALLED_IN_SCOPE: scope=multiplyBy2 function=multiplyBy2$lambda_0
// CHECK_NOT_CALLED_IN_SCOPE: scope=multiplyBy2 function=run

internal inline fun <T> runLambdaInLambda(noinline inner: (T) -> T, outer: ((T) -> T, T) -> T, arg: T): T {
    return outer(inner, arg)
}

// CHECK_BREAKS_COUNT: function=multiplyBy2 count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=multiplyBy2 name=$l$block count=0 TARGET_BACKENDS=JS_IR
internal fun multiplyBy2(x: Int): Int {
    return runLambdaInLambda({ it * 2  }, { f, x -> f(x) }, x)
}

fun box(): String {
    assertEquals(0, multiplyBy2(0))
    assertEquals(4, multiplyBy2(2))
    assertEquals(8, multiplyBy2(4))

    return "OK"
}
