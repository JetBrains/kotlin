expect class Foo {
    val p: Int
    fun bar(r: () -> Int = this::p): Int
}
actual class Foo {
    actual val p = 42
    actual fun bar(r: () -> Int) = r()
}
fun main() {
    println(Foo().bar())
}
