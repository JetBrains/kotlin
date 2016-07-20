object Singleton {
    fun create(x : Int): Int {
        return x * 8
    }
}

fun signleton_test(i: Int): Int {
    return Singleton.create(i)
}

