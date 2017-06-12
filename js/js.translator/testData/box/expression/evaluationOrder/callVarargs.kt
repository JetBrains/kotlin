// EXPECTED_REACHABLE_NODES: 495
package foo

var global: String = ""

fun <T> bar(s: String, value: T): T {
    global += s
    return value
}

fun <T> baz(vararg args: T): String {
    return "baz: ${args.size}"
}

fun <T> idVarArg(vararg a: T) = a

fun box(): String {
    baz(bar("A", 10), try { global += "B"; 20} finally {})
    assertEquals("AB", global)

    global = ""
    baz(bar("A", 10), 30, if (true) { while(false){}; global+= "B"; 20 } else { 50 })
    assertEquals("AB", global)

    global = ""
    assertEquals("baz: 4", baz(bar("A", 1), *try {bar("B", arrayOf(2, 3))} catch(e: Exception) { bar("C", arrayOf(4, 5))}, bar("D", 6)))
    assertEquals("ABD", global)

    return "OK"
}