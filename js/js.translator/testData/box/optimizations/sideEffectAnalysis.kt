@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.Effects
import kotlin.internal.Effect

var callCounter = 0

fun foo(f: () -> Int) = f()

@Effects(Effect.READONLY) // Trick the optimizer
fun id(x: Int): Int {
    callCounter += 1
    return x
}

fun effectful(x: Int): Int {
    callCounter += 1
    return x
}

// Effectively pure function
fun mySum(a: Int, b: Int) = id(a) + id(b)

fun box(): String {
    id(42) // Unused value, the call should be eliminated
    assertEquals(0, callCounter)

    mySum(id(1), id(2)) // Unused value, the call should be eliminated
    assertEquals(0, callCounter)

    // Call to mySum should be eliminated, but calls to effectful shouldn't be
    mySum(effectful(1), effectful(2))

    assertEquals(2, callCounter)
    return "OK"
}