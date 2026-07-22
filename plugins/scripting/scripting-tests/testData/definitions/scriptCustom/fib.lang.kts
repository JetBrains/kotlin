// this script expects parameter num : Int

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    return v
}

val result = fib(num)
