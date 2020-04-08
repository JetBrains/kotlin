// DISABLE-ERRORS
val array = Array(2) { it }
fun <T> Array<T>.contains(t: T): Boolean = false

val a = 1 in array
val b = array.contains(1)
val c = 1.contains(array)
val d = <selection>1 !in array</selection>
val e = !array.contains(1)