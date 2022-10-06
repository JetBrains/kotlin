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

fun mySum(a: Int, b: Int) = id(a) + id(b)

fun box(): String {
    id(42) // Unused value, the call should be eliminated
    assertEquals(0, callCounter)

    mySum(id(1), id(2))

    assertEquals(0, callCounter)
    return "OK"
}