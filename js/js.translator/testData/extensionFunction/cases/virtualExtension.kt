package foo

class A(var a: Int) {

    fun Int.modify(): Int {
        return this * 3;
    }

    fun eval() = a.modify();
}

fun box(): Boolean {
    val a = A(4)
    return (a.eval() == 12)
}
