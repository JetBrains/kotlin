package foo

var global: String = ""

fun bar<T>(s: String, value: T): T {
    global += s
    return value
}

fun baz<T>(vararg args: T): String {
    return "baz: ${args.size}"
}

fun idVarArg<T>(vararg a: T) = a

fun box(): String {
    baz(bar("A", 10), try { global += "B"; 20} finally {})
    assertEquals("AB", global)

    global = ""
    baz(bar("A", 10), 30, if (true) { while(false){}; global+= "B"; 20 } else { 50 })
    assertEquals("AB", global)

    global = ""
    assertEquals("baz: 4", baz(bar("A", 1), *try {bar("B", array(2, 3))} catch(e: Exception) { bar("C", array(4, 5))}, bar("D", 6)))
    assertEquals("ABD", global)

    return "OK"
}