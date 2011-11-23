namespace foo

class A() {
    val pp = true
    fun p() = true
}

fun box() : Boolean {
  var a = 0
  when(A()) {
    .pp => a -=2
    .p() => a--
    is A? => a++;
    is A => a++;
    else => a++;
  }
  return (a == -2)
}