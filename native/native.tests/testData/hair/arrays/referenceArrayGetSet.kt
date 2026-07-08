fun roundTrip(): String {
    val a = arrayOfNulls<String>(3)
    a[1] = "hello"
    a[2] = a[1] + " world"
    return a[2]!!
}
fun main() {
    val r = roundTrip()
    if (r != "hello world") error("roundTrip() = $r, expected 'hello world'")
}
