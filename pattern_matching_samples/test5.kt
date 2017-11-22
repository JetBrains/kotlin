fun main(args: Array<String>) {
    val subject = Pair(1, Pair(3, 4))
    val b = when(subject) {
        is Any -> 10
        match :Any -> 20
        match (a, (b, c)) -> 30
        else -> 40
    }
    println("hello $b")
}