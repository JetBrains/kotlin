// EXPECTED_REACHABLE_NODES: 496
package foo

fun run(a: A, arg: String, funRef:(A, String) -> String): String {
    return funRef(a, arg)
}

class A {
    val s = "sA"
    fun memBar(other: String): String = s +":memBar:" + other
}

fun A.extBar(other: String):String = s + ":extBar:" + other

fun box():String {
    fun A.locExtBar(other: String):String = s + ":locExtBar:" + other

    val a = A()

    var r = run(a, "!!", A::memBar)
    if (r != "sA:memBar:!!") return r

    r = run(a, "!!", A::extBar)
    if (r != "sA:extBar:!!") return r

    r = run(a, "!!", A::locExtBar)
    if (r != "sA:locExtBar:!!") return r

    r = run(a, "!!") { a: A, other: String -> a.s + ":literal:" + other }
    if (r != "sA:literal:!!") return r

    return "OK"
}
