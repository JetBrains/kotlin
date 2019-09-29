fun foo() {
    Loop@ loop@ while (true) {
        when (take()) {
            1 -> continue@loop
            2 -> {
                println("2")
                return
            }
            3 -> break@Loop
        }
        println()
    }
}