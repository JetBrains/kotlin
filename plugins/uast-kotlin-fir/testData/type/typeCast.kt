import java.lang.Runnable

fun stringConsumer(s: String) {
}

fun foo(x: Any, isSafe: Boolean) =
    if (isSafe) x as Runnable else x as? Runnable

fun box(): String {
    // Unit as Any
    val x = stringConsumer("Hi") as Any
    if (x != Unit) return "Fail: $x"

    // Unit as? Any
    val y = stringConsumer("Hi, again") as Any
    if (y != Unit) return "Fail: $y"

    val r = object : Runnable {
        override fun run() {}
    }
    if (foo(r, true) !=== r) return "Fail: $r"
    if (foo(r, false) !=== r) return "Fail: $r"

    return "OK"
}
