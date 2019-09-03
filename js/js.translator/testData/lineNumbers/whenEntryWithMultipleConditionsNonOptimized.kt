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

// LINES: 1 19 4 4 3 6 4 6 4 7 4 8 9 9 11 4 11 4 12 13 13 16 16 2 21 21 21 23 23 23 25 25 25 27 27 27 29 29 29