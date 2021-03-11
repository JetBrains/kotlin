fun foo() {
    Loop@ while (true) {
        when (take()) {
            1 -> continue
            2 -> {
                println("2")
                return
            }
            3 -> break@Loop
        }
        println()
    }
}