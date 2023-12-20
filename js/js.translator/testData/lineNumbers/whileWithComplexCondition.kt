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

// LINES(JS_IR): 1 1 2 * 3 * 5 5 5 5 8 8 * 4 9 * 11 11
