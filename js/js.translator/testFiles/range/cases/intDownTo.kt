package foo

import java.util.ArrayList

fun box() : Boolean {
    var elems = ArrayList<Int>()
    for (i in 4 downto 0) {
      elems.add(i)
    }
    return elems[0] == 4 && elems[1] == 3 && elems[2] == 2 && elems[3] == 1 &&  elems[4] == 0
}