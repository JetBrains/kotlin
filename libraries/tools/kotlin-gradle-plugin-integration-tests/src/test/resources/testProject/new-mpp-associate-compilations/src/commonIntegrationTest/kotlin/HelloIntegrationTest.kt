package com.example

import kotlin.test.Test

class HelloIntegrationTest {
    @Test
    fun test(): Unit = Hello().run {
        hello()
        internalFun()

        HelloTest().run {
            test()
            internalTestFun()
        }
    }

    @Test
    fun secondTest() = Unit

    @Test
    fun thirdTest() = Unit
}

fun topLevelMemberToMakeTheCompilerGenerateTheModuleFile() = 1
