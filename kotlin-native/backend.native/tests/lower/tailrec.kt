/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package lower.tailrec


fun main() {
    println(add(5, 7))
    println(add(100000000, 0))

    println(fib(6))

    println(one(5))

    countdown(3)

    println(listOf(1, 2, 3).indexOf(3))
    println(listOf(1, 2, 3).indexOf(4))
}

tailrec fun add(x: Int, y: Int): Int = if (x > 0) add(x - 1, y + 1) else y

fun fib(n: Int): Int {
    tailrec fun fibAcc(n: Int, acc: Int): Int = if (n < 2) {
        n + acc
    } else {
        fibAcc(n - 1, fibAcc(n - 2, acc))
    }

    return fibAcc(n, 0)
}

tailrec fun one(delay: Int, result: Int = delay + 1): Int = if (delay > 0) one(delay - 1) else result

tailrec fun countdown(iterations: Int): Unit {
    if (iterations > 0) {
        println("$iterations ...")
        countdown(iterations - 1)
    } else {
        println("ready!")
    }
}

tailrec fun <T> List<T>.indexOf(x: T, startIndex: Int = 0): Int {
    if (startIndex >= this.size) {
        return -1
    }

    if (this[startIndex] != x) {
        return this.indexOf(x, startIndex + 1)
    }

    return startIndex

}
