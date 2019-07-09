// EXPECTED_REACHABLE_NODES: 1281
// CALL_MAIN

var ok: String = "fail"

fun main(args: Array<String>) {
    assertEquals(1, args.size)
    assertEquals("testArg", args[0])

    ok = "OK"
}

fun box() = ok