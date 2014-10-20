fun foo(a: Int): Int {
    when (a) {
        1 -> {
            System.out.println("1")
            return 1
        }
        2 -> {
            System.out.println("2")
            return 2
        }
        3 -> {
            System.out.println("3")
            throw RuntimeException()
        }
        else -> {
            System.out.println("default")
            return 0
        }
    }
}