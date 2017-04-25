// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_CALLED_IN_SCOPE: scope=multiplyBy2 function=multiplyBy2$lambda
// CHECK_NOT_CALLED_IN_SCOPE: scope=multiplyBy2 function=multiplyBy2$lambda_0
// CHECK_NOT_CALLED_IN_SCOPE: scope=multiplyBy2 function=run

internal inline fun <T> runLambdaInLambda(noinline inner: (T) -> T, outer: ((T) -> T, T) -> T, arg: T): T {
    return outer(inner, arg)
}

internal fun multiplyBy2(x: Int): Int {
    return runLambdaInLambda({ it * 2  }, { f, x -> f(x) }, x)
}

fun box(): String {
    assertEquals(0, multiplyBy2(0))
    assertEquals(4, multiplyBy2(2))
    assertEquals(8, multiplyBy2(4))

    return "OK"
}