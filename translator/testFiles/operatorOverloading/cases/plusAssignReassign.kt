package foo

class A(i : Int) {

    val a = i

    fun plusAssign(other : A) = A(this.a + other.a)

}

fun box() : Boolean {
    var c = A(2)
    val d = c
    c += A(3)
    return (c.a == 5) && (d.a == 2) && (d != c)
}