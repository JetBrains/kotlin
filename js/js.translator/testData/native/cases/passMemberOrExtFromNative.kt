package foo

native
class A(val v: String) {
    fun m(i:Int, s:String): String = noImpl
}
native
fun A.nativeExt(i:Int, s:String): String = noImpl

native("nativeExt2AnotherName")
fun A.nativeExt2(i:Int, s:String): String = noImpl

fun bar(a: A, extLambda: A.(Int, String) -> String): String = a.(extLambda)(4, "boo")

fun box(): String {
    val a = A("test")

    assertEquals("A.m test 4 boo", a.m(4, "boo"))
    assertEquals("A.m test 4 boo", bar(a, fun A.(i, s) = (A::m)(this, i, s)))

    assertEquals("nativeExt test 4 boo", a.nativeExt(4, "boo"))
    assertEquals("nativeExt test 4 boo", bar(a, fun A.(i, s) = (A::nativeExt)(this, i, s)))

    assertEquals("nativeExt2 test 4 boo", a.nativeExt2(4, "boo"))
    assertEquals("nativeExt2 test 4 boo", bar(a, fun A.(i, s) = (A::nativeExt2)(this, i, s)))

    return "OK"
}
