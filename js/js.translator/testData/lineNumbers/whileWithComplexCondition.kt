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

// LINES: 5 2 3 5 8 3 4 9 3 11