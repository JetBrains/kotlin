// EXPECTED_REACHABLE_NODES: 1315

open class Ex0(msg: String, cs: Throwable): Throwable(msg, cs)

class Ex1: Ex0("A", Error("fail2")) {
    override val cause = Error("B")
}

class Ex2: Ex0("fail3", Error("C")) {
    override val message = "D"
}

fun box(): String {
    val ex1: Throwable = Ex1()
    val ex2: Throwable = Ex2()

    val r = ex1.message + ex1.cause?.message + ex2.cause?.message + ex2.message
    if (r != "ABCD") return "Fail: $r"

    return "OK"
}