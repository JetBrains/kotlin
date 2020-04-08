package first

import java.util.HashMap

fun firstFun() {
  val a = HashMap<Int, String>()
  a.toSort<caret>
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"toSortedMap", itemText:"toSortedMap", tailText:"() for Map<out K, V> in kotlin.collections" }
// EXIST: { lookupString:"toSortedMap", itemText:"toSortedMap", tailText:"(comparator: Comparator<in Int>) for Map<out K, V> in kotlin.collections" }
// NOTHING_ELSE
