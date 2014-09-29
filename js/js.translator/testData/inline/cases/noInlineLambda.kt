package foo

// CHECK_CALLED_IN_SCOPE: scope=multiplyBy2 function=multiplyBy2$f
// CHECK_NOT_CALLED_IN_SCOPE: scope=multiplyBy2 function=run

inline fun run<T>(noinline func: (T) -> T, arg: T): T {
    return func(arg)
}

fun multiplyBy2(x: Int): Int {
    return run({ it * 2 }, x)
}

fun box(): String {
    assertEquals(0, multiplyBy2(0))
    assertEquals(4, multiplyBy2(2))
    assertEquals(8, multiplyBy2(4))

    return "OK"
}