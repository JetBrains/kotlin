data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int) {
    fun deconstruct() = A(a, b)
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int) {
    println("[ begin ] matcher")
    when (value) {
        is String ->
            println("[   0   ] IS")
        match m @ B(a, #(p2 + p3)) ->
            println("[   1   ] MATCH $m -> { $a }")
        match m @ A(a, #(p2 + p3)) ->
            println("[   2   ] MATCH $m -> { $a }")
        match m @ Pair<*, *>(5, 7) ->
            println("[   3   ] MATCH $m -> {}")
        match m @ Pair<*, *>(a, #p1) ->
            println("[   4   ] MATCH $m -> { $a }")
        match m @ List<*>(:Int, :Int) ->
            println("[   5   ] MATCH $m -> {}")
        match m @ Pair<*, *>(a: Int, b: Int) if (a > p1) ->
            println("[   6   ] MATCH $m -> { $a $b }")
        match m @ Pair<*, *>("some string $p4 with parameter", _) ->
            println("[   7   ] MATCH $m -> {}")
        match m @ Pair<*, *>(: Int, Pair<*, *>(a, b)) ->
            println("[   8   ] MATCH $m -> { $a $b }")
        match m ->
            println("[   9   ] MATCH $m -> {}")
    }
    println("[  end  ] matcher")
}

fun main(args: Array<String>) {
    matcher(B(5, 6), 0, 4, 2, 0)
    matcher(A(5, 6), 0, 3, 3, 0)
    matcher(Pair(5, 7), 0, 0, 0, 0)
    matcher(Pair(1, 2), 2, 0, 0, 0)
    matcher(listOf(1, 2, 3, 4, 5, 6, 7), 0, 0, 0, 0)
    matcher(Pair(1, 2), 0, 0, 0, 0)
    matcher(Pair("some string 4 with parameter", 7), 0, 0, 0, 4)
    matcher("3D4R0V4", 0, 0, 0, 0)
    matcher(10, 0, 0, 0, 0)
    matcher(Pair(1, Pair(3, 9)), 0, 0, 0, 0)
}
