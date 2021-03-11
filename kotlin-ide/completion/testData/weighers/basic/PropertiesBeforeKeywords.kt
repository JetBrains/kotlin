val br1 = 11

fun br() = 111

class Test(val br2 = 12) {
    val br3 = 13

    fun brf() = 112

    fun test(br4: Int) {
        while (true) {
            val br5 = 14
            br<caret>
        }
    }
}

// "br" function is before other elements because of exact prefix match

// ORDER: br, br4, br5, br2, br3, brf, br1, break
// SELECTED: 0