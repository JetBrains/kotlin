package demo

internal class Map {
    internal fun <K, V> put(k: K?, v: V) {
    }
}

internal class U {
    internal fun test() {
        val m = Map()
        m.put<String, Int>(null, 10)
    }
}