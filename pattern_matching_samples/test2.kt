import kotlin.system.measureTimeMillis

fun matcher(p: Any?) = when (p) {
    is String -> println("string")
    match Pair<*, *>(a, Pair<*, *>(b, c)) -> println("$a $b $c")
    else -> println(p)
}

fun main(args: Array<String>) {
    val p: Any = Pair(1, Pair(2, 3))
    println(measureTimeMillis {
        matcher(p)
    })
}
