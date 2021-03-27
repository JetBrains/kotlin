import java.util.HashMap

internal enum class E {
    A, B, C
}

internal class A {
    fun foo(list: List<String?>, collection: Collection<Int?>, map: Map<Int, Int?>) {
        val a = "".length
        val b = E.A.name
        val c = E.A.ordinal
        val d = list.size + collection.size
        val e = map.size
        val f = map.keys.size
        val g = map.values.size
        val h = map.entries.size
        val i = map.entries.iterator().next().key + 1
    }

    fun bar(list: MutableList<String?>, map: HashMap<String, Int>) {
        val c = "a"[0]
        val b = 10.toByte()
        val i = 10.1.toInt()
        val f = 10.1.toFloat()
        val l = 10.1.toLong()
        val s = 10.1.toInt().toShort()
        try {
            val removed = list.removeAt(10)
            val isRemoved = list.remove("a")
        } catch (e: Exception) {
            System.err.println(e.message)
            throw RuntimeException(e.cause)
        }
        for (entry in map.entries) {
            val key = entry.key
            val value = entry.value
            entry.setValue(value + 1)
        }
    }

    fun kt21504() {
        val b = "1".toByte()
        val s = "1".toShort()
        val i = "1".toInt()
        val l = "1".toLong()
        val f = "1".toFloat()
        val d = "1".toDouble()
        val b2 = "1".toByte(10)
        val s2 = "1".toShort(10)
        val i2 = "1".toInt(10)
        val l2 = "1".toLong(10)
    }

    fun kt7940() {
        val b1 = Byte.MIN_VALUE
        val b2 = Byte.MAX_VALUE
        val s1 = Short.MIN_VALUE
        val s2 = Short.MAX_VALUE
        val i1 = Int.MIN_VALUE
        val i2 = Int.MAX_VALUE
        val l1 = Long.MIN_VALUE
        val l2 = Long.MAX_VALUE
        val f1 = Float.MIN_VALUE
        val f2 = Float.MAX_VALUE
        val f3 = Float.POSITIVE_INFINITY
        val f4 = Float.NEGATIVE_INFINITY
        val f5 = Float.NaN
        val d1 = Double.MIN_VALUE
        val d2 = Double.MAX_VALUE
        val d3 = Double.POSITIVE_INFINITY
        val d4 = Double.NEGATIVE_INFINITY
        val d5 = Double.NaN
    }

    fun kt35593() {
        val number: Number = 1
        val b = number.toByte()
        val d = number.toDouble()
        val f = number.toFloat()
        val i = number.toInt()
        val l = number.toLong()
        val s = number.toShort()
    }
}