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

// LINES(JS_IR): 1 1 2 3 3 4 4 * 8 8 8 8 11 11 * 7 12
