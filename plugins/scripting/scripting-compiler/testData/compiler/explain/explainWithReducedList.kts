val ints = listOf(1, 2, 3, 4, 5)

if (ints.isNotEmpty()) {
    val reduced = ints.reduce { acc, value ->
        println("Reduce step acc=$acc value=$value")
        acc + value
    }
}