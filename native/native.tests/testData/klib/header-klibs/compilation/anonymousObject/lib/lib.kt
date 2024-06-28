package lib

interface Interface {
    fun getInt(): Int
}

fun getInterface(): Interface =
    object : Interface {
        override fun getInt(): Int = 10
    }