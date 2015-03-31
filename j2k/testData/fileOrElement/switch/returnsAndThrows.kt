fun foo(a: Int): Int {
    when (a) {
        1 -> {
            println("1")
            return 1
        }
        2 -> {
            println("2")
            return 2
        }
        3 -> {
            println("3")
            throw RuntimeException()
        }
        else -> {
            println("default")
            return 0
        }
    }
}