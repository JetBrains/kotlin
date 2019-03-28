fun foo(a: Int): Int {
    return when (a) {
        1 -> {
            println("1")
            1
        }
        2 -> {
            println("2")
            2
        }
        3 -> {
            println("3")
            throw RuntimeException()
        }
        else -> {
            println("default")
            0
        }
    }
}