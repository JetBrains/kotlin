// EXPECTED_REACHABLE_NODES: 1397
// MODULE: lib
// FILE: lib.kt
open class C {
    internal inline fun a(noinline x: () -> String = { "a" }) = x()

    internal inline fun b(): () -> String = { "b" }

    inline fun c(noinline x: () -> String = { "c" }) = x()

    inline fun d(): () -> String = { "d" }
}

internal inline fun test1(): String {
    val x = C()
    return x.a() + x.b()() + x.c() + x.d()()
}

internal fun callTest1(): String = test1()

// MODULE: main()(lib)
// FILE: lib.kt
internal inline fun test2(): String {
    val x = C()
    return "2" + x.a() + x.b()() + x.c() + x.d()()
}

fun box(): String {
    val r1 = test1()
    if (r1 != "abcd") return "fail1: $r1"

    val rc1 = callTest1()
    if (rc1 != "abcd") return "fail1c: $rc1"

    val r2 = test2()
    if (r2 != "2abcd") return "fail2: $r2"

    return "OK"
}
