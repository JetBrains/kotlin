package com.example

import kotlin.test.Test

class HelloTest {
    @Test
    fun test(): Unit = Hello().run {
        hello()
        internalFun()
    }

    @Test
    fun secondTest() = Unit

    @Test
    fun thirdTest() = Unit

    internal fun internalTestFun() = 1
}
