package foo

class A {
    val s = "sA"
    fun memBar(other: String): String = s +":memBar:" + other
}

fun A.extBar(other: String):String = s + ":extBar:" + other

fun box():String {
    fun A.locExtBar(other: String):String = s + ":locExtBar:" + other

    val a = A()

    var r = a.(A::memBar)("!!")
    if (r != "sA:memBar:!!") return r

    r = a.(A::extBar)("!!")
    if (r != "sA:extBar:!!") return r

    r = a.(A::locExtBar)("!!")
    if (r != "sA:locExtBar:!!") return r

    return "OK"
}