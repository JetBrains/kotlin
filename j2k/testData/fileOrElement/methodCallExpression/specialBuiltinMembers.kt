import java.util.*

internal enum class E {
    A, B, C
}

internal class A {
    fun foo(list: List<String>, collection: Collection<Int>, map: Map<Int, Int>) {
        val a = "".length
        val b = E.A.name
        val c = E.A.ordinal
        val d = list.size + collection.size
        val e = map.size
        val f = map.keys.size
        val g = map.values.size
        val h = map.entries.size
    }

    fun bar(list: MutableList<String>) {
        val c = "a"[0]
        val b = 10.toByte()
        val i = 10.1.toInt()
        val f = 10.1.toFloat()
        val l = 10.1.toLong()
        val s = 10.1.toShort()

        val removed = list.removeAt(10)
        val isRemoved = list.remove("a")
    }
}
