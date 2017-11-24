fun matcher(value: Any?) {
    when (value) {
        match Pair<*, *>(a: Pair<*, *> @ Pair<*, *>(b: Int, _), :Int) ->
            println("[   1   ] MATCH -> $a, $b")
        match x ->
            println("[   0   ] MATCH -> $x")
    }
}

fun main(args: Array<String>) {
    matcher(Pair(Pair(2, Any()), 1))
    matcher(Pair(Pair(Any(), Any()), 1))
}