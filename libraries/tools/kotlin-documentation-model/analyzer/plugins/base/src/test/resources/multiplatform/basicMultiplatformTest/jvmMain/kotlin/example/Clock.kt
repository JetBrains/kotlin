/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package example

import greeteer.Greeter

/**
 * Documentation for actual class Clock in JVM
 */
actual open class Clock {
    actual fun getTime(): String = System.currentTimeMillis().toString()

    /**
     * Time in minis
     */
    actual fun getTimesInMillis(): String = System.currentTimeMillis().toString()

    /**
     * Documentation for onlyJVMFunction on...
     * wait for it...
     * ...JVM!
     */
    fun onlyJVMFunction(): Double = 2.5

    open fun getDayOfTheWeek(): String {
        TODO("not implemented")
    }

    /**
     * JVM custom kdoc
     */
    actual fun getYear(): String {
        TODO("not implemented")
    }
}

fun clockList() = listOf(Clock())

fun main() {
    Greeter().greet().also { println(it) }
}
