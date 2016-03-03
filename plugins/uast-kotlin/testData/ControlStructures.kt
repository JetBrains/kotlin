class ControlStructures {
    fun test(): Boolean {
        if (5 > 3) {
            println("5 > 3")
        }

        for (c in "ABC") {
            println(c)
        }

        for (c: Char in "DEF") {
            println(c.toByte())
        }

        var i = 5
        while (i > 0) {
            i--
            if (i == 3) break
            if (i == 2) {
                continue
            }
        }

        i = 5
        do {
            i -= 1
        } while (i > 0)

        "ABC".forEach { println(it.toString()[0]) }

        "ABC".zip("DEF").forEach { println(it.first + " " + it.second) }

        val (a, b) = "ABC".zip("DEF")

        val value = if (5 > 3) "a" else "b"
        val list = listOf("A")
        val list2 = listOf("A")

        val type = when (value) {
            in list -> "inlist"
            !in list2 -> "notinlist2"
            is String -> "string"
            is CharSequence -> "cs"
            else -> "unknown"
        }

        val x = when {
            value == "b" -> "B"
            5 % 2 == 0 -> {
                println("A")
                "Q"
            }
            false -> "!"
            else -> "A"
        }

        return false
    }
}