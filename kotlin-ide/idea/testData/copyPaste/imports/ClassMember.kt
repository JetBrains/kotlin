package a

class A(val c: Int = 1) {
    var d = 2

    val g: Int
        get() = d

    fun j() = c

    <selection>fun f() = c + d + g + j()</selection>
}