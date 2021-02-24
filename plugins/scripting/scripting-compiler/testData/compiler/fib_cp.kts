// this script expected parameter param : class { val memberNum: Int }

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    println("fib($n)=$v")
    return v
}

println("num: ${param.memberNum}")
val result = fib(param.memberNum)

