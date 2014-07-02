package foo

class A {
    val s = "sA"
    fun memBar(other: String): String = s +":memBar:" + other
}

fun msg(a: A, ref: ExtensionFunction1<A, String, String>): String {
    return a.(ref)("!!")
}

fun box():String {
    fun A.extBar(other: String):String = s + ":extBar:" + other
    val a = A()
    var result = msg(a, A::memBar) + msg(a, A::extBar) + msg(a, {A.(other:String):String -> s + ":literal:" + other })
    return (if (result == "sA:memBar:!!sA:extBar:!!sA:literal:!!") "OK" else result)
}