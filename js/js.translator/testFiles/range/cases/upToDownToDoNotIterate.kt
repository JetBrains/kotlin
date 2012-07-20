package foo

import java.util.ArrayList

fun box() : Boolean {
    for (i in 0 upto -1) {
      return false
    }
    for (i in 0 downto 1) {
      return false
    }
    return true
}