package foo

import java.util.ArrayList

var baz = 0
fun withSideEffect(v: Int): Int {
  baz = v
  return v
}

fun box(): Boolean {
  val al = ArrayList<Int>(withSideEffect(2))
  return al.size() == 0 && baz == 2
}