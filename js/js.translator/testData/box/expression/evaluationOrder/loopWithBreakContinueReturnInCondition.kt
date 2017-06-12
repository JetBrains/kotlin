// EXPECTED_REACHABLE_NODES: 498
package foo

var global: String = ""


fun whileReturn(): String {
    var i = 0
    while(if (i<2) true else { global += ":return:"; return ":whileReturn:"}) {
        i++
        global += "A"
    }
    return ":whileReturn:after:"
}

fun whileImmediateReturn(): String {
    var i = 0
    while(return ":whileImmediateReturn:") {
        global += "A"
    }
    return ":whileImmediateReturn:after"
}

fun doWhileReturn(): String {
    var i = 0
    do {
        i++
        global += "A"
    }
    while(if (i<2) true else { global += ":return:"; return ":doWhileReturn:"})
    return ":doWhileReturn:after:"
}

fun doWhileReturnFromCondition(): String {
    var i = 0
    do {
        global += "A"
    } while(return ":doWhileReturnFromCondition:")
    return ":doWhileReturnFromCondition:after"
}

fun doWhileImmediateReturn(): String {
    var i = 0
    do {
        return ":doWhileImmediateReturn:"
        global += "A"
    } while(false)
    return ":doWhileImmediateReturn:after"
}

fun forReturn(b: Boolean): String {
    var i = 0
    for(i in (if (b) 1..2 else { global += ":return:"; return ":forReturn:"})) {
        global += "A"
    }
    return ":forReturn:after:"
}

fun box(): String {
    assertEquals(":whileReturn:", whileReturn())
    assertEquals("AA:return:", global)

    global = ""
    assertEquals(":whileImmediateReturn:", whileImmediateReturn())
    assertEquals("", global)

    global = ""
    assertEquals(":doWhileReturn:", doWhileReturn())
    assertEquals("AA:return:", global)

    global = ""
    assertEquals(":doWhileReturnFromCondition:", doWhileReturnFromCondition())
    assertEquals("A", global)

    global = ""
    assertEquals(":doWhileImmediateReturn:", doWhileImmediateReturn())
    assertEquals("", global)

    global = ""
    assertEquals(":forReturn:after:", forReturn(true))
    assertEquals("AA", global)

    global = ""
    assertEquals(":forReturn:", forReturn(false))
    assertEquals(":return:", global)

    return "OK"
}