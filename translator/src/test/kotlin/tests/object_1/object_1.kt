object Singleton {

    val i: Int = 123456789

    fun create(x : Int): Int {
        return x * 8
    }
}

fun singleton_test(i: Int): Int {
    return Singleton.create(i)
}

fun singleton_test2(): Int {
    return Singleton.i
}
