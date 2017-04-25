// EXPECTED_REACHABLE_NODES: 498
package foo

var global: String = ""

class A {
    var prop: Boolean = false
}

fun getA(): A = try { global += "getA"; A() } finally {}

fun barEq(): String {
    10 == return "eq"
}

fun barLt(): String {
    10 < (return "lt") as Int
}

fun setGlobal(i: Int): Int {
    global = "setGlobal"
    return i
}

fun box(): String {
    var b: Boolean

    var i = 0
    while(i++<5) {
        if (i==2) 10 == break
    }
    assertEquals(2, i, "break 1")

    i = 0
    while(i++<5) {
        if (i==2) 10 < (break as Int)
    }
    assertEquals(2, i, "break 2")

    i = 0
    while(i++<5) {
        if (i==2) {
            var x = 10 == break;
        }
    }
    assertEquals(2, i, "break 3")

    i = 0
    while(i++<5) {
        if (i==2) {
            var x = 10 < (break as Int)
        }
    }
    assertEquals(2, i, "break 4")

    i = 0
    var bVar: Boolean
    while(i++<5) {
        if (i==2) {
            bVar = 10 < (break as Int)
        }
    }
    assertEquals(2, i, "break 5")

    i = 0
    var bVarArray = arrayOf(true, false)
    while(i++<5) {
        if (i==2) {
            bVarArray[try { global += "A"; 0} finally {}] = 10 < (break as Int)
        }
    }
    assertEquals(2, i, "break 6")
    assertEquals("A", global, "break 6")

    i = 0
    while(i++<5) {
        if (i==2) {
            bVarArray[setGlobal(0)] = 10 < (break as Int)
        }
    }
    assertEquals(2, i, "break 6a")
    assertEquals("setGlobal", global, "break 6a")

    i = 0
    global = ""
    while(i++<5) {
        if (i==2) {
            getA().prop = 10 < (break as Int)
        }
    }
    assertEquals(2, i, "break 7")
    assertEquals("getA", global, "break 7")

    i = 0
    var n = 0
    while(i++<5) {
        if (i==2) 10 == continue
        n++
    }
    assertEquals(4, n, "continue 1")

    i = 0
    n = 0
    while(i++<5) {
        if (i==2) 10 < (continue as Int)
        n++
    }
    assertEquals(4, n, "continue 2")

    i = 0
    n = 0
    while(i++<5) {
        if (i==2)  {
            var x = 10 == continue
        }
        n++
    }
    assertEquals(4, n, "continue 3")

    i = 0
    n = 0
    while(i++<5) {
        if (i==2)  {
            var x = 10 < (continue as Int)
        }
        n++
    }
    assertEquals(4, n, "continue 4")

    assertEquals("eq", barEq())
    assertEquals("lt", barLt())

    return "OK"
}