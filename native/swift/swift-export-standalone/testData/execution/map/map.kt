// KIND: STANDALONE
// MODULE: MapExport
// FILE: main.kt

class Box(val x: Int) {
    override fun equals(other: Any?): Boolean = when (other) {
        is Box -> x == other.x
        else -> false
    }
    override fun hashCode(): Int = x.hashCode()
    override fun toString(): String = "Box($x)"
}

fun <K, V> Map<K, V>.inversed(): Map<V, Set<K>> {
    return entries.groupBy({ it.value }) { it.key }.mapValues { it.value.toSet() }
}

fun inverseMapIntString(m: Map<Int, String>) = m.inversed()
fun inverseMapStringInt(m: Map<String, Int>) = m.inversed()

fun inverseMapLongBox(m: Map<Long, Box>) = m.inversed()
fun inverseMapBoxLong(m: Map<Box, Long>) = m.inversed()

fun inverseMapOptIntListInt(m: Map<Int?, List<Int>>) = m.inversed()

fun inverseMapNothingNothing(m: Map<Nothing, Nothing>) = m.inversed()
