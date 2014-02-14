package foo

var a = MyInt()

class MyInt() {
    var b = 0

    fun inc(): MyInt {
        b = b + 1;
        return this;
    }
}


fun box(): Boolean {
    val d = a++;
    return (a.b == 1) && (d.b == 1);
}