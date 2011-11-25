namespace foo

class A(var b : Int, var a : String) {

}

fun box() : Boolean {
    val c = A(1, "1")
    return (c.b == 1 && c.a == "1")
}