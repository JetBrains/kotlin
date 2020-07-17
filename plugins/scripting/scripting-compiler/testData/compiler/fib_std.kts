// Expecting two string parameters or nothing

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    println("fib($n)=$v")
    return v
}

val num = if (args.size > 0) java.lang.Integer.parseInt(args[0]) else 4
val comment = if (args.size > 1) args[1] else "none"

println("num: $num ($comment)")
val result = fib(num)
