// EXPECTED_REACHABLE_NODES: 503
package foo

var global = ""

fun addToGlobal(s: String): String {
    global += s
    return ""
}

fun bar(s: String, u: Int) { global += ":bar:${s}" }

class A() {
    fun memFun(s: String) { global += ":memFun:${s}" }
}

class B() {
    fun A.bExt(s: String) { global += ":bExt:${s}" }

    fun baz(s: String, a: A) = a.bExt(s)
}

fun A.extFun(s: String) { global += ":extFun:${s}" }

fun box(): String {

    bar(addToGlobal("A"), try { global += "B"; 10 } finally {})
    assertEquals("AB:bar:", global)

    global = ""
    val a = A()
    a.memFun(try { "A" } finally {})
    assertEquals(":memFun:A", global)

    global = ""
    (try { global += "A"; a } finally {}).memFun("B")
    assertEquals("A:memFun:B", global)

    global = ""
    (try { global += "A"; a } finally {}).memFun(try { global += "B"; "C" } finally {})
    assertEquals("AB:memFun:C", global)

    global = ""
    val b = B()
    b.baz("S", a)
    assertEquals(":bExt:S", global)

    return "OK"
}