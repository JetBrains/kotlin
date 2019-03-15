// EXPECTED_REACHABLE_NODES: 1236
package foo

fun whileReturn(): String {
    var log = ""
    var i = 0
    while(if (i<2) { log += "A"; i++; true } else { return log + ":whileReturn:"});
    return ":whileReturn:after:"
}

fun whileImmediateReturn(): String {
    while(return ":whileImmediateReturn:") {
    }
    return ":whileImmediateReturn:after"
}

fun doWhileReturn(): String {
    var log = ""
    var i = 0
    do while(if (i<2) { log += "A"; i++; true} else { return log + ":doWhileReturn:"})
    return ":doWhileReturn:after:"
}

fun doWhileReturnFromCondition(): String {
    do {
    } while(return ":doWhileReturnFromCondition:")
    return ":doWhileReturnFromCondition:after"
}

fun forReturn(b: Boolean): String {
    for(i in (if (b) 1..2 else { return ":forReturn:"}));
    return ":forReturn:after:"
}

fun box(): String {
    assertEquals("AA:whileReturn:", whileReturn())

    assertEquals(":whileImmediateReturn:", whileImmediateReturn())

    assertEquals("AA:doWhileReturn:", doWhileReturn())

    assertEquals(":doWhileReturnFromCondition:", doWhileReturnFromCondition())

    assertEquals(":forReturn:after:", forReturn(true))

    assertEquals(":forReturn:", forReturn(false))

    return "OK"
}
