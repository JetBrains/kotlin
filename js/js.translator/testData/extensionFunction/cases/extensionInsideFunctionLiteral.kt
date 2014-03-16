package foo

class M() {
    var m = 0

    fun eval() {
        var d = {
            var c = { Int.() -> this + 3 }
            m += 3.c()
        }
        d();
    }
}

fun box(): Boolean {
    var a = M()
    if (a.m != 0) return false;
    a.eval()
    if (a.m != 6) return false;

    return true;
}