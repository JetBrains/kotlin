// KIND: STANDALONE
// MODULE: SetExport
// FILE: main.kt

class Box(val x: Int) {
    override fun equals(other: Any?): Boolean = when (other) {
        is Box -> x == other.x
        else -> false
    }
    override fun hashCode(): Int = x.hashCode()
    override fun toString(): String = "Box($x)"
}

fun <T> Set<T>.recollect(): Set<T> {
    val result = mutableSetOf<T>()
    this.forEach { result += it }
    return result.toSet()
}

fun testSetInt(s: Set<Int>) = s.recollect()
fun testSetShort(s: Set<Short>) = s.recollect()
fun testSetString(s: Set<String>) = s.recollect()
fun testSetBox(s: Set<Box>) = s.recollect()

fun testSetOptInt(s: Set<Int?>) = s.recollect()
fun testSetOptString(s: Set<String?>) = s.recollect()
fun testSetOptBox(s: Set<Box?>) = s.recollect()

fun testSetListInt(s: Set<List<Int>>) = s.recollect()
fun testSetSetInt(s: Set<Set<Int>>) = s.recollect()
fun testSetOptSetInt(s: Set<Set<Int>?>) = s.recollect()
fun testOptSetInt(s: Set<Int>?) = s?.recollect()

fun testSetNothing(s: Set<Nothing>) = s.recollect()
fun testSetOptNothing(s: Set<Nothing?>) = s.recollect()
