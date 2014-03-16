package foo

class MyInt() {
    var b = 0

    fun dec(): MyInt {
        val res = MyInt()
        res.b++;
        return res;
    }
}


fun box(): Boolean {
    var c = MyInt()
    --c;
    return (c.b == 1);
}