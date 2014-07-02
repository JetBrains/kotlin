package foo

class A {
    val s = "sA"
    fun memBar(other: String): String = s +":memBar:" + other
}

fun A.extBar(other: String):String = s + ":extBar:" + other

fun box():String {
    val a = A()
    var result = a.(A::memBar)("!!") + a.(A::extBar)("!!")
    return (if (result == "sA:memBar:!!sA:extBar:!!") "OK" else result)
}