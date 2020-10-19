fun isOdd(x: Int) = x % 2 != 0

fun a() {
    val numbers = listOf(1, 2, 3)
    println(numbers.filter(<warning descr="SSR">::isOdd</warning>))
}