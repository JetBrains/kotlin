package foo

class MyInt() {
  var b = 0

    fun inc() {
      b = b + 1;
    }
}


fun box() : Boolean {
    var c = MyInt()
    c++;
    return (c.b == 1);
}