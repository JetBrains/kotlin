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

// LINES(ClassicFrontend JS_IR): 1 1 2 8 7 6 4 6 7 4 7 8 4 8 9 12 11 4 11 12 4 12 13 16
// LINES(FIR JS_IR):             1 1 2 6 4 6 7 4 7 8 4 8 9 11 4 11 12 4 12 13 16
