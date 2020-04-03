// this script expected parameter num : Int

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    println("fib($n)=$v")
    return v
}

println("num: $num")
val result = fib(num)
