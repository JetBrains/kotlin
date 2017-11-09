fun box() {
    var i = 0
    do {
        println("body: $i")
    }
    while(
        try {
           i++
        }
        finally {
            println("finally: $i")
        } < i
    )
}

// LINES: 14 8 8 7 3 2 2 3 3 3 8 8 11 11 * 3 7 12 3 3 4 4