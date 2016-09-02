abstract class Base<A> {
    fun baseF(): A = null!!
}

annotation class Anno

@Anno
class Test<T> : Base<Int>() {
    fun a(): String = ""
    fun b(i: String, b: CharSequence) {}
    fun c(): Int = 5
    fun d(): T = null!!
    fun <D : Any> e(item: D): D = item
    fun f(items: List<Map<String, Int>>, i: Int) {}
    fun <D : String> g(item: D) {}
}