package foo

class MyInt() {
    var b = 0

    fun dec(): MyInt {
        b = b + 1;
        return this;
    }
}


fun box(): Boolean {
    var c = MyInt()
    val d = --c;
    return (c.b == 1);
}