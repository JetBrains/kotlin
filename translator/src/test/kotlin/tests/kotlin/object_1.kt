object Singleton {
    fun create(x : Int): Int {
        return x * 8
    }
}

fun singleton_test(i: Int): Int {
    return Singleton.create(i)
}
