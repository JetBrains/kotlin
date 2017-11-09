fun box() {
    var i = 0
    while(
        try {
           i++
        }
        finally {
            println("finally: $i")
        } < i
    ) {
        println("body: $i")
    }
}

// LINES: 13 5 5 4 2 2 3 5 5 8 8 * 3 4 9 3 11 11