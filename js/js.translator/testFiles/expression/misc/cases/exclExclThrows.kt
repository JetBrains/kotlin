package foo


fun box(): Boolean {
    val a: Int? = null

    return (a!! + 3) == 3
}