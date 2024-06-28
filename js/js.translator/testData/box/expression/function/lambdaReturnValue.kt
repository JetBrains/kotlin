// EXPECTED_REACHABLE_NODES: 1285

fun <T> rawReturnValue(fn: () -> T): Any {
    return fn() as Any
}

fun unitFun() {}

fun charFun(): Char = 'a'

value class VC(val v: Int)

fun vcFun(): VC = VC(1)



fun box(): String {
    if (rawReturnValue { unitFun() } != Unit) return "fail1.1"
    if (rawReturnValue<Unit> { unitFun() } != Unit) return "fail1.2"
    if (rawReturnValue<Any> { unitFun() } != Unit) return "fail1.3"

    val boxedA: Any = 'a'

    if (rawReturnValue { charFun() } != boxedA) return "fail2.1"
    if (rawReturnValue<Char> { charFun() } != boxedA) return "fail2.2"
    if (rawReturnValue<Any> { charFun() } != boxedA) return "fail2.3"

    val boxed1: Any = VC(1)

    if (rawReturnValue { vcFun() } != boxed1) return "fail3.1"
    if (rawReturnValue<VC> { vcFun() } != boxed1) return "fail3.2"
    if (rawReturnValue<Any> { vcFun() } != boxed1) return "fail3.3"

    return "OK"
}