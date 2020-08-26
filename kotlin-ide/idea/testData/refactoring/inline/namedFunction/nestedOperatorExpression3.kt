fun callFail(p: String?) {
    println(printAndGet(42) + r<caret>un {
        println(33 - 33)
        33 + 33
    })

    println(33 - 33)
    println(printAndGet(-printAndGet(33 + 33)))

    println(44 + run {
        println(33 - 33)
        printAndGet(-printAndGet(33 + 33))
    })
}

fun printAndGet(i: Int) = i.also { println(it) }