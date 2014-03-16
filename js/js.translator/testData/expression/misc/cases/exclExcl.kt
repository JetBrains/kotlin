package foo


fun box(): Boolean {
    val a: Int? = 0

    return (a!! + 3) == 3
}