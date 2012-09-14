package foo

fun <T> Iterator<T>.take(n: Int): ()->Boolean {
    var count = n
    return { --count >= 0 }
}

fun box(): Boolean {
    return java.util.ArrayList<Int>().iterator().take(3)()
}