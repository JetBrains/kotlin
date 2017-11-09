package android.util

class SparseArray<E : Any> {
    private val map = HashMap<Int, E>()

    fun get(key: Int): E? {
        return map.get(key)
    }

    fun put(key: Int, value: E) {
        map.put(key, value)
    }

    fun remove(key: Int): E? {
        return map.remove(key)
    }

    fun clear() {}
}