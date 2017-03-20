package foo

fun test1(): String {
    run {
        if (false) {
        }
    }
    return "O"
}

fun test2(): String {
    1.let {
        if (false) {
        }
    }
    return "K"
}

fun box() = test1() + test2()