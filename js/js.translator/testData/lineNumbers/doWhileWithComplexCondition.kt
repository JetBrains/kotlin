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

// LINES: 8 3 2 3 3 3 8 11 3 7 12 3 3 4