package foo

class A(var i: Int) {
    override fun toString() = "a$i"
}

fun box(): Boolean {
    var p = A(2);
    var n = A(1);
    if ("$p$n" != "a2a1") {
        return false;
    }
    if ("${A(10)}" != "a10") {
        return false;
    }
    return true;
}

