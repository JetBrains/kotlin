package foo

class A {
    val s = "sA"
    fun memBar(other: String): String = s +":memBar:" + other
}

fun A.extBar(other: String):String = s + ":extBar:" + other

fun msg(a: A, ref: ExtensionFunction1<A, String, String>): String {
    return a.(ref)("!!")
}

fun box():String {
    val a = A()
    var result = msg(a, A::memBar) + msg(a, A::extBar)
    return (if (result == "sA:memBar:!!sA:extBar:!!") "OK" else result)
}