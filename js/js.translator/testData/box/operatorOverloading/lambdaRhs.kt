// EXPECTED_REACHABLE_NODES: 896
package foo

var global = ""

fun log(message: String) {
    global += message
}

fun test1(): String {
    val list = mutableListOf<(Int) -> Unit>()

    var result = ""
    list += { log("<$it>") }
    list += { result += "($it)" }
    list += { result += "[$it]"; result += "{$it}" }

    for(f in list) f(1)

    return result
}

fun box(): String {
    assertEquals("(1)[1]{1}", test1())
    assertEquals(global, "<1>")

    return "OK"
}