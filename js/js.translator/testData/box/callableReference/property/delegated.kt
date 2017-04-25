// EXPECTED_REACHABLE_NODES: 506
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

import kotlin.reflect.KProperty

object NumberDecrypter {
    operator fun getValue(instance: Any?, data: KProperty<*>) = when (data.name) {
        "four" -> 4
        "two" -> 2
        else -> throw Exception()
    }
}

val four: Int by NumberDecrypter

class A {
    val two: Int by NumberDecrypter
}

fun box(): String {
    val x = ::four.get()
    if (x != 4) return "Fail x: $x"
    val a = A()
    val y = A::two.get(a)
    if (y != 2) return "Fail y: $y"
    return "OK"
}
