package foo

open abstract class A() {
    abstract var pos: Int;
}

class B() : A() {
    override var pos: Int = 2
}

fun box(): Boolean {

    val a: A = B()
    if (a.pos != 2) {
        return false;
    }
    if (B().pos != 2) {
        return false;
    }
    a.pos = 3;
    if (a.pos != 3) {
        return false
    }
    return true
}