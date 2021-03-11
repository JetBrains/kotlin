fun test(p: Int): Int {
    return p + p
}

fun callFail(p: String?) {
    println(printAndGet(42) + te<caret>st(33))


    println(printAndGet(-printAndGet(test(33))))

    println(44 + printAndGet(-printAndGet(test(33))))
}

fun printAndGet(i: Int) = i.also { println(it) }