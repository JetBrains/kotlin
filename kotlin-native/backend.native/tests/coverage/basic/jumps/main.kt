package coverage.basic.jumps

fun simpleReturn(n: Int) {
    if (n == 0) return
    println(n)
}

fun returnFromIfBranch(n: Int) {
    if (n > 0) {
        if (n > 10) {
            return
        }
    } else if (n < -10) {
        return
    } else if (n == 0) {
        return
    }
    println(n)
}

fun returnFromWhenBranch(n: Int) {
    when {
        n == 0 -> return
        n == 1 -> return
        n == 2 -> {
            println(n)
            return
        }
        else -> println(n)
    }
}

fun breakFromWhile() {
    var a = 7
    while (true) {
        if (a < 4) break
        println(a)
        a--
    }
}

fun continueFromDoWhile() {
    var a = 0
    do {
        if (a % 3 == 0) {
            a++
            continue
        }
        println(a)
        a++
    } while (a < 10)
}

fun singleReturn() {
    return
}

fun nestedReturn() {
    while (true) {
        while (true) {
            while (true) {
                if (1 < 2) {
                    return
                }
                println()
            }
            println()
        }
    }
    println()
}

fun main() {
    simpleReturn(0)
    simpleReturn(1)

    returnFromIfBranch(1)
    returnFromIfBranch(11)
    returnFromIfBranch(-11)
    returnFromIfBranch(0)

    returnFromWhenBranch(0)
    returnFromWhenBranch(1)
    returnFromWhenBranch(2)

    breakFromWhile()
    continueFromDoWhile()
    singleReturn()
    nestedReturn()
}