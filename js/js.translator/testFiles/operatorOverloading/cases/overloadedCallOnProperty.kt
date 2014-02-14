package foo

var a = MyInt()

class MyInt() {
    var b = 0

    fun inc(): MyInt {
        val res = MyInt();
        res.b = b;
        res.b++;
        return res;
    }
}


fun box(): Boolean {
    a++;
    a++;
    return (a.b == 2);
}