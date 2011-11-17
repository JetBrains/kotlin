namespace foo

class A() {

}

class B() {

}

fun box() : Boolean {
  var c : Int = 0
  var a = A() : Any?
  var b = null : Any?
  when(a) {
    is null => c = 10;
    is B => c = 10000
    is A  => c = 20;
  }
  when(b) {
    is null => c += 5
    is B => c += 100
  }
  return (c == 25)
}