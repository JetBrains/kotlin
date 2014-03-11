package foo

class MyInt(i: Int) {
    var b = i
    fun inc(): MyInt {
        b++;
        return this;
    }
}

fun box(): Boolean {
    var t = MyInt(0)
    t++;
    return (t.b == 1)
}