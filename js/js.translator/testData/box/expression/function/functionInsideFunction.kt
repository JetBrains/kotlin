// EXPECTED_REACHABLE_NODES: 491
package foo


fun box(): String {
    fun f() = 3

    if ((f() + f()) != 6) return "fail1"
    if (b() != 24) return "fail2"
    return "OK"
}


fun b(): Int {

    fun a(): Int {
        fun c() = 4
        return c() * 3
    }
    val a = 2
    return a() * a
}