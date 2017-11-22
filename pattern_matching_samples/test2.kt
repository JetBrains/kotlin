data class TContainer(val a: Int, val b: Int, val c: Int)

open class A

open class B<T>(val a: T, val b: Int, val c: Int, d: Int) : A() {
    fun component1() = a

    fun component2() = a to b

    fun component3() = "$c"
}

class C<T>(a: T, b: Int, c: Int, d: Int) : B<T>(a, b, c, d)


fun main(args: Array<String>) {
    val value: B<Int> = C(1, 2, 3, 4)
    val number = when (value) {
        match C(a, (b, c)) -> a
        match B(1, p @ (b: Int, c) if(b > c)) -> b + c
        match B<Int>(a, p @ (b, c) if (b > c)) -> a + b + c
        match any @ a: A @ (12, (b, c)) if (b > c) -> 10
        match (_, p: Pair, _) -> 20
        else -> 40
    }
    println(number)
}
