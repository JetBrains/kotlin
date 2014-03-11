fun main(args: Array<String>) {
    println(max(parseInt(args[0]), parseInt(args[1])))
}

fun max(a: Int, b: Int) = if (a > b) a else b