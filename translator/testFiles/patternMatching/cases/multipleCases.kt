namespace foo

fun box() : Boolean {
  val c = 3
  val d = 5
  var z = 0
  when(c) {
    is 5, is 3 => z++;
    else => z = -1000;
  }

  when(d) {
    is 5, is 3 => z++;
    else => z = -1000;
  }
  return c == 2
}