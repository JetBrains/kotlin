fun box(x: Int) {
    println(
            when (
                x
            ) {
                one(),
                two(),
                three() ->
                    55

                four(),
                five() ->
                    66

                else ->
                    77
            }
    )
}

fun one() = 1

fun two() = 2

fun three() = 3

fun four() = 4

fun five() = 5

// LINES(ClassicFrontend JS_IR): 1 1 2 8 7 6 4 6 7 4 7 8 4 8 9 12 11 4 11 12 4 12 13 16 21 21 21 21 23 23 23 23 25 25 25 25 27 27 27 27 29 29 29 29
// LINES(FIR JS_IR):             1 1 2 6 4 6 7 4 7 8 4 8 9 11 4 11 12 4 12 13 16 21 21 21 21 23 23 23 23 25 25 25 25 27 27 27 27 29 29 29 29
