package lib

interface Interface {
    fun getInt(): Int
}

inline fun getCounter(crossinline init: () -> Int): Interface =
    object : Interface {
        var value = init()
        override fun getInt(): Int = value++
    }
