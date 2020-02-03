inline class IC(val value: Int)

fun foo(): Int {
    val x = IC(10)
    return x.value
}

fun foo3() {
    val x: Any = IC(10)
    val y: IC = x as IC
}

fun foo1(x: IC): IC = x

fun foo2(x: Any) = x

fun box(): String {
    foo3()

    val x: Any = IC(10)
    val y: IC = x as IC

    if (foo() != 10) return "Fail1"

    if (foo1(IC(20)).value != 20) return "Fail2"

    if ((foo2(IC(30)) as IC).value != 30) return "Fail3"

    return "OK"
}