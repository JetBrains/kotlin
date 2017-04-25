// EXPECTED_REACHABLE_NODES: 494
package foo

class A {
    val s = "sA"
    fun memBar(other: String): String = s +":memBar:" + other
}

fun A.extBar(other: String):String = s + ":extBar:" + other

fun box():String {
    fun A.locExtBar(other: String):String = s + ":locExtBar:" + other

    val a = A()

    var r = (A::memBar)(a, "!!")
    if (r != "sA:memBar:!!") return r

    r = (A::extBar)(a, "!!")
    if (r != "sA:extBar:!!") return r

    r = (A::locExtBar)(a, "!!")
    if (r != "sA:locExtBar:!!") return r

    return "OK"
}
