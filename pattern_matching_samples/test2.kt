data class TContainer(val a: Int, val b: Int, val c: Int)

open class A

open class B<T>(val a: Int, val b: T, val c: Int, d: Int) : A() {
    fun component1() = a

    fun component2() = a to b

    fun component3() = "$c"
}

class C(a: Int, b: Int, c: Int, d: Int) : B<Int>(a, b, c, d)


fun main(args: Array<String>) {
    val c = C(1, 2, 3, 4)
    val number = when (c) {
        match m @ a: A @ B(e1, e2 @ Pair(l, r) if(l > r), e3) -> e1 + l + r
        match (_, p :Pair, _) -> 20
        else -> 40
    }
    println(number)
}
