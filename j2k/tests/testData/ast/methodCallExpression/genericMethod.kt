package demo

class Map {
    fun <K, V> put(k: K, v: V) {
    }
}

class U {
    fun test() {
        val m = Map()
        m.put<String, Int>("10", 10)
    }
}