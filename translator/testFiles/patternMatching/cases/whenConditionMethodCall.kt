namespace foo

class A() {
    fun p() = true
}

fun box() : Boolean {
  var a = 0
  when(A()) {
    .p() => a--
    is A? => a++;
    is A => a++;
    else => a++;
  }
  return (a == -1)
}