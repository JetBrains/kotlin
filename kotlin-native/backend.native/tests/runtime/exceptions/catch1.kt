/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.exceptions.catch1

import kotlin.test.*

@Test fun runTest() {
    try {
        println("Before")
        foo()
        println("After")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}

fun foo() {
    throw Error("Error happens")
    println("After in foo()")
}