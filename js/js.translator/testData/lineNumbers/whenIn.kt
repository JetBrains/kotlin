fun box() {
    when (foo()) {
        in 2..5 ->
            println("A")
        !in 100..200 ->
            println("B")
        in bar() ->
            println("C")
    }
}

fun foo(): Int = 23

fun bar(): IntRange = 1000..2000

// LINES(JS_IR): 1 1 2 3 3 4 4 5 5 6 6 7 7 7 7 8 8 12 12 12 12 14 14 14 14
