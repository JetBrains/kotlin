package foo

open class A(val v: String) {
    open fun m(i:Int, s:String): String = "A.m ${this.v} $i $s"
}

class B(v: String): A(v) {
    override fun m(i:Int, s:String): String = "B.m ${this.v} $i $s"
}

native
fun bar(a: A, extLambda: A.(Int, String) -> String): String = noImpl

fun A.topLevelExt(i:Int, s:String): String = "A::topLevelExt ${this.v} $i $s"

fun box(): String {
    val a = A("test")

    var r = bar(a) { i, s -> "${this.v} $i $s"}
    if (r != "test 4 boo") return r

    fun A.LocalExt(i:Int, s:String): String = "A::LocalExt ${this.v} $i $s"

    r = bar(a, A::topLevelExt)
    if (r != "A::topLevelExt test 4 boo") return r

    r = bar(a, A::LocalExt)
    if (r != "A::LocalExt test 4 boo") return r

    r = bar(a, A::m)
    if (r != "A.m test 4 boo") return r

    val b = B("test")
    r = bar(b, A::m)
    if (r != "B.m test 4 boo") return r

    return "OK"
}