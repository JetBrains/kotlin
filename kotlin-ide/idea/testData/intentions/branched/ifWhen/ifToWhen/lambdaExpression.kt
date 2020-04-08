fun test(x: Int) {
    val f: Function<Int> = <caret>if (x == 0) {
        { 0 }
    } else if (x == 1) {
        { 1 }
    } else {
        { 2 }
    }
}