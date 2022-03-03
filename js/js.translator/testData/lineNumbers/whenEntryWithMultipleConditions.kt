fun box(x: Int) {
    println(
            when (
                x
            ) {
                1,
                2,
                3 ->
                    55

                4,
                5 ->
                    66

                else ->
                    77
            }
    )
}

// LINES(JS):    1 19 4 4 3 3 4 6 9 9 6 11 13 13 11 16 16 3 2
// LINES(JS_IR): 4 4 2 6 7 8 9 11 12 13 16
