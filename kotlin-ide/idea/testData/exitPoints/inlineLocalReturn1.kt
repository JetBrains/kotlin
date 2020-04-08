fun f(a: Int): Int {
    if (a < 5) {
        run {
            <caret>return@run 1
        }
    }
    else {
        return 2
    }
}

inline public fun <T> run(f: () -> T): T { }

//HIGHLIGHTED: return@run 1
