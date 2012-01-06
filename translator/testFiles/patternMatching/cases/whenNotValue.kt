package foo

fun box() : Boolean {
  var a = 4
  when(a) {
    !is 3 -> {a = 10;}
    !is 4 -> {a = 20;}
  }
  return (a == 10)
}