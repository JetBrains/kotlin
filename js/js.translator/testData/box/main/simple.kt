// EXPECTED_REACHABLE_NODES: 1281
// IGNORE_BACKEND: JS_IR
// CALL_MAIN

var ok: String = "fail"

fun main(args: Array<String>) {
    assertEquals(1, args.size)
    assertEquals("testArg", args[0])

    ok = "OK"
}

fun box() = ok