package foo

var a = MyInt()

class MyInt() {
  var b = 0

    fun inc() = b + 1
}


fun box() : Boolean {
    a++;
    return (a.b == 1);
}