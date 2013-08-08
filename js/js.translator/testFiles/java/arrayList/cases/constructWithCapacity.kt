package foo

import java.util.ArrayList

fun box(): Boolean {
  val al = ArrayList<Int>(10)
  return al.size() == 0
}