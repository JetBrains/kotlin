package foo

var i = 0

class A() {
    override fun toString(): String {
        i++
        return "bar"
    }
}

fun box(): Boolean {
    val a = A()
    val s = "$a == $a"
    return s == "bar == bar" && i == 2
}