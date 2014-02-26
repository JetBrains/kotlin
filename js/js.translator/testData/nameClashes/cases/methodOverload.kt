package foo

class A() {

    fun eval() = 3
    fun eval(a: Int) = 4
    fun eval(a: String) = 5
    fun eval(a: String, b: Int) = 6

}

fun box(): Boolean {

    if (A().eval() != 3) return false;
    if (A().eval(2) != 4) return false;
    if (A().eval("3") != 5) return false;
    if (A().eval("a", 3) != 6) return false;

    return true;
}