// SET_TRUE: ALLOW_TRAILING_COMMA

fun test() {
    val (a, b) = 1 to 2

    val (a, b) = 1 to
            2

    val (a, b) = 1
    to
    2


    val (a, b) = 1 to 2

    val (a) =
            b

    val (
            a,
    ) =
            b

    val (a
    ) =
            b

    val (a) = b

    val (
            a,
    ) = b

    val (a, b
    ) = 1 to 2

    val (a,
            b) = 1 to 2

    val (
            a, b
    ) = 1 to 2

    val (
            a, b,
    ) = 1 to 2

    val (a, b, c,
            d, f
    ) = 1 to 2

    val (
            a, b, c,
            d, f,
    ) = 1 to 2

    val (a, b/**/) = 1 to 2

    val (a, /**/b/**/) /**/ =/**/ 1 to
            2

    val (a,/**/ b) = 1
    to
    2

    val (a, b/**/) = 1 to 2

    val (a/**/, b/**//**/) = 1 to 2

    val (a/**//**/) =
            b

    val (a) = b

    val (a, b/**/
    ) = 1 to 2

    val (a, b// awd
    ) = 1 to 2

    val (a,/**/ b /**/       // awd
    ) = 1 to 2

    val (a, // ad
            /**/b) = 1 to 2

    val (
            a, b // fe
            /**/)/**/ = 1 to 2

    val (
            a, b, // awd
    ) = 1 to 2

    val (a, b, c,
            d, f // awd
            /*
        */) = 1 to 2

    val (
            a, b, c,
            d, f, // awd
    ) = 1 to 2
}