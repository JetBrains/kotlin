/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package sample

public fun main(args: Array<String>): Unit {
    val hello = Hello()
    hello.doSomething()
}

public class Hello {
    var x = 0

    fun doSomething(): Unit {
        x++
    }
}