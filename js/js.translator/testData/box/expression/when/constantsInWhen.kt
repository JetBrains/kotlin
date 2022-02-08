



fun test(i: Int, j: Int, k: Int, episode: Boolean, ep2: Boolean = false): String {
    return when (i) {
        5 -> when (j) {
            0 -> when (k) {
                0 -> if (episode) "A" else "B"
                1 -> if (episode) "C" else "D"
                3 -> if (episode) "E" else "F"
                else -> if (episode) "G" else "H"
            }
            1 -> when (k) {
                0 -> if (episode) "I" else "J"
                1 -> if (episode) "K" else "L"
                3 -> if (episode) "M" else "N"
                else -> if (episode) "O" else "P"
            }
            2 -> when (k) {
                0 -> if (episode) "Q" else "R"
                1 -> if (episode) "S" else "T"
                3 -> if (episode) "U" else "V"
                else -> if (episode) "W" else "X"
            }
            else -> if (episode) "Y" else "Z"
        }
        3 -> when (j) {
            0 -> if (episode) "1" else if (ep2) "!" else "2"
            1 -> if (episode) "3" else if (ep2) "?" else "4"
            3 -> if (episode) "5" else if (ep2) "$" else "6"
            else -> if (episode) "7" else if (ep2) "#" else "8"
        }
        else -> "9"
    }
}

fun test2(i: Int, episode: Boolean): String {
    return when (i) {

        5 -> when (i % 3) {
            0 -> if (episode) "FAIL3" else "FAIL2"
            2 -> if (episode) "O" else "K"
            else -> if (episode) "FAIL4" else "FAIL5"
        }

        else -> "FAIL1"
    }
}


fun box(): String {
    var result = ""

    result += test(5, 0, 0, true)
    result += test(5, 0, 0, false)
    result += test(5, 0, 1, true)
    result += test(5, 0, 1, false)
    result += test(5, 0, 3, true)
    result += test(5, 0, 3, false)
    result += test(5, 0, 2, true)
    result += test(5, 0, 2, false)

    result += test(5, 1, 0, true)
    result += test(5, 1, 0, false)
    result += test(5, 1, 1, true)
    result += test(5, 1, 1, false)
    result += test(5, 1, 3, true)
    result += test(5, 1, 3, false)
    result += test(5, 1, 2, true)
    result += test(5, 1, 2, false)

    result += test(5, 2, 0, true)
    result += test(5, 2, 0, false)
    result += test(5, 2, 1, true)
    result += test(5, 2, 1, false)
    result += test(5, 2, 3, true)
    result += test(5, 2, 3, false)
    result += test(5, 2, 2, true)
    result += test(5, 2, 2, false)

    result += test(5, 3, 0, true)
    result += test(5, 3, 0, false)
    result += test(5, 3, 1, true)
    result += test(5, 3, 1, false)
    result += test(5, 3, 3, true)
    result += test(5, 3, 3, false)
    result += test(5, 3, 2, true)
    result += test(5, 3, 2, false)

    result += test(3, 0, -1, true)
    result += test(3, 0, -1, false, true)
    result += test(3, 0, -1, false, false)
    result += test(3, 1, -1, true)
    result += test(3, 1, -1, false, true)
    result += test(3, 1, -1, false, false)
    result += test(3, 2, -1, true)
    result += test(3, 2, -1, false, true)
    result += test(3, 2, -1, false, false)
    result += test(3, 3, -1, true)
    result += test(3, 3, -1, false, true)
    result += test(3, 3, -1, false, false)

    result += test(1, -1, -1, false)

    if (result != "ABCDEFGHIJKLMNOPQRSTUVWXYZYZYZYZ1!23?47#85\$69") return "FAIL1: $result"

    return test2(5, true) + test2(5, false)
}