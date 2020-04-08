// DISABLE-ERRORS
val array = Array(2) { it }
operator fun <T> Array<T>.contains(t: T): Boolean = false

val a = <selection>1 in array</selection>
val b = array.contains(1)
val c = 1.contains(array)
val d = 1 !in array
val e = {
    when (1) {
        in array -> {}
    }
}
