package foo

var global: String = ""

fun bar(s: String, value: Int): Int {
    global += s
    return value
}

fun baz(vararg args: Int): String {
    return "baz: ${args.size}"
}

fun box(): String {
    baz(bar("A", 10), try { global += "B"; 20} finally {})
    assertEquals("AB", global)

    global = ""
    baz(bar("A", 10), 30, if (true) { while(false){}; global+= "B"; 20 } else { 50 })
    assertEquals("AB", global)

    return "OK"
}