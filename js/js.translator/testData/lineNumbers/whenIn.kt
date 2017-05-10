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

// LINES: 2 2 3 4 5 6 7 8 12 14