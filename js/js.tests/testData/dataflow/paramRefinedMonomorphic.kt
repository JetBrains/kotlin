// Precision: monomorphic call-site refines private callee parameters.

interface Base
class Child : Base

private fun take(p: Base, n: Int): String {
    return if (p is Child && n == 7) "OK" else "FAIL"
}

fun box(): String = take(Child(), 7)
